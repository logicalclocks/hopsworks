package se.kth.hopsworks.meta.wscomm;

import javax.json.JsonObject;
import se.kth.hopsworks.meta.exception.ApplicationException;
import se.kth.hopsworks.meta.wscomm.message.Message;

/**
 *
 * @author Vangelis
 */
public class DecoderHelper {

  private JsonObject json;

  public DecoderHelper(JsonObject json) {
    this.json = json;
  }

  /**
   * Instantiates a message object on the runtime based on the 'type' parameter
   * of the message
   * <p>
   * @return the initialized Message object
   * <p>
   * @throws ApplicationException
   */
  public Message getMessage() throws ApplicationException {

    Message msg = null;
    try {
      String message = this.json.getString("type");
      Class c = getClass().getClassLoader().loadClass(
              "se.kth.meta.wscomm.message." + message);
      msg = (Message) c.newInstance();
    } catch (ClassNotFoundException | InstantiationException |
            IllegalAccessException e) {
      throw new ApplicationException(e.getMessage());
    }

    return msg;
  }
}
