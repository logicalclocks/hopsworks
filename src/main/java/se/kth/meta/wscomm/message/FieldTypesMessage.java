package se.kth.meta.wscomm.message;

import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.FieldTypes;

/**
 *
 * @author vangelis
 */
public class FieldTypesMessage extends PlainMessage {

  private String TYPE = "FieldTypesMessage";
  private String sender;
  private String message;
  private String action;
  private String status;

  public FieldTypesMessage() {
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

    List<FieldTypes> ft = (List<FieldTypes>) (List<?>) list;
    System.out.println("LIST SIZE " + list.size());

    JsonObjectBuilder builder = Json.createObjectBuilder();
    JsonArrayBuilder fieldTypes = Json.createArrayBuilder();

    for (FieldTypes fi : ft) {
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
