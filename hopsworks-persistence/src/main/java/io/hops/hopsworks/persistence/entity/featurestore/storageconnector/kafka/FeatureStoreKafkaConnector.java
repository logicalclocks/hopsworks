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

package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka;

import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "feature_store_kafka_connector", catalog = "hopsworks")
@XmlRootElement
public class FeatureStoreKafkaConnector implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @NotNull
  @Column(name = "bootstrap_servers")
  private String bootstrapServers;
  @NotNull
  @Column(name = "security_protocol")
  private SecurityProtocol securityProtocol;
  @JoinColumns({@JoinColumn(name = "ssl_secret_uid", referencedColumnName = "uid"),
    @JoinColumn(name = "ssl_secret_name", referencedColumnName = "secret_name")})
  @ManyToOne(cascade = CascadeType.ALL)
  private Secret sslSecret;
  @Column(name = "ssl_endpoint_identification_algorithm")
  private SSLEndpointIdentificationAlgorithm sslEndpointIdentificationAlgorithm;
  @Column(name = "options")
  private String options;
  @JoinColumns({
    @JoinColumn(name = "truststore_inode_pid",
      referencedColumnName = "parent_id"),
    @JoinColumn(name = "truststore_inode_name",
      referencedColumnName = "name"),
    @JoinColumn(name = "truststore_partition_id",
      referencedColumnName = "partition_id")})
  @ManyToOne
  private Inode truststoreInode;
  @JoinColumns({
    @JoinColumn(name = "keystore_inode_pid",
      referencedColumnName = "parent_id"),
    @JoinColumn(name = "keystore_inode_name",
      referencedColumnName = "name"),
    @JoinColumn(name = "keystore_partition_id",
      referencedColumnName = "partition_id")})
  @ManyToOne
  private Inode keystoreInode;
  
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
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
  
  public Secret getSslSecret() {
    return sslSecret;
  }
  
  public void setSslSecret(Secret sslSecret) {
    this.sslSecret = sslSecret;
  }
  
  public SSLEndpointIdentificationAlgorithm getSslEndpointIdentificationAlgorithm() {
    return sslEndpointIdentificationAlgorithm;
  }
  
  public void setSslEndpointIdentificationAlgorithm(
    SSLEndpointIdentificationAlgorithm sslEndpointIdentificationAlgorithm) {
    this.sslEndpointIdentificationAlgorithm = sslEndpointIdentificationAlgorithm;
  }
  
  public String getOptions() {
    return options;
  }
  
  public void setOptions(String options) {
    this.options = options;
  }

  public Inode getTruststoreInode() {
    return truststoreInode;
  }

  public void setTruststoreInode(Inode truststore_inode) {
    this.truststoreInode = truststore_inode;
  }

  public Inode getKeystoreInode() {
    return keystoreInode;
  }

  public void setKeystoreInode(Inode keystore_inode) {
    this.keystoreInode = keystore_inode;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureStoreKafkaConnector that = (FeatureStoreKafkaConnector) o;
    return Objects.equals(id, that.id) && Objects.equals(bootstrapServers, that.bootstrapServers) &&
      securityProtocol == that.securityProtocol &&
      Objects.equals(truststoreInode, that.truststoreInode) &&
      Objects.equals(sslSecret, that.sslSecret) &&
      Objects.equals(keystoreInode, that.keystoreInode) &&
      sslEndpointIdentificationAlgorithm == that.sslEndpointIdentificationAlgorithm &&
      Objects.equals(options, that.options);
  }
  
  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (bootstrapServers != null ? bootstrapServers.hashCode() : 0);
    result = 31 * result + (securityProtocol != null ? securityProtocol.hashCode() : 0);
    result = 31 * result + (truststoreInode != null ? truststoreInode.hashCode() : 0);
    result = 31 * result + (sslSecret != null ? sslSecret.hashCode() : 0);
    result = 31 * result + (keystoreInode != null ? keystoreInode.hashCode() : 0);
    result = 31 * result + (sslEndpointIdentificationAlgorithm != null ? sslEndpointIdentificationAlgorithm.hashCode() :
      0);
    result = 31 * result + (options != null ? options.hashCode() : 0);
    return result;
  }
}
