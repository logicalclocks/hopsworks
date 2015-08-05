package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.MTable;

/**
 *
 * @author vangelis
 */
public class TableMetadataMessage extends MetadataMessage {

  private static final Logger logger = Logger.
          getLogger(TableMetadataMessage.class.getName());

  public TableMetadataMessage() {
    super();
    super.TYPE = "TableMetadataMessage";
  }

  /**
   * Used to send custom messages
   *
   * @param sender the message sender
   * @param message the actual message
   */
  public TableMetadataMessage(String sender, String message) {
    this();
    this.sender = sender;
    this.message = message;
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
            + "\"message\": \"" + this.message + "\"}";
  }
}
