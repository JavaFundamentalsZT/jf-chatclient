package ee.tut.fundamentals.chat;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller that receives events from the visible chat form.
 * Only reacts when [Enter] is pressed in the message field
 * Then it invokes the ChatClient to send the message to the server.
 */
public class ChatController implements Initializable, MessageListener {

  private ChatClient chatClient;

  @FXML
  private TextField say;

  @FXML
  private TextArea chat;



  /**
   * Fired when user presses the [Enter] key when the text box has focus.
   */
  @FXML
  public void handleButtonAction(ActionEvent event) {
      String text = say.getText().trim();
      if (text.length()==0) {
        return;
      }

      try {
        //post the value of the text box
        chatClient.postMessage(text);
      }
      catch (Exception e) {
        System.out.println("ERROR: " + e.getMessage());
      }

      //clear the text box
      say.setText("");
  }


  /**
   * Great, we received an IM from the server. Let's display it
   */
  @Override
  public void onMessage(String msg) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        chat.appendText(msg + "\n");
      }
    });
  }


  @Override
  public void initialize(URL location, ResourceBundle resources) {
    say.requestFocus();
  }

  public void setChatClient(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

}
