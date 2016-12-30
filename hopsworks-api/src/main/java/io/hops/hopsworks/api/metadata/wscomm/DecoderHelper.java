package io.hops.hopsworks.api.metadata.wscomm;

import io.hops.hopsworks.api.metadata.wscomm.message.Message;
import io.hops.hopsworks.common.metadata.exception.ApplicationException;
import javax.json.JsonObject;

public class DecoderHelper {

  private JsonObject json;

  public DecoderHelper(JsonObject json) {
    this.json = json;
  }

  /**
   * Instantiates a message object on the runtime based on the 'type' parameter
   * of the message
   * <p/>
   * @return the initialized Message object
   * <p/>
   * @throws ApplicationException
   */
  public Message getMessage() throws ApplicationException {

    Message msg = null;
    try {
      String message = this.json.getString("type");
      Class c = getClass().getClassLoader().loadClass(
              "se.kth.hopsworks.meta.wscomm.message." + message);
      msg = (Message) c.newInstance();
    } catch (ClassNotFoundException | InstantiationException |
            IllegalAccessException e) {
      throw new ApplicationException(
              "Could not find the right class to initialize the message", e);
    }

    return msg;
  }
}
