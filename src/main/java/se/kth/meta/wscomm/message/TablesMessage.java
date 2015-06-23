package se.kth.meta.wscomm.message;

import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Tables;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Tables;

/**
 *
 * @author Vangelis
 */
public class TablesMessage extends ContentMessage {

  private static final Logger logger = Logger.getLogger(TablesMessage.class.
          getName());

  private final String TYPE = "TablesMessage";
  private String sender;
  private String message;
  private String action;
  private String status;

  @Override
  public void init(JsonObject json) {
    this.sender = json.getString("sender");
    this.message = json.getString("message");
    this.action = json.getString("action");
    this.setStatus("OK");
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

  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    int tableId = obj.getInt("id");
    String tableName = obj.getString("name");
    boolean forceDelete = false;

    try {
      forceDelete = obj.getBoolean("forceDelete");
      logger.log(Level.SEVERE, "FORCE DELETE ON TABLE {0}", forceDelete);
    } catch (NullPointerException e) {
    }

    Tables table = new Tables(tableId, tableName);
    table.setForceDelete(forceDelete);
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
            + "\"message\": \"" + this.message + "\"}";
  }
}
