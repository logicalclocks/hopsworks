package se.kth.meta.wscomm.message;

import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.MTable;

/**
 * Represents a message request for stored metadata filtered by table id and
 * inode id
 *
 * @author Vangelis
 */
public class FetchMetadataMessage extends MetadataMessage {

  /**
   * Default constructor. Vital for the class loader
   */
  public FetchMetadataMessage() {
    super();
    this.TYPE = "FetchMetadataMessage";
    this.status = "OK";
  }

  /**
   * Used to send custom messages
   *
   * @param sender the message sender
   * @param message the actual message
   */
  public FetchMetadataMessage(String sender, String message) {
    this();
    this.sender = sender;
    this.message = message;
  }
  
  @Override
  public void init(JsonObject obj){
    super.init(obj);
  }

  @Override
  public String encode() {
    System.out.println("SENDER " + this.sender + " STATUS " + this.status + " MESSAGE  " + this.message);
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
  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  @Override
  public List<EntityIntf> parseSchema() {
    return super.parseSchema();
  }

  @Override
  public String buildSchema(List<EntityIntf> entities) {
    JsonObjectBuilder builder = Json.createObjectBuilder();

    MTable table = (MTable) entities.get(0);
    builder.add("table", table.getName());

    JsonArrayBuilder fields = Json.createArrayBuilder();

    List<Field> f = table.getFields();

    for (Field fi : f) {
      JsonObjectBuilder field = Json.createObjectBuilder();
      field.add("id", fi.getId());
      field.add("name", fi.getName());

      JsonArrayBuilder rd = Json.createArrayBuilder();
      List<RawData> data = fi.getRawData();

      for (RawData raw : data) {
        JsonObjectBuilder rawdata = Json.createObjectBuilder();
        rawdata.add("raw", raw.getData());
        //rawdata.add("inodeid", raw.getInodeid());

        rd.add(rawdata);
      }
      field.add("data", rd);
      fields.add(field);
    }

    builder.add("fields", fields);

    return builder.build().toString();
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
  public String getStatus() {
    return this.status;
  }

  @Override
  public void setStatus(String status) {
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
