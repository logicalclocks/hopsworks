
package se.kth.meta.wscomm;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import se.kth.meta.wscomm.message.Message;

/**
 *
 * @author Vangelis
 */
public class MessageEncoder implements Encoder.Text<Message> {

    @Override
    public String encode(Message msg) throws EncodeException {

        String value = msg.encode();

        System.err.println("RETURNED VALUE IS " + value);
        return value;
    }

    @Override
    public void init(final EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }

}
