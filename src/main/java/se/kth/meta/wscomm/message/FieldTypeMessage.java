package se.kth.meta.wscomm.message;

import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.FieldType;

/**
 *
 * @author vangelis
 */
public class FieldTypeMessage extends PlainMessage {

  private static final Logger logger = Logger.
          getLogger(FieldTypeMessage.class.getName());

  private String TYPE = "FieldTypeMessage";
  private String sender;
  private String message;
  private String action;
  private String status;

  public FieldTypeMessage() {
    this.status = "OK";
    this.action = "fetch_field_types";
  }

  @Override
  public void init(JsonObject json) {
    this.sender = json.getString("sender");
    this.message = json.getString("message");
    this.action = json.getString("action");
    this.setStatus("OK");
    super.setAction(this.action);
  }

  @Override
  public String encode() {
    String value = Json.createObjectBuilder()
            .add("sender", this.sender)
            .add("type", this.TYPE)
            .add("status", this.status)
            .add("message", this.message)
            .build()
            .toString();

    return value;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  @Override
  public void setAction(String action) {
    this.action = action;
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

  @Override
  public String buildSchema(List<EntityIntf> list) {

    List<FieldType> ft = (List<FieldType>) (List<?>) list;

    JsonObjectBuilder builder = Json.createObjectBuilder();
    JsonArrayBuilder fieldTypes = Json.createArrayBuilder();

    for (FieldType fi : ft) {
      JsonObjectBuilder field = Json.createObjectBuilder();
      field.add("id", fi.getId());
      field.add("description", fi.getDescription());

      fieldTypes.add(field);
    }

    builder.add("fieldTypes", fieldTypes);

    return builder.build().toString();
  }

  @Override
  public String toString() {
    return "{\"sender\": \"" + this.sender + "\", "
            + "\"action\": \"" + this.action + "\", "
            + "\"type\": \"" + this.TYPE + "\", "
            + "\"message\": \"" + this.message + "\"}";
  }
}
