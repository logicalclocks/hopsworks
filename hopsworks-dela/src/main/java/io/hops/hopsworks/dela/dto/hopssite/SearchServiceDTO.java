package io.hops.hopsworks.dela.dto.hopssite;

import io.hops.hopsworks.dela.dto.common.ClusterAddressDTO;
import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

public class SearchServiceDTO {
  @XmlRootElement
  public static class Params implements Serializable {

    private String searchTerm;

    public Params() {
    }

    public Params(String searchTerm) {
      this.searchTerm = searchTerm;
    }

    public String getSearchTerm() {
      return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
      this.searchTerm = searchTerm;
    }
  }

  @XmlRootElement
  public static class SearchResult implements Serializable {

    private String sessionId;
    private int nrHits;

    public SearchResult() {
    }

    public SearchResult(String sessionId, int nrHits) {
      this.sessionId = sessionId;
      this.nrHits = nrHits;
    }

    public String getSessionId() {
      return sessionId;
    }

    public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
    }

    public int getNrHits() {
      return nrHits;
    }

    public void setNrHits(int nrHits) {
      this.nrHits = nrHits;
    }
  }

  @XmlRootElement
  public static class Item implements Serializable {

    private String publicDSId;
    private DatasetDTO.Search dataset;
    private float score;

    public Item() {
    }

    public Item(String publicDSId, DatasetDTO.Search dataset, float score) {
      this.publicDSId = publicDSId;
      this.dataset = dataset;
      this.score = score;
    }

    public String getPublicDSId() {
      return publicDSId;
    }

    public void setPublicDSId(String publicDSId) {
      this.publicDSId = publicDSId;
    }

    public DatasetDTO.Search getDataset() {
      return dataset;
    }

    public void setDataset(DatasetDTO.Search dataset) {
      this.dataset = dataset;
    }

    public float getScore() {
      return score;
    }

    public void setScore(float score) {
      this.score = score;
    }
  }
  
  public static class ItemDetails implements Serializable {
    private DatasetDTO.Details dataset;
    private List<ClusterAddressDTO> bootstrap;

    public ItemDetails() {
    }

    public ItemDetails(DatasetDTO.Details dataset, List<ClusterAddressDTO> bootstrap) {
      this.dataset = dataset;
      this.bootstrap = bootstrap;
    }

    public DatasetDTO.Details getDataset() {
      return dataset;
    }

    public void setDataset(DatasetDTO.Details dataset) {
      this.dataset = dataset;
    }

    public List<ClusterAddressDTO> getBootstrap() {
      return bootstrap;
    }

    public void setBootstrap(List<ClusterAddressDTO> bootstrap) {
      this.bootstrap = bootstrap;
    }
  }
}
