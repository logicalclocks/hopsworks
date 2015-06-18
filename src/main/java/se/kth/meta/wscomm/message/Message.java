package se.kth.meta.wscomm.message;

import se.kth.meta.entity.EntityIntf;
import java.util.List;
import javax.json.JsonObject;

/**
 * Represents the different types of messages that may be exchanged between the
 * client and the server
 *
 * @author Vangelis
 */
public interface Message {

  /**
   * Every incoming message has a corresponding action.
   *
   * @param action the action of the message
   */
  public void setAction(String action);

  /**
   * Returns the corresponding action of the message.
   * <p>
   * @return the action of the message
   */
  public String getAction();

  /**
   * Initializes the message variables with the values that come with the
   * JsonObject parameter
   *
   * @param obj The incoming message as a json object
   */
  public void init(JsonObject obj);

  /**
   * Creates a json object as a string with the corresponding message values.
   *
   * @return the created json object as a string
   */
  public String encode();

  /**
   * Returns a list containing all the tables with their fields and field
   * types, based on the contents of the JSON incoming message. It's the
   * opposite of buildSchema()
   *
   * @return the created schema
   */
  public List<EntityIntf> parseSchema();

  /**
   * Builds a JSON object that represents the front end board based on the
   * list of entities it accepts as a parameter. It's the opposite of
   * parseSchema()
   *
   * @param entity the list with the entities
   * @return the schema as a JSON string
   */
  public String buildSchema(List<EntityIntf> entity);

  /**
   * Returns the message
   *
   * @return
   */
  public String getMessage();

  /**
   *
   * @param msg
   */
  public void setMessage(String msg);

  /**
   * Returns the sender of the message
   *
   * @return
   */
  public String getSender();

  /**
   * Sets the sender of the message
   *
   * @param sender
   */
  public void setSender(String sender);

  /**
   * Returns the status of the current message. Indicates the outcome of an
   * action
   *
   * @return the current message status
   */
  public String getStatus();

  /**
   * Sets the status of the current message
   *
   * @param status
   */
  public void setStatus(String status);
}
