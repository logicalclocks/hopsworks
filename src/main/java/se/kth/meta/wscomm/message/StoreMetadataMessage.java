package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.RawData;

/**
 * A message requesting to store metadata
 * <p>
 * @author vangelis
 */
public class StoreMetadataMessage extends MetadataMessage {

  private static final Logger logger = Logger.
          getLogger(StoreMetadataMessage.class.getName());

  public StoreMetadataMessage() {
    super();
    super.TYPE = "StoreMetadataMessage";
  }
  
  /**
   * Used to send custom messages
   *
   * @param sender the message sender
   * @param message the actual message
   */
  public StoreMetadataMessage(String sender, String message) {
    this();
    this.sender = sender;
    this.message = message;
  }

  //returns the inode id and table id wrapped in an entity class in a list
  public List<EntityIntf> superParseSchema(){
    return super.parseSchema();
  }
  
  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    List<EntityIntf> data = new LinkedList<>();

    Set<Entry<String, JsonValue>> set = obj.entrySet();

    for (Entry<String, JsonValue> entry : set) {
      RawData raw = new RawData();

      //avoid the inodeid field as it has been accessed previously
      if (isNumeric(entry.getKey())) {
        //set the field id and the actual data
        raw.setFieldid(Integer.parseInt(entry.getKey()));
        raw.setData(entry.getValue().toString());
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
  public String toString() {
    return "{\"sender\": \"" + this.sender + "\", "
            + "\"type\": \"" + this.TYPE + "\", "
            + "\"status\": \"" + this.status + "\", "
            + "\"action\": \"" + this.action + "\", "
            + "\"message\": \"" + this.message + "\"}";
  }
}
