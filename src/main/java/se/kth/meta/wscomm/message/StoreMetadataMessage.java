package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
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
  public List<EntityIntf> superParseSchema() {
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
      //keys are the field ids
      if (isNumeric(entry.getKey())) {
        //set the field id and the actual data
        raw.setFieldid(Integer.parseInt(entry.getKey()));

        if (entry.getValue().getValueType() == JsonValue.ValueType.STRING) {
          raw.setData(entry.getValue().toString());
        } else if (entry.getValue().getValueType() == JsonValue.ValueType.ARRAY) {
          /*
           * build a json array out of the string and then iterate through it
           * to get the actual values
           */
          JsonArray array = Json.
                  createReader(new StringReader(entry.getValue().toString())).
                  readArray();

          String multiValues = "";
          /*
           * in case the multiselect values are not there, it means the user
           * didn't select anything so skip this part.
           * Avoids adding en empty line to the metadata table
           */
          int length = array.size();
          if (length == 0) {
            continue;
          }

          //scan the array and extract the values
          for (JsonValue value : array) {
            JsonObject object = Json.
                    createReader(new StringReader(value.toString())).
                    readObject();

            multiValues += object.getString("value") + "-";
          }
          multiValues = multiValues.substring(0, multiValues.length() - 2);
          raw.setData(multiValues);
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
