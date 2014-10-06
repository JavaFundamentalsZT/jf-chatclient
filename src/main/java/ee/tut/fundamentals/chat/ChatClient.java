package ee.tut.fundamentals.chat;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

/**
 * Simple Chat Client implementation
 * Connects to the remote server using plain TCP connection and sends over the client's name.
 * The socket is later used for receiving IM's only.
 *
 * Posting messages to the server is done using HTTP protocol.
 * The client makes a POST request to the server, where the username is set as a HTTP header and
 * the message is in the request body.
 */
public class ChatClient implements Closeable {

  private final URL postUrl;
  private final URI serverUri;
  private final String name;
  private volatile IncomingMessageListener listener;

  public ChatClient (String name, String hostname, int serverPort, int httpPort) throws UnknownHostException, IOException {
    this.name = name;

    //messages will be POST'ed to this URL
    postUrl = new URL("http", hostname, httpPort, "/");
    //messages will be received from this TCP endpoint
    try {
      serverUri = new URI("tcp", null, hostname, serverPort, null, null, null);
    }
    catch (URISyntaxException e) {
      throw new RuntimeException("should not happen!", e);
    }

    //connect to the server, send the name and start listening for incoming messages
    //spawned as a new thread, which terminates when the main application is closed
    listener = new IncomingMessageListener(serverUri, name);
  }

  @Override
  public void close() throws IOException {
    if (listener != null) {
      listener.close();
    }
  }

  public final void join() throws InterruptedException {
    listener.join();
  }

  /**
   * Invokes the HTTP post request with the message
   */
  public void postMessage(String msg) {
    System.out.println("posting message: " + msg);
    OutputStreamWriter out = null;
    try {
      HttpURLConnection conn = (HttpURLConnection) this.postUrl.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
      conn.setRequestProperty("author", name);
      conn.setDoOutput(true);

      out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
      out.write(msg);
      out.flush();
      System.out.println("Message sent: " + conn.getResponseMessage());
    }
    catch (ConnectException e) {
      System.out.println("Could not connect, re-initializing: " + e);
      listener.reset();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    finally {
      if (out != null) {
        try {
          out.close();
        }
        catch (Exception e) {}
      }
    }

  }

  public void waitFor(String waitFor) {
    this.listener.waitFor(waitFor);
  }

  /**
   * Provides a hook for the GUI to receive notifications about incoming IM's
   */
  public void setMessageListener(MessageListener listener) {
    this.listener.setMessageListener(listener);
  }


  private static class IncomingMessageListener implements Runnable, Closeable {

    private BufferedReader in;
    private final URI serverUri;
    private volatile MessageListener listener;
    private final String name;
    private final Thread listenerThread;
    private volatile boolean isClosing = false;
    private volatile Socket sock;

    private volatile String waitForStr;
    private volatile CountDownLatch waitForLatch;

    public IncomingMessageListener (URI serverUri, String name) throws IOException {
      this.serverUri = serverUri;
      this.name = name;
      listenerThread = new Thread(this);
      listenerThread.setDaemon(true);
      listenerThread.start();
    }

    public void setMessageListener(MessageListener listener) {
      this.listener = listener;
    }

    public void waitFor(String waitFor) {
      this.waitForStr = waitFor;
      CountDownLatch latch = new CountDownLatch(1);
      this.waitForLatch = latch;
      try {
        latch.await();
      }
      catch (InterruptedException e) {
        System.out.println("interrupted while waiting for: " + waitFor);
      }
    }

    @Override
    public void close() throws IOException {
      this.isClosing = true;
      if (listenerThread != null) {
        listenerThread.interrupt();
      }
    }

    public void reset() {
      Socket s = this.sock;
      if (s != null) {
        try {
          s.close();
        }
        catch (IOException e) {
          System.out.println("error while closing socket: " + e);
        }
      }
    }

    public final void join() throws InterruptedException {
      listenerThread.join();
    }

    /**
     * Main loop that listens for incoming IM's and notifies the listener, if present
     */
    public void run() {
      while (!isClosing) {
        connectAndProcess();
        try {
          Thread.sleep(1000);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    public void connectAndProcess() {
      System.out.println("Connecting to " + serverUri);
      try (
          Socket sock = new Socket(serverUri.getHost(), serverUri.getPort());
          InputStream sis = sock.getInputStream();
          OutputStream sos = sock.getOutputStream();
      ) {
        this.sock = sock;
        in = new BufferedReader(new InputStreamReader(sis, "UTF-8"));
        System.out.println("Connected to server");

        //send over the name
        sendServerName(sos);
        while (!isClosing) {
          if (listenForServerMessages()) return;
        }
      } catch (ConnectException e) {
        System.out.println("unable to connect to " + serverUri + ": " + e);
      } catch (Exception e) {
        if (!isClosing) {
          e.printStackTrace();
        }
      } finally {
        this.sock = null;
      }

    }

    private boolean listenForServerMessages() throws IOException {
      System.out.println("Waiting for readline");
      String line = in.readLine();
      if (line == null) {
        return true;
      }
      System.out.println("Received line:" + line);

      if (listener != null) {
        onMessage(line);
      } else {
        System.out.println("Could not process line, no listener provided.");
      }
      return false;
    }

    private void onMessage(String msg) {
      listener.onMessage(msg);
      CountDownLatch latch = this.waitForLatch;
      if (latch != null && msg.trim().equals(this.waitForStr)) {
        latch.countDown();
        this.waitForLatch = null;
        this.waitForStr = null;
      }
    }

    private void sendServerName(OutputStream sos) throws IOException {
      OutputStreamWriter out = new OutputStreamWriter(sos);
      out.write(name + "\n");
      out.flush();
    }


  }

}
