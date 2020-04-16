package io.hops.hopsworks.common.featurestore.trainingdatasets.split;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * DTO containing the human-readable information of a training dataset split, can be converted to JSON or XML
 * representation using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"name", "percentage"})
public class TrainingDatasetSplitDTO {
  @XmlElement
  private String name;
  @XmlElement
  private float percentage;
  
  public TrainingDatasetSplitDTO(){}
  
  public TrainingDatasetSplitDTO(String name, float percentage) {
    this.name = name;
    this.percentage = percentage;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public float getPercentage() {
    return percentage;
  }
  
  public void setPercentage(float percentage) {
    this.percentage = percentage;
  }
  
  @Override
  public String toString() {
    return "TrainingDatasetSplitDTO{" +
      "name='" + name + '\'' +
      ", percentage=" + percentage +
      '}';
  }
}
