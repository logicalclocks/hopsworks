package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Metadata;
import se.kth.meta.entity.MetadataPK;
import se.kth.meta.entity.RawData;

/**
 * A message request to update a specific raw data row.
 * <p>
 * @author vangelis
 */
public class UpdateMetadataMessage extends MetadataMessage {

  private static final Logger logger = Logger.
          getLogger(UpdateMetadataMessage.class.getName());

  public UpdateMetadataMessage() {
    super();
    this.TYPE = "UpdateMetadataMessage";
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

  /**
   * parses the incoming message and returns a RawData object wrapped in a list.
   * <p>
   * @return 
   */
  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    try {
      List<EntityIntf> data = new LinkedList<>();

      int metaid = obj.getInt("metaid");
      String metadata = obj.getString("metadata");
      
      Metadata mtd = new Metadata();
      MetadataPK metaPrimaryKey = new MetadataPK();
      metaPrimaryKey.setId(metaid);
      
      mtd.setMetadataPK(metaPrimaryKey);
      mtd.setData(metadata);
      
      data.add(mtd);
      return data;
      
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE, "Raw id or data was not present in the message");
    }
    return null;
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