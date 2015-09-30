package se.kth.hopsworks.meta.wscomm.message;

import java.util.List;
import javax.json.JsonObject;
import se.kth.hopsworks.meta.entity.EntityIntf;

/**
 * Represents the different types of messages that may be exchanged between the
 * client and the server
 * <p/>
 * @author Vangelis
 */
public interface Message {

  /**
   * Every incoming message has a corresponding action. An action is a client
   * command instructing the Protocol class of the steps that must be taken.
   * <p/>
   * @param action the action of the message
   */
  public void setAction(String action);

  /**
   * Returns the corresponding action of the message.
   * <p/>
   * @return the action of the message
   */
  public String getAction();

  /**
   * Initializes the message variables with the values that come with the
   * JsonObject parameter.
   * <p/>
   * @param obj The incoming message as a json object
   */
  public void init(JsonObject obj);

  /**
   * Creates a json object as a string with the corresponding message values.
   * <p/>
   * @return the created json object as a string
   */
  public String encode();

  /**
   * Returns a list containing entities that represent all the content a
   * metadata template carries, based on the incoming JSON message. It's the
   * opposite of buildSchema().
   * <p/>
   * @return the created schema
   */
  public List<EntityIntf> parseSchema();

  /**
   * Builds a JSON object that represents the front end metadata template based
   * on the list of entities it accepts as a parameter. It's the opposite of
   * parseSchema()
   * <p/>
   * @param entity the list with the entities
   * @return the schema as a JSON string
   */
  public String buildSchema(List<EntityIntf> entity);

  /**
   * Returns the message.
   * <p/>
   * @return
   */
  public String getMessage();

  /**
   * Sets the message to be returned to the client.
   * <p/>
   * @param msg
   */
  public void setMessage(String msg);

  /**
   * Returns message sender.
   * <p/>
   * @return
   */
  public String getSender();

  /**
   * Sets message sender.
   * <p/>
   * @param sender
   */
  public void setSender(String sender);

  /**
   * Returns the status of the current message. Indicates the outcome of an
   * action.
   * <p/>
   * @return the current message status
   */
  public String getStatus();

  /**
   * Sets the status of the current message.
   * <p/>
   * @param status
   */
  public void setStatus(String status);
}
