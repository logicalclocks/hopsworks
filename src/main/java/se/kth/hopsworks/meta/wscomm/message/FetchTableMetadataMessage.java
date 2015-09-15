package se.kth.hopsworks.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import se.kth.hopsworks.meta.entity.EntityIntf;
import se.kth.hopsworks.meta.entity.MTable;

/**
 * A message asking for a table's metadata
 * <p>
 * @author vangelis
 */
public class FetchTableMetadataMessage extends FetchMetadataMessage {

  public FetchTableMetadataMessage() {
    super();
    super.TYPE = "FetchTableMetadataMessage";
  }

  /**
   * Used to send custom messages
   *
   * @param sender the message sender
   * @param message the actual message
   */
  public FetchTableMetadataMessage(String sender, String message) {
    this();
    this.sender = sender;
    this.message = message;
  }

  @Override
  public void init(JsonObject obj) {
    super.init(obj);
  }

  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    List<EntityIntf> data = new LinkedList<>();

    int tableId = obj.getInt("tableid");

    MTable table = new MTable(tableId);
    data.add(table);

    return data;
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
