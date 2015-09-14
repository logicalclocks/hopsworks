package se.kth.hopsworks.rest;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.elasticsearch.search.SearchHit;

/**
 * Represents a JSONifiable version of the elastic hit object
 * <p>
 * @author vangelis
 */
@XmlRootElement
public class ElasticHit {
  
  private SearchHit hit;
  private String id;
  private Map<String, Object> source;
  
  public ElasticHit(){
  }
  
  public ElasticHit(SearchHit hit){
    this.hit = hit;
    this.id = hit.getId();
    this.source = hit.getSource();
  }
  
  @JsonIgnore
  public void setHit(SearchHit hit){
    this.hit = hit;
  }
  
  @JsonIgnore
  public SearchHit getHit(){
    return this.hit;
  }
  
  public void setId(String id){
    this.id = id;
  }
  
  public String getId(){
    return this.id;
  }
  
  public void setSource(Map<String, Object> source){
    this.source = new HashMap<>(source);
  }
  
  public Map<String, Object> getSource(){
    return this.source;
  }
}
