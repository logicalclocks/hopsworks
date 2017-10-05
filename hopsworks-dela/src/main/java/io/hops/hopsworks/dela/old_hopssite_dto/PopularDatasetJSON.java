package io.hops.hopsworks.dela.old_hopssite_dto;

import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.dela.old_dto.ManifestJSON;

@XmlRootElement
public class PopularDatasetJSON {

  private ManifestJSON manifestJson;

  private String datasetId;

  private int leeches;

  private int seeds;

  public PopularDatasetJSON(ManifestJSON manifestJson, String datasetId,
          int leeches, int seeds) {
    this.manifestJson = manifestJson;
    this.datasetId = datasetId;
    this.leeches = leeches;
    this.seeds = seeds;
  }

  public PopularDatasetJSON() {
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public void setLeeches(int leeches) {
    this.leeches = leeches;
  }

  public void setSeeds(int seeds) {
    this.seeds = seeds;
  }

  public ManifestJSON getManifestJson() {
    return manifestJson;
  }

  public void setManifestJson(ManifestJSON manifestJson) {
    this.manifestJson = manifestJson;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public int getLeeches() {
    return leeches;
  }

  public int getSeeds() {
    return seeds;
  }

}
