package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Metadata;
import se.kth.meta.entity.RawData;

/**
 * A message requesting to store metadata
 * <p>
 * @author vangelis
 */
public class StoreMetadataMessage extends MetadataMessage {

  private static final Logger logger = Logger.
          getLogger(StoreMetadataMessage.class.getName());

  private int datasetId;
  private int inodeId;

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

  public Message getMetadataLogMessage() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    //initialize the projectid and projectInodeid - the actual parent node
    this.datasetId = obj.getInt("parentid");
    this.inodeId = obj.getInt("inodeid");
    
    String json = "{\"datasetid\": " + this.datasetId + ", \"inodeid\": " + this.inodeId + "}";
    MetadataLogMessage msg = new MetadataLogMessage();
    msg.setMessage(json);
    
    return msg;
  }

  //returns the inode id and table id wrapped in an entity class in a list
  public List<EntityIntf> superParseSchema() {
    return super.parseSchema();
  }

  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    JsonObject meta = obj.getJsonObject("metadata");
    List<EntityIntf> data = new LinkedList<>();

    Set<Entry<String, JsonValue>> set = meta.entrySet();
    /*
     * processes a json string of the following format:
     * "{"1":"singleTwo","2":[{"value":"multiOne"},{"value":"multiThree"}],"1025":"55","inodeid":5,"tableid":1}"}
     *
     * Numbers are field ids and the strings are the values they carry. Values
     * may be single or multi strings
     */
    for (Entry<String, JsonValue> entry : set) {

      //avoid the inodeid field as it has been accessed previously
      //keys are the field ids
      if (isNumeric(entry.getKey())) {
        RawData raw = new RawData();
        List<Metadata> metalist = new LinkedList<>();

        //set the field id
        int fieldid = Integer.parseInt(entry.getKey());
        raw.getRawdataPK().setFieldid(fieldid);

        if (entry.getValue().getValueType() == JsonValue.ValueType.STRING) {
          //set the actual metadata
          Metadata metadata = new Metadata();
          metadata.getMetadataPK().setFieldid(fieldid);
          metadata.setData(entry.getValue().toString().replaceAll("\"", ""));
          metalist.add(metadata);

          raw.setMetadata(metalist);
        } else if (entry.getValue().getValueType() == JsonValue.ValueType.ARRAY) {
          /*
           * build a json array out of the string and then iterate through it
           * to get the actual values
           */
          JsonArray array = Json.
                  createReader(new StringReader(entry.getValue().toString())).
                  readArray();
          /*
           * in case the multiselect values are not there, it means the user
           * didn't select anything so skip this part.
           * Avoids adding en empty line to the metadata table
           */
          if (array.size() == 0) {
            continue;
          }

          //scan the array and extract the values
          for (JsonValue value : array) {
            JsonObject object = Json.
                    createReader(new StringReader(value.toString())).
                    readObject();
            //set the actual metadata
            Metadata metadata = new Metadata();
            metadata.getMetadataPK().setFieldid(fieldid);
            metadata.setData(object.getString("value").replaceAll("\"", ""));
            metalist.add(metadata);
          }
          raw.setMetadata(metalist);
        }
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
