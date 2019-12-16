/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.dao.kafka;

public final class KafkaConst {
  public static final String COLON_SEPARATOR = ":";
  public static final String SLASH_SEPARATOR = "//";
  public static final String KAFKA_SECURITY_PROTOCOL = "SSL";
  public static final String KAFKA_BROKER_EXTERNAL_PROTOCOL = "EXTERNAL";
  public static final String PROJECT_DELIMITER = "__";
  public static final String DLIMITER = "[\"]";
  public static final String KAFKA_ENDPOINT_IDENTIFICATION_ALGORITHM = "";
  
  public static final String INFERENCE_SCHEMA_VERSION_1 = "{\"fields\": [{\"name\": \"modelId\", \"type\": \"int\"}, " +
    "{ \"name\": \"modelName\", \"type\": \"string\" }, {  \"name\": \"modelVersion\",  \"type\": \"int\" }, " +
    "{  \"name\": \"requestTimestamp\",  \"type\": \"long\" }, {  \"name\": \"responseHttpCode\",  \"type\": " +
    "\"int\" }, {  \"name\": \"inferenceRequest\",  \"type\": \"string\" }, {  \"name\": \"inferenceResponse\"," +
    "  \"type\": \"string\" }  ],  \"name\": \"inferencelog\",  \"type\": \"record\" }";
  public static final String INFERENCE_SCHEMA_VERSION_2 = "{\"fields\": [{\"name\": \"modelId\", \"type\": \"int\"}, " +
    "{ \"name\": \"modelName\", \"type\": \"string\" }, {  \"name\": \"modelVersion\",  \"type\": \"int\" }, " +
    "{  \"name\": \"requestTimestamp\",  \"type\": \"long\" }, {  \"name\": \"responseHttpCode\",  " +
    "\"type\": \"int\" }, {  \"name\": \"inferenceRequest\",  \"type\": \"string\" }, {  " +
    "\"name\": \"inferenceResponse\",  \"type\": \"string\" }, { \"name\": \"servingType\", \"type\": \"string\" } ]," +
    "  \"name\": \"inferencelog\",  \"type\": \"record\" }";
  
  public static String buildPrincipalName(String projectName, String userName) {
    return projectName + PROJECT_DELIMITER + userName;
  }
  
  public static String getProjectNameFromPrincipal(String principal) {
    return principal.split(KafkaConst.PROJECT_DELIMITER)[0];
  }
  
}
