package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.RawData;

/**
 *
 * @author vangelis
 */
public class UpdateMetadataMessage extends MetadataMessage {

  private static final Logger logger = Logger.
          getLogger(UpdateMetadataMessage.class.getName());

  public UpdateMetadataMessage() {
    super();
    super.TYPE = "UpdateMetadataMessage";
  }

  /**
   * Used to send custom messages
   *
   * @param sender the message sender
   * @param message the actual message
   */
  public UpdateMetadataMessage(String sender, String message) {
    this();
    this.sender = sender;
    this.message = message;
  }

  //returns the inode id and table id wrapped in an entity class in a list
  public List<EntityIntf> superParseSchema() {
    return super.parseSchema();
  }

  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    try {
      List<EntityIntf> data = new LinkedList<>();

      int rawId = obj.getInt("rawid");
      String rawdata = obj.getString("rawdata");
      
      RawData raw = new RawData();
      raw.setId(rawId);
      raw.setData(rawdata);
      
      data.add(raw);
      return data;
      
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE, "Raw id or data was not present in the message");
    }
    return null;
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
  public String toString() {
    return "{\"sender\": \"" + this.sender + "\", "
            + "\"type\": \"" + this.TYPE + "\", "
            + "\"status\": \"" + this.status + "\", "
            + "\"action\": \"" + this.action + "\", "
            + "\"message\": \"" + this.message + "\"}";
  }
}
