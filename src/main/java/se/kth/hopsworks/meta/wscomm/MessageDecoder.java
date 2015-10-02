package se.kth.hopsworks.meta.wscomm;

import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import se.kth.hopsworks.meta.exception.ApplicationException;
import se.kth.hopsworks.meta.wscomm.message.Message;

/**
 *
 * @author Vangelis
 */
public class MessageDecoder implements Decoder.Text<Message> {

  private static final Logger logger = Logger.getLogger(MessageDecoder.class.
          getName());

  @Override
  public Message decode(String textMessage) throws DecodeException {

    Message msg = null;
    JsonObject obj = Json.createReader(new StringReader(textMessage)).
            readObject();

    try {
      DecoderHelper helper = new DecoderHelper(obj);
      msg = helper.getMessage();
      msg.init(obj);
    } catch (ApplicationException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
    return msg;
  }

  @Override
  public void init(final EndpointConfig ec) {
  }

  @Override
  public boolean willDecode(final String s) {
    return true;
  }

  @Override
  public void destroy() {
  }
}
