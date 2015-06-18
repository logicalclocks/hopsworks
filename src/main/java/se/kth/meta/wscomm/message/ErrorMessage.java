package se.kth.meta.wscomm.message;

import java.util.logging.Logger;
import javax.json.Json;

/**
 *
 * @author Vangelis
 */
public class ErrorMessage extends PlainMessage {

  private static final Logger logger = Logger.
          getLogger(ErrorMessage.class.getName());

  private final String TYPE = "ErrorMessage";
  private String sender;
  private String message;
  private String action;

  public ErrorMessage(String sender, String message) {
    this.sender = sender;
    this.message = message;
  }

  @Override
  public String encode() {
    String value = Json.createObjectBuilder()
            .add("sender", this.sender)
            .add("type", this.TYPE)
            .add("message", this.message)
            .build()
            .toString();

    return value;
  }

  @Override
  public String getMessage() {
    return this.message;
  }

  @Override
  public void setMessage(String msg) {
    this.message = msg;
  }

  @Override
  public String getSender() {
    return this.sender;
  }

  @Override
  public void setSender(String sender) {
    this.sender = sender;
  }
}
