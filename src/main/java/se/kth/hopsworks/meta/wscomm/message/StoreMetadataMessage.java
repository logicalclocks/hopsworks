package se.kth.hopsworks.meta.wscomm.message;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import se.kth.hopsworks.meta.entity.EntityIntf;
import se.kth.hopsworks.util.JsonUtil;

/**
 * A message requesting to store metadata
 * <p/>
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

  //returns the inode primary key and table id wrapped in an entity class in a list
  public List<EntityIntf> superParseSchema() {
    return super.parseSchema();
  }

  @Override
  public List<EntityIntf> parseSchema() {
    return JsonUtil.parseSchemaPayload(this.message);
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
