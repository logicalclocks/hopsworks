package se.kth.meta.wscomm.message;

import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Fields;
import se.kth.meta.entity.Tables;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import se.kth.meta.entity.FieldPredefinedValues;
import se.kth.meta.entity.FieldTypes;
import se.kth.meta.entity.Templates;
import se.kth.meta.exception.ApplicationException;

/**
 * Represents a metadata schema message
 *
 * @author Vangelis
 */
public class TemplateMessage extends ContentMessage {

  private static final Logger logger = Logger.getLogger(TemplateMessage.class.
          getName());

  private final String TYPE = "TemplateMessage";
  private String sender;
  private String message;
  private String action;
  private String status;

  public TemplateMessage() {
    this.status = "OK";
    this.action = "fetch_template";
  }

  public TemplateMessage(String sender) {
    this();
    this.sender = sender;
  }

  @Override
  public void init(JsonObject json) {
    this.sender = json.getString("sender");
    this.message = json.getString("message");
    this.action = json.getString("action");
    this.setStatus("OK");
    super.setAction(this.action);

    //when asking for template names list, tempid is null
    try {
      JsonObject object = Json.createReader(new StringReader(this.message)).
              readObject();
      super.setTemplateid(object.getInt("tempid"));
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE, "Error while retrieving the templateid."
              + " Probably fetching the templates");
    }
  }

  @Override
  public String encode() {
    String value = Json.createObjectBuilder()
            .add("sender", this.sender)
            .add("type", this.TYPE)
            .add("status", this.status)
            .add("message", this.message)
            .build() //build the actual json structure
            .toString();

    return value;
  }

  /**
   * Returns the template object. It is used only when creating a new template
   * or deleting one. The message carries either the template name or the
   * template id depending on the desired action.
   *
   * @return the template to be added in the database
   * @throws se.kth.meta.exception.ApplicationException
   */
  @Override
  public Templates getTemplate() throws ApplicationException {
    Templates temp = null;
    JsonObject object = Json.createReader(new StringReader(this.message)).
            readObject();

    try {
      switch (Command.valueOf(this.action.toUpperCase())) {
        case ADD_NEW_TEMPLATE:
          temp = new Templates(-1, object.getString("templateName"));
          break;
        case REMOVE_TEMPLATE:
          temp = new Templates(object.getInt("templateId"));
          break;
        default:
          throw new ApplicationException("Unknown command in received message");
      }
    } catch (NullPointerException e) {
      logger.
              log(Level.SEVERE,
                      "Error while retrieving the template attributes.");
      throw new ApplicationException(
              "Error while retrieving the template attributes.");
    }

    return temp;
  }

  @Override
  public List<EntityIntf> parseSchema() {

    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();
    JsonObject board = obj.getJsonObject("bd");

    Map<String, String[][]> schema = new HashMap<>();
    List<EntityIntf> tlist = new LinkedList<>();

    //get the prospective tables
    JsonArray tables = board.getJsonArray("columns");

    //there is the case of the empty template
    if (tables == null) {
      return tlist;
    }

    int noofTables = tables.size();

    for (int i = 0; i < noofTables; i++) {
      JsonObject item = tables.getJsonObject(i);
      String tableName = item.getString("name");
      int tableId = item.getInt("id");

      /*
       * if a template is being extended, cancel the table id so that they are
       * reinserted and attached to the new template
       */
      if (Command.valueOf(this.action.toUpperCase()) == Command.EXTEND_TEMPLATE) {
        tableId = -1;
      }

      Tables table = new Tables(tableId, tableName);
      table.setTemplateid(super.getTemplateid());

      //get the table attributes/fields
      JsonArray fields = item.getJsonArray("cards");
      int noofFields = fields.size();

      int fieldId = -1;
      String fieldName;
      boolean searchable = false;
      boolean required = false;
      String maxsize;
      String description;
      int fieldtypeid;

      //retrieve the table fields/attributes
      for (int j = 0; j < fields.size(); j++) {

        try {
          JsonObject field = fields.getJsonObject(j);
          fieldId = field.getInt("id");

          /*
           * if a template is being extended, cancel the field id so that they
           * are
           * reinserted and attached to the new template
           */
          if (Command.valueOf(this.action.toUpperCase())
                  == Command.EXTEND_TEMPLATE) {
            fieldId = -1;
          }

          fieldName = field.getString("title");
          searchable = field.getBoolean("find");
          required = field.getBoolean("required");
          maxsize = field.getJsonObject("sizefield").getString("value");
          description = field.getString("description");
          fieldtypeid = field.getInt("fieldtypeid");

          try {
            //just in case the user has entered shit
            Double.parseDouble(maxsize);
            //sanitize fucking maxsize
            maxsize = (!"".equals(maxsize)) ? maxsize : "0";
          } catch (NumberFormatException e) {
            maxsize = "0";
          }

          Fields f = new Fields(fieldId, tableId, fieldName,
                  "VARCHAR(50)", Integer.parseInt(maxsize),
                  (short) ((searchable) ? 1 : 0), (short) ((required) ? 1 : 0),
                  description, fieldtypeid);
          f.setFieldTypes(new FieldTypes(fieldtypeid));

          //get the predefined values of the field if it is a yes/no field or a dropdown list field
          if (fieldtypeid != 1) {

            JsonArray predefinedFieldValues = field.getJsonArray(
                    "fieldtypeContent");
            List<FieldPredefinedValues> ll = new LinkedList<>();

            for (JsonValue predefinedFieldValue : predefinedFieldValues) {

              JsonObject defaultt = Json.createReader(new StringReader(
                      predefinedFieldValue.toString())).readObject();
              String defaultValue = defaultt.getString("value");

              FieldPredefinedValues predefValue = new FieldPredefinedValues(-1,
                      f.getId(), defaultValue);
              //predefValue.setFields(field);
              ll.add(predefValue);
            }

            f.setFieldPredefinedValues(ll);
          }

          table.addField(f);

        } catch (NullPointerException e) {
          System.err.println("-- find is null mapping to " + (false));
          searchable = false;
          required = false;
        }
      }
      tlist.add(table);
    }
    return tlist;
  }

  @Override
  public String getAction() {
    return this.action;
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
            + "\"action\": \"" + this.action + "\", "
            + "\"type\": \"" + this.TYPE + "\", "
            + "\"message\": \"" + this.message + "\"}";
  }

}
