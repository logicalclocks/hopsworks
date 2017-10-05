package io.hops.hopsworks.dela.old_dto;

import java.util.List;

public class ExtendedDetails {

  private List<HdfsDetails> hdfsDetails;
  private List<KafkaDetails> kafkaDetails;

  public ExtendedDetails() {
  }

  public ExtendedDetails(List<HdfsDetails> hdfsDetails, List<KafkaDetails> kafkaDetails) {
    this.hdfsDetails = hdfsDetails;
    this.kafkaDetails = kafkaDetails;
  }

  public List<HdfsDetails> getHdfsDetails() {
    return hdfsDetails;
  }

  public void setHdfsDetails(List<HdfsDetails> hdfsDetails) {
    this.hdfsDetails = hdfsDetails;
  }

  public List<KafkaDetails> getKafkaDetails() {
    return kafkaDetails;
  }

  public void setKafkaDetails(List<KafkaDetails> kafkaDetails) {
    this.kafkaDetails = kafkaDetails;
  }
}
