package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.FieldPredefinedValue;
import se.kth.meta.entity.MTable;

/**
 *
 * @author Vangelis
 */
public class FieldMessage extends ContentMessage {

  private static final Logger logger = Logger.getLogger(FieldMessage.class.
          getName());

  public FieldMessage() {
    super();
    this.TYPE = "FieldMessage";
  }

  @Override
  public void init(JsonObject json) {
    this.sender = json.getString("sender");
    this.message = json.getString("message");
    this.action = json.getString("action");
    super.setAction(this.action);

    try {
      JsonObject object = Json.createReader(new StringReader(this.message)).
              readObject();
      super.setTemplateid(object.getInt("tempid"));
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE, "Error while retrieving the templateid", e);
    }
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
  public String getAction() {
    return this.action;
  }

  /**
   * Returns a list with just one MTable object containing all its fields,
   * based on the contents of the JSON incoming message. It's the opposite of
   * buildSchema()
   *
   * @return the created schema
   */
  @Override
  public List<EntityIntf> parseSchema() {

    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    int fieldId = obj.getInt("id");
    int tableId = obj.getInt("tableid");
    String tableName = obj.getString("tablename");
    String name = obj.getString("name");
    String type = obj.getString("type");
    String maxsize = obj.getJsonObject("sizefield").getString("value");

    try {
      //sanitize maxsize in case the user has entered shit
      maxsize = (!"".equals(maxsize)) ? maxsize : "0";
      Integer.parseInt(maxsize);
    } catch (NumberFormatException e) {
      maxsize = "0";
    }

    boolean searchable = obj.getBoolean("searchable");
    boolean required = obj.getBoolean("required");
    String description = obj.getString("description");
    int fieldtypeid = obj.getInt("fieldtypeid");

    Field field = new Field(fieldId, tableId, name, type,
            Integer.parseInt(maxsize), (short) ((searchable) ? 1 : 0),
            (short) ((required) ? 1 : 0), description, fieldtypeid);

    //-- ATTACH the field's parent entity (FieldType)
    field.setFieldTypeId(fieldtypeid);

    //-- ATTACH fieldtype's child entity (Field)
    //fieldtype.getFields().add(field);
    //get the predefined values of the field if it is a yes/no field or a dropdown list field
    if (fieldtypeid != 1) {

      JsonArray predefinedFieldValues = obj.getJsonArray("fieldtypeContent");
      List<FieldPredefinedValue> ll = new LinkedList<>();

      for (JsonValue predefinedFieldValue : predefinedFieldValues) {

        JsonObject defaultt = Json.createReader(new StringReader(
                predefinedFieldValue.toString())).readObject();
        String defaultValue = defaultt.getString("value");

        FieldPredefinedValue predefValue = new FieldPredefinedValue(-1, field.
                getId(), defaultValue);

        //-- ATTACH predefinedValue's parent entity (Field)
        //predefValue.setFieldid(field.getId());
        ll.add(predefValue);
      }

      //-- ATTACH the field's children entities (FieldPredefinedValue)
      field.setFieldPredefinedValues(ll);
    }

    MTable table = new MTable(tableId, tableName);
    table.setTemplateid(super.getTemplateid());

    //-- ATTACH the field's parent entity (MTable)
    field.setTableid(table.getId());
    //-- ATTACH the table's child entity (Field)
    table.addField(field);

    List<EntityIntf> list = new LinkedList<>();
    list.add(table);

    return list;
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
            + "\"status\": \"" + this.status + "\", "
            + "\"action\": \"" + this.action + "\", "
            + "\"message\": \"" + this.message + "\"}";
  }
}
