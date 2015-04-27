
package se.kth.meta.wscomm;

import se.kth.meta.exception.ApplicationException;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import se.kth.meta.wscomm.message.Message;

/**
 *
 * @author Vangelis
 */
public class MessageDecoder implements Decoder.Text<Message> {

    @Override
    public Message decode(String textMessage) throws DecodeException {

        Message msg = null;
        JsonObject obj = Json.createReader(new StringReader(textMessage)).readObject();

        try {
            DecoderHelper helper = new DecoderHelper(obj);
            msg = helper.getMessage();
            msg.init(obj);
        } catch (ApplicationException e) {
            System.err.println("MessageDecoder.java: " + e.getMessage());
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
