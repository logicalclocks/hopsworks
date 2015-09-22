package se.kth.hopsworks.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import se.kth.hopsworks.meta.entity.EntityIntf;
import se.kth.hopsworks.meta.entity.InodeTableComposite;

/**
 * Represents a generic metadata message. It may be about fetching table
 * metadata or inode metadata, or about updating table metadata depending on its
 * subclasses
 * <p>
 * @author vangelis
 */
public class MetadataMessage implements Message {

  private static final Logger logger = Logger.
          getLogger(MetadataMessage.class.getName());

  protected String TYPE = "MetadataMessage";
  protected String sender;
  protected String message;
  protected String action;
  protected String status;

  public MetadataMessage() {
    this.status = "OK";
  }

  @Override
  public void init(JsonObject obj) {
    this.sender = obj.getString("sender");
    this.message = obj.getString("message");
    this.action = obj.getString("action");
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
  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  /**
   * Instantiates the custom entity InodeTableComposite to pass table id and
   * inode id to protocol. This way raw data can be filtered by table id and
   * inode id and be grouped in the front end.
   *
   * @return the list with the RawData objects
   */
  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    List<EntityIntf> list = null;

    try {
      int inodePid = obj.getInt("inodepid");
      String inodeName = obj.getString("inodename");
      int tableid = obj.getInt("tableid");

      InodeTableComposite itc = new InodeTableComposite(tableid, inodePid,
              inodeName);

      list = new LinkedList<>();
      list.add(itc);
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE,
              "Inodepid or inodename or tableid not present in the message");
    }

    return list;
  }

  @Override
  public String buildSchema(List<EntityIntf> entity) {
    throw new UnsupportedOperationException("Not necessary for this message.");
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

}
