
package se.kth.meta.wscomm.message;

import javax.json.Json;
import javax.json.JsonObject;

/**
 *
 * @author Vangelis
 */
public class TextMessage extends PlainMessage {

    private final String TYPE = "TextMessage";
    private String sender;
    private String message;
    private String action;
    private String status;

    /**
     * Default constructor. Used by the class loader to create an instance of
     * this class
     */
    public TextMessage() {
        this.status = "OK";
    }

    /**
     * Used to send custom messages
     *
     * @param sender the message sender
     */
    public TextMessage(String sender) {
        this();
        this.sender = sender;
    }
    
    public TextMessage(String sender, String message){
        this(sender);
        this.message = message;
    }

    @Override
    public void init(JsonObject json) {

        this.sender = json.getString("sender");
        this.message = json.getString("message");
        this.action = json.getString("action");
        //System.out.println("TextMessage INIT " + sender + " " + message + " " + action);
    }

    @Override
    public String encode() {

        if (this.sender == null || this.message == null) {
            this.sender = "Unknown";
            this.message = "Nothing to do";
        }

        String value = Json.createObjectBuilder()
                .add("sender", this.sender)
                .add("type", this.TYPE)
                .add("status", this.status)
                .add("message", this.message)
                .build() //pretty necessary so as to build the actual json structure
                .toString();

        return value;
    }

    @Override
    public String getAction() {
        return this.action;
    }

    @Override
    public void setSender(String sender) {
        this.sender = sender;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getSender() {
        return this.sender;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
    
    @Override
    public void setStatus(String status){
        this.status = status;
    }

    @Override
    public String toString() {
        return "{\"sender\": \"" + this.sender + "\", "
                + "\"type\": \"" + this.TYPE + "\", "
                + "\"action\": \"" + this.action + "\", "
                + "\"message\": \"" + this.message + "\"}";
    }
}
