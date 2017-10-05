package io.hops.hopsworks.dela.exception;

import io.hops.hopsworks.common.exception.AppException;

public class ThirdPartyException extends AppException {

  private Source source;
  private String sourceDetails;
  
  public ThirdPartyException(int status, String msg, Source source, String sourceDetails) {
    super(status, msg);
    this.source = source;
    this.sourceDetails = sourceDetails;
  }

  public Source getSource() {
    return source;
  }

  public void setSource(Source source) {
    this.source = source;
  }

  public String getSourceDetails() {
    return sourceDetails;
  }

  public void setSourceDetails(String sourceDetails) {
    this.sourceDetails = sourceDetails;
  }
  
  public static enum Source {
    LOCAL, SETTINGS, MYSQL, HDFS, KAFKA, DELA, REMOTE_DELA, HOPS_SITE
  }
  
  public static ThirdPartyException from(AppException ex, Source source, String sourceDetails) {
    return new ThirdPartyException(ex.getStatus(), ex.getMessage(), source, sourceDetails);
  }
  
  public static enum Error {
    CLUSTER_NOT_REGISTERED("cluster not registered"),
    HEAVY_PING("heavy ping required"),
    USER_NOT_REGISTERED("user not registered"), 
    DATASET_EXISTS("dataset exists"),
    DATASET_DOES_NOT_EXIST("dataset does not exist");
    
    
    private final String msg;
    
    Error(String msg) {
      this.msg = msg;
    }
    
    public boolean is(String msg) {
      return this.msg.equals(msg);
    }

    @Override
    public String toString() {
      return msg;
    }
  }
}
