package io.hops.hopsworks.common.dao.kafka;

public final class KafkaConst {
  public static final String COLON_SEPARATOR = ":";
  public static final String SLASH_SEPARATOR = "//";
  public static final String KAFKA_SECURITY_PROTOCOL = "SSL";
  public static final String KAFKA_BROKER_EXTERNAL_PROTOCOL = "EXTERNAL";
  public static final String PROJECT_DELIMITER = "__";
  public static final String DLIMITER = "[\"]";
  public static final String KAFKA_ENDPOINT_IDENTIFICATION_ALGORITHM = "";
  
  public static String buildPrincipalName(String projectName, String userName) {
    return projectName + PROJECT_DELIMITER + userName;
  }
  
}
