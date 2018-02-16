/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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
