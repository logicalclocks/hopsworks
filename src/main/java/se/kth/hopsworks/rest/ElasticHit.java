package se.kth.hopsworks.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.bind.annotation.XmlRootElement;
import org.elasticsearch.search.SearchHit;

/**
 * Represents a JSONifiable version of the elastic hit object
 * <p>
 * @author vangelis
 */
@XmlRootElement
public class ElasticHit {

  //the inode id
  private String id;
  //inode name 
  private String name;
  //the rest of the hit (search match) data
  private Map<String, Object> map;
  //whether the inode is a parent, a child or a dataset
  private String type;

  public ElasticHit() {
  }

  public ElasticHit(SearchHit hit) {
    //the id of the retrieved hit (i.e. the inode_id)
    this.id = hit.getId();
    //the source of the retrieved record (i.e. all the indexed information)
    this.map = hit.getSource();
    //the data type of the retrieved record. It can be 'parent', 'child' or 'dataset'
    this.type = hit.getType();

    //export the name of the retrieved record from the list
    for (Entry<String, Object> entry : map.entrySet()) {
      //set the name explicitly so that it's easily accessible in the frontend
      if (entry.getKey().equals("name")) {
        this.setName(entry.getValue().toString());
      }
      
      //logger.log(Level.FINE, "KEY -- {0} VALUE --- {1}", new Object[]{entry.getKey(), entry.getValue()});
    }
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public final void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return this.type;
  }

  public void setHits(Map<String, Object> source) {
    this.map = new HashMap<>(source);
  }

  public Map<String, String> getHits() {
    //flatten hits (remove nested json objects) to make it more readable
    Map<String, String> refined = new HashMap<>();

    for (Entry<String, Object> entry : this.map.entrySet()) {
      //convert value to string
      String value = (entry.getValue() == null) ? "null" : entry.getValue().
              toString();
      refined.put(entry.getKey(), value);
    }

    return refined;
  }
}
