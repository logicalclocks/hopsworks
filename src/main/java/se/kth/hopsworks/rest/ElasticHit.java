package se.kth.hopsworks.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.json.JsonObject;
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
  //whether the inode is a parent or child
  private String type;

  public ElasticHit() {
  }

  public ElasticHit(SearchHit hit, Map<String, Object> map) {
    this.id = hit.getId();
    this.map = map;
    
    for(Entry<String, Object> entry : map.entrySet()){
      //set the name explicitly so that it's easily accessible in the frontend
      if(entry.getKey().equals("name")){
        this.setName(entry.getValue().toString());
      }
    }
  }
  
  public ElasticHit(SearchHit hit, Map<String, Object> map, String type){
    this(hit, map);
    this.type = type;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }
  
  public final void setName(String name){
    this.name = name;
  }
  
  public String getName(){
    return this.name;
  }

  public void setType(String type){
    this.type = type;
  }
  
  public String getType(){
    return this.type;
  }
  
  public void setHits(Map<String, Object> source) {
    this.map = new HashMap<>(source);
  }

  public Map<String, String> getHits() {
    //make hits more readable before returning them
    Map<String, String> refined = new HashMap<>();
    
    for(Entry<String, Object> entry : this.map.entrySet()){
      //convert value to string
      String value = (entry.getValue() == null) ? "null" : entry.getValue().toString();
      refined.put(entry.getKey(), value);
    }
    
    return refined;
  }
}
