package io.hops.hopsworks.api.metadata.wscomm.message;

import io.hops.hopsworks.common.dao.metadata.EntityIntf;
import io.hops.hopsworks.common.util.JsonUtil;
import java.util.List;
import java.util.logging.Logger;

/**
 * A message requesting to store metadata
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
