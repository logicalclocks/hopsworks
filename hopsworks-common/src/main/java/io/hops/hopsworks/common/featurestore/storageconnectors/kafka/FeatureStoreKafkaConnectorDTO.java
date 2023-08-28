/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.kafka;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.FeatureStoreKafkaConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.SecurityProtocol;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@JsonTypeInfo(
  use=JsonTypeInfo.Id.NAME,
  include=JsonTypeInfo.As.PROPERTY,
  property="type")
@JsonTypeName("featureStoreKafkaConnectorDTO")
public class FeatureStoreKafkaConnectorDTO extends FeaturestoreStorageConnectorDTO {
  private String bootstrapServers;
  private SecurityProtocol securityProtocol;
  private String sslTruststoreLocation;
  private String sslTruststorePassword;
  private String sslKeystoreLocation;
  private String sslKeystorePassword;
  private String sslKeyPassword;
  private String sslEndpointIdentificationAlgorithm;
  private List<OptionDTO> options = new ArrayList<>();
  private Boolean externalKafka;
  
  public FeatureStoreKafkaConnectorDTO() {
  }
  
  public FeatureStoreKafkaConnectorDTO(FeaturestoreConnector featureStoreConnector) {
    super(featureStoreConnector);
    FeatureStoreKafkaConnector kafkaConnector = featureStoreConnector.getKafkaConnector();
    this.bootstrapServers = kafkaConnector.getBootstrapServers();
    this.securityProtocol = kafkaConnector.getSecurityProtocol();
    this.sslEndpointIdentificationAlgorithm = kafkaConnector.getSslEndpointIdentificationAlgorithm().getValue();
  }
  
  public String getBootstrapServers() {
    return bootstrapServers;
  }
  
  public void setBootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }
  
  public SecurityProtocol getSecurityProtocol() {
    return securityProtocol;
  }
  
  public void setSecurityProtocol(
    SecurityProtocol securityProtocol) {
    this.securityProtocol = securityProtocol;
  }
  
  public String getSslTruststoreLocation() {
    return sslTruststoreLocation;
  }
  
  public void setSslTruststoreLocation(String sslTruststoreLocation) {
    this.sslTruststoreLocation = sslTruststoreLocation;
  }
  
  public String getSslTruststorePassword() {
    return sslTruststorePassword;
  }
  
  public void setSslTruststorePassword(String sslTruststorePassword) {
    this.sslTruststorePassword = sslTruststorePassword;
  }
  
  public String getSslKeystoreLocation() {
    return sslKeystoreLocation;
  }
  
  public void setSslKeystoreLocation(String sslKeystoreLocation) {
    this.sslKeystoreLocation = sslKeystoreLocation;
  }
  
  public String getSslKeystorePassword() {
    return sslKeystorePassword;
  }
  
  public void setSslKeystorePassword(String sslKeystorePassword) {
    this.sslKeystorePassword = sslKeystorePassword;
  }
  
  public String getSslKeyPassword() {
    return sslKeyPassword;
  }
  
  public void setSslKeyPassword(String sslKeyPassword) {
    this.sslKeyPassword = sslKeyPassword;
  }
  
  public String getSslEndpointIdentificationAlgorithm() {
    return sslEndpointIdentificationAlgorithm;
  }
  
  public void setSslEndpointIdentificationAlgorithm(
    String sslEndpointIdentificationAlgorithm) {
    this.sslEndpointIdentificationAlgorithm = sslEndpointIdentificationAlgorithm;
  }
  
  public List<OptionDTO> getOptions() {
    return options;
  }
  
  public void setOptions(List<OptionDTO> options) {
    this.options = options;
  }

  public Boolean isExternalKafka() {
    return externalKafka;
  }

  public void setExternalKafka(Boolean externalKafka) {
    this.externalKafka = externalKafka;
  }
}
