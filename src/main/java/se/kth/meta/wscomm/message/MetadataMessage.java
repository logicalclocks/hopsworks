package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Fields;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.Tables;

/**
 * Represents a message carrying the actual metadata
 *
 * @author Vangelis
 */
public class MetadataMessage implements Message {

  private final String TYPE = "MetadataMessage";
  private String sender;
  private String message;
  private String action;
  private String status;

  /**
   * Default constructor. Vital for the class loader
   */
  public MetadataMessage() {
    this.status = "OK";
  }

  /**
   * Used to send custom messages
   *
   * @param sender the message sender
   * @param message the actual message
   */
  public MetadataMessage(String sender, String message) {
    this();
    this.sender = sender;
    this.message = message;
  }

  @Override
  public void init(JsonObject obj) {
    this.sender = obj.getString("sender");
    this.message = obj.getString("message");
    this.action = obj.getString("action");
  }

  @Override
  public String encode() {
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
  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  /**
   * If inodeid is present in the message, parseShema creates a list of
   * RawData entity objects initialized with the actual data contained in the
   * message object. If inodeid is not present in the message, then the
   * current message asks for a table's raw data, so parseSchema will look for
   * tableid instead.
   *
   * @return the list with the RawData objects
   */
  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();
    List<EntityIntf> data = new LinkedList<>();

    try {
      int tableId = obj.getInt("tableid");
      Tables table = new Tables(tableId);
      List<EntityIntf> list = new LinkedList<>();
      list.add(table);
      return list;
    } catch (NullPointerException e) {

    }

    int inodeid = 1367; //Integer.parseInt(obj.getString("inodeid"));
    Set<Entry<String, JsonValue>> set = obj.entrySet();

    for (Entry<String, JsonValue> entry : set) {
      RawData raw = new RawData();

      //avoid the inodeid field as it has been accessed previously
      if (isNumeric(entry.getKey())) {
        //set the field id and the actual data
        raw.setFieldid(Integer.parseInt(entry.getKey()));
        raw.setData(entry.getValue().toString());
        raw.setInodeid(inodeid);
        data.add(raw);
      }
    }

    return data;
  }

  private boolean isNumeric(String value) {
    try {
      Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  @Override
  public String buildSchema(List<EntityIntf> entities) {
    JsonObjectBuilder builder = Json.createObjectBuilder();

    Tables table = (Tables) entities.get(0);
    builder.add("table", table.getName());

    JsonArrayBuilder fields = Json.createArrayBuilder();

    List<Fields> f = table.getFields();

    for (Fields fi : f) {
      JsonObjectBuilder field = Json.createObjectBuilder();
      field.add("id", fi.getId());
      field.add("name", fi.getName());

      JsonArrayBuilder rd = Json.createArrayBuilder();
      List<RawData> data = fi.getRawData();

      for (RawData raw : data) {
        JsonObjectBuilder rawdata = Json.createObjectBuilder();
        rawdata.add("raw", raw.getData());
        rawdata.add("inodeid", raw.getInodeid());

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
