package ee.tut.fundamentals.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Map;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Simple JavaFX based chat client
 * By default it uses the ports 8888 and 8080 to communicate with the chat server.
 * This class sets up ChatClient class which handles all of the communication,
 * as well as the JavaFX UI which is defined by Chat.fxml and ChatController class
 */
public class ChatClientFx extends Application {

  private static final String CMD_SLEEP_PREFIX = "cmd:sleep ";
  private static final String CMD_WAIT_FOR_PREFIX = "cmd:wait-for ";
  private static final String CMD_EXIT_PREFIX = "cmd:exit ";
  static final String ARGUMENT_HELP = "--name=yourName --host=hostname [--serverPort=number] [--httpPort=number]";
  static final String PARAM_NAME_HOST = "host";
  static final String PARAM_NAME_NAME = "name";
  static final String PARAM_NAME_HTTP_PORT = "httpPort";
  static final String PARAM_NAME_TCP_PORT = "serverPort";
  static final String PARAM_NAME_CLI = "cli";
  static final String PARAM_NAME_INPUT = "input";
  static final String PARAM_NAME_OUTPUT = "output";
  static final int HTTP_PORT = 8080;
  static final int TCP_PORT = 8888;
  private static final String ERR_FXML = "ee/tut/fundamentals/chat/Err.fxml";
  private static final String CHAT_FXML = "ee/tut/fundamentals/chat/Chat.fxml";

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {

    Map<String, String> params = getParameters().getNamed();

    if (params.containsKey(PARAM_NAME_CLI)) {
      runCli(stage, params);
    } else {
      setupGui(stage, params);
    }

  }

  /**
   * Command line interface is used for automated tests.
   */
  private void runCli(Stage stage, Map<String, String> params) {
    try (
        ChatClient client = setupChatClient(params);
        BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(params), "UTF-8"));
        PrintStream writer = new PrintStream(getOutputStream(params), false, "UTF-8");
      ) {
      
      client.setMessageListener(msg -> writer.println(msg));
      reader.lines().forEach(line -> postMessage(client, line));

      client.join();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void postMessage(ChatClient client, String msg) {
    if (msg.startsWith(CMD_SLEEP_PREFIX) && msg.length() > CMD_SLEEP_PREFIX.length()) {
      try {
        long sleepTime = Long.parseLong(msg.substring(CMD_SLEEP_PREFIX.length()).trim());
        System.out.format("sleeping for %sms%n", sleepTime);
        Thread.sleep(sleepTime);
      }
      catch (NumberFormatException e) {
        System.out.println("error while trying to parse sleeping time: " + e);
      }
      catch (InterruptedException e) {
        System.out.println("we were interrupted while sleeping");
      }
    } else if (msg.startsWith(CMD_EXIT_PREFIX) && msg.length() > CMD_EXIT_PREFIX.length()) {
      try {
        int exitCode = Integer.parseInt(msg.substring(CMD_EXIT_PREFIX.length()).trim());
        System.out.format("exiting with exit code %s%n", exitCode);
        System.exit(exitCode);
      }
      catch (NumberFormatException e) {
        System.out.println("error while trying to parse exit code: " + e);
      }
    } else if (msg.startsWith(CMD_WAIT_FOR_PREFIX) && msg.length() > CMD_WAIT_FOR_PREFIX.length()) {
      String waitFor = msg.substring(CMD_WAIT_FOR_PREFIX.length()).trim();
      System.out.format("waiting for '%s'%n", waitFor);
      client.waitFor(waitFor);
      System.out.format("got '%s', proceeding%n", waitFor);
    } else {
      client.postMessage(msg);
    }
  }

  private PrintStream getOutputStream(Map<String, String> params) throws IOException {
    PrintStream os;
    if (params.containsKey(PARAM_NAME_OUTPUT)) {
      File f = new File(params.get(PARAM_NAME_OUTPUT));
      if (f.exists() && !f.canWrite()) {
        throw new IllegalArgumentException("file exists and is not writable: " + f);
      } else {
        os = new PrintStream(new FileOutputStream(f), true, "UTF-8");
      }
    } else {
      //if output not specified, use stdout
      os = System.out;
    }
    return os;
  }

  private InputStream getInputStream(Map<String, String> params) throws FileNotFoundException {
    InputStream is;
    if (params.containsKey(PARAM_NAME_INPUT)) {
      File f = new File(params.get(PARAM_NAME_INPUT));
      if (!f.exists()) {
        throw new IllegalArgumentException("file does not exist: " + f);
      } else if (!f.canRead()) {
        throw new IllegalArgumentException("file is not readable: " + f);
      } else {
        is = new FileInputStream(f);
      }
    } else {
      //if input not specified, use stdin
      is = System.in;
    }
    return is;
  }

  private void setupGui(Stage stage, Map<String, String> params) throws IOException {
    FXMLLoader loader = makeLoader(CHAT_FXML);
    BorderPane root = (BorderPane) loader.load();

    stage.setScene(new Scene(root));
    stage.setTitle("Fundamental Chat");

    try {
      ChatClient client = setupChatClient(params);
      //get the controller, we need to configure it further
      ChatController controller = loader.getController();

      //Configure the controller to use the ChatClient
      controller.setChatClient(client);
      client.setMessageListener(controller);
    }
    catch (Exception e) {
      stage.setScene(buildErrorScene());
    }

    //show the GUI
    stage.show();
  }



  private ChatClient setupChatClient(Map<String, String> params) throws IOException {
    if (!params.containsKey(PARAM_NAME_NAME) || !params.containsKey(PARAM_NAME_HOST)) {
      usage();
    }
    String user = params.get(PARAM_NAME_NAME);
    String host = params.get(PARAM_NAME_HOST);

    int serverPort = TCP_PORT;
    int httpPort=HTTP_PORT;

    if (params.containsKey(PARAM_NAME_TCP_PORT)) {
      serverPort = Integer.valueOf(params.get(PARAM_NAME_TCP_PORT));
    }
    if (params.containsKey(PARAM_NAME_HTTP_PORT)) {
      httpPort = Integer.valueOf(params.get(PARAM_NAME_HTTP_PORT));
    }

    return new ChatClient(user, host, serverPort, httpPort);
  }



  private FXMLLoader makeLoader(String fxmlResourceName) {
    URL url = getClass().getClassLoader().getResource(fxmlResourceName);
    if (url == null) {
      System.out.println("Unable to read fxml at " + fxmlResourceName);
      System.exit(-1);
    }
    return new FXMLLoader(url);
  }

  public Scene buildErrorScene() throws IOException {
    return new Scene(makeLoader(ERR_FXML).load());
  }

  public void usage() {
    System.out.println("Usage: java -jar ChatClientFx.jar " + ARGUMENT_HELP);
    System.exit(-1);
  }

}
