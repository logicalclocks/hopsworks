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

  public void setHits(Map<String, Object> source) {
    this.map = new HashMap<>(source);
  }

  public Map<String, Object> getHits() {
    return this.map;
  }
}
