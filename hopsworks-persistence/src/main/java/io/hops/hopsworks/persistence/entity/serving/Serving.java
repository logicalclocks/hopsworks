/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.serving;

import io.hops.hopsworks.persistence.entity.kafka.ProjectTopics;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "serving", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Serving.findAll", query = "SELECT t FROM Serving t"),
    @NamedQuery(name = "Serving.findById", query = "SELECT t FROM Serving t WHERE t.id = :id"),
    @NamedQuery(name = "Serving.findByProject", query = "SELECT t FROM Serving t " +
      "WHERE t.project = :project"),
    @NamedQuery(name = "Serving.findByProjectAndId", query = "SELECT t FROM Serving t " +
      "WHERE t.project = :project AND t.id = :id"),
    @NamedQuery(name = "Serving.findByCreated", query = "SELECT t FROM Serving t WHERE t.created = :created"),
    @NamedQuery(name = "Serving.findLocalhostRunning", query = "SELECT t FROM Serving t WHERE t.localPid != -2"),
    @NamedQuery(name = "Serving.expiredLocks", query = "SELECT t FROM Serving t " +
        "WHERE t.lockTimestamp is not NULL AND t.lockTimestamp < :lockts"),
    @NamedQuery(name = "Serving.findByProjectAndName", query = "SELECT t FROM Serving t " +
      "WHERE t.name = :name AND t.project = :project")})
public class Serving implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @JoinColumn(name = "creator", referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users creator;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "artifact_path")
  private String artifactPath;
  @Basic(optional = false)
  @NotNull
  @Column(name = "version")
  private Integer version;
  @Basic(optional = false)
  @NotNull
  @Column(name = "optimized")
  private boolean optimized;
  @Column(name = "instances")
  private Integer instances;
  @JoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  @Column(name = "enable_batching")
  private Boolean batchingEnabled;

  @Column(name = "lock_ip")
  private String lockIP;
  @Column(name = "lock_timestamp")
  private Long lockTimestamp;

  @JoinColumn(name = "kafka_topic_id", referencedColumnName = "id")
  @ManyToOne
  private ProjectTopics kafkaTopic;

  @Basic(optional = true)
  @Column(name = "local_port")
  private Integer localPort;
  @Basic(optional = true)
  @Column(name = "local_pid")
  private Integer localPid;
  @Basic(optional = true)
  @Column(name = "local_dir")
  private String localDir;
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "serving_type")
  private ServingType servingType = ServingType.TENSORFLOW;

  public Serving() { }

  public Serving(Integer id, String name, String artifactPath, Integer version,
                 Integer nInstances, Boolean batchingEnabled, ServingType servingType) {
    this.id = id;
    this.name = name;
    this.artifactPath = artifactPath;
    this.version = version;
    this.instances = nInstances;
    this.batchingEnabled = batchingEnabled;
    this.servingType = servingType;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getArtifactPath() {
    return artifactPath;
  }

  public void setArtifactPath(String artifactPath) {
    this.artifactPath = artifactPath;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Integer getInstances() {
    return instances;
  }

  public void setInstances(Integer instances) {
    this.instances = instances;
  }

  public boolean isOptimized() {
    return optimized;
  }

  public void setOptimized(boolean optimized) {
    this.optimized = optimized;
  }
  
  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Integer getLocalPort() {
    return localPort;
  }

  public void setLocalPort(Integer localPort) {
    this.localPort = localPort;
  }

  public Integer getLocalPid() {
    return localPid;
  }

  public void setLocalPid(Integer localPid) {
    this.localPid = localPid;
  }

  public String getLocalDir() {
    return localDir;
  }

  public void setLocalDir(String localDir) {
    this.localDir = localDir;
  }

  public Boolean isBatchingEnabled() {
    return batchingEnabled;
  }

  public void setBatchingEnabled(Boolean batching) {
    this.batchingEnabled = batching;
  }

  public String getLockIP() {
    return lockIP;
  }

  public void setLockIP(String lockIP) {
    this.lockIP = lockIP;
  }

  public Long getLockTimestamp() {
    return lockTimestamp;
  }

  public void setLockTimestamp(Long lockTimestamp) {
    this.lockTimestamp = lockTimestamp;
  }

  public ProjectTopics getKafkaTopic() {
    return kafkaTopic;
  }

  public void setKafkaTopic(ProjectTopics kafkaTopic) {
    this.kafkaTopic = kafkaTopic;
  }

  public ServingType getServingType() {
    return servingType;
  }

  public void setServingType(ServingType servingType) {
    this.servingType = servingType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Serving serving = (Serving) o;

    if (optimized != serving.optimized) return false;
    if (batchingEnabled != serving.batchingEnabled) return false;
    if (id != null ? !id.equals(serving.id) : serving.id != null) return false;
    if (created != null ? !created.equals(serving.created) : serving.created != null) return false;
    if (creator != null ? !creator.equals(serving.creator) : serving.creator != null) return false;
    if (!name.equals(serving.name)) return false;
    if (!artifactPath.equals(serving.artifactPath)) return false;
    if (!version.equals(serving.version)) return false;
    if (instances != null ? !instances.equals(serving.instances) : serving.instances != null) return false;
    if (project != null ? !project.equals(serving.project) : serving.project != null) return false;
    if (lockIP != null ? !lockIP.equals(serving.lockIP) : serving.lockIP != null) return false;
    if (lockTimestamp != null ? !lockTimestamp.equals(serving.lockTimestamp) : serving.lockTimestamp != null)
      return false;
    if (kafkaTopic != null ? !kafkaTopic.equals(serving.kafkaTopic) : serving.kafkaTopic != null) return false;
    if (localPort != null ? !localPort.equals(serving.localPort) : serving.localPort != null) return false;
    if (localPid != null ? !localPid.equals(serving.localPid) : serving.localPid != null) return false;
    if (servingType != null ? !servingType.equals(serving.servingType) : serving.servingType != null) return false;
    return localDir != null ? localDir.equals(serving.localDir) : serving.localDir == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (creator != null ? creator.hashCode() : 0);
    result = 31 * result + name.hashCode();
    result = 31 * result + artifactPath.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + (optimized ? 1 : 0);
    result = 31 * result + (instances != null ? instances.hashCode() : 0);
    result = 31 * result + (project != null ? project.hashCode() : 0);
    result = 31 * result + (batchingEnabled ? 1 : 0);
    result = 31 * result + (lockIP != null ? lockIP.hashCode() : 0);
    result = 31 * result + (lockTimestamp != null ? lockTimestamp.hashCode() : 0);
    result = 31 * result + (kafkaTopic != null ? kafkaTopic.hashCode() : 0);
    result = 31 * result + (localPort != null ? localPort.hashCode() : 0);
    result = 31 * result + (localPid != null ? localPid.hashCode() : 0);
    result = 31 * result + (localDir != null ? localDir.hashCode() : 0);
    result = 31 * result + (servingType!= null ? servingType.hashCode() : 0);
    return result;
  }
}
