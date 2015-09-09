package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.HdfsMetadataLog;
import se.kth.meta.entity.HdfsMetadataLogPK;

/**
 *
 * @author vangelis
 */
public class MetadataLogMessage extends TextMessage {

  private final String TYPE = "CreateMetadataLogMessage";

  private static final Logger logger = Logger.
          getLogger(MetadataLogMessage.class.getName());

  /**
   * Default constructor. Used by the class loader to create an instance of
   * this class
   */
  public MetadataLogMessage() {
    super();
  }

  /**
   * Used to send custom messages
   *
   * @param sender the message sender
   */
  public MetadataLogMessage(String sender) {
    this();
    this.sender = sender;
  }

  public MetadataLogMessage(String sender, String message) {
    this(sender);
    this.message = message;
  }

  @Override
  public void init(JsonObject json) {

    this.sender = json.getString("sender");
    this.message = json.getString("message");
    this.action = json.getString("action");
  }

  @Override
  public String encode() {

    if (this.sender == null || this.message == null) {
      this.sender = "Unknown";
      this.message = "Nothing to do";
    }

    String value = Json.createObjectBuilder()
            .add("sender", this.sender)
            .add("type", this.TYPE)
            .add("status", this.status)
            .add("message", this.message)
            .build()
            .toString();

    return value;
  }

  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    List<EntityIntf> list = null;

    try {
      int datasetid = obj.getInt("datasetid");
      int inodeid = obj.getInt("inodeid");
      
      HdfsMetadataLog metaLog = new HdfsMetadataLog();
      HdfsMetadataLogPK pk = new HdfsMetadataLogPK(datasetid, inodeid, 0);
      metaLog.setHdfsMetadataLogPK(pk);
      list = new LinkedList<>();
      list.add(metaLog);
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE, "Inodeid path not present in the message");
    }

    return list;
  }

  @Override
  public String buildSchema(List<EntityIntf> list) {

    return "Metadata log created successfully";
  }

  @Override
  public String getAction() {
    return this.action;
  }

  @Override
  public void setSender(String sender) {
    this.sender = sender;
  }

  @Override
  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String getSender() {
    return this.sender;
  }

  @Override
  public String getMessage() {
    return this.message;
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
