/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
import javax.persistence.Convert;
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
import java.nio.file.Paths;
import java.util.Date;

@Entity
@Table(name = "serving", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Serving.findAll", query = "SELECT t FROM Serving t"),
    @NamedQuery(name = "Serving.findById", query = "SELECT t FROM Serving t WHERE t.id = :id"),
    @NamedQuery(name = "Serving.findByProject", query = "SELECT t FROM Serving t " +
      "WHERE t.project = :project"),
    @NamedQuery(name = "Serving.findByProjectAndModel", query = "SELECT t FROM Serving t " +
            "WHERE t.project = :project AND t.modelName = :modelName"),
  @NamedQuery(name = "Serving.findByProjectAndModelVersion", query = "SELECT t FROM Serving t " +
    "WHERE t.project = :project AND t.modelName = :modelName AND t.modelVersion = :modelVersion"),
    @NamedQuery(name = "Serving.findByProjectAndId", query = "SELECT t FROM Serving t " +
      "WHERE t.project = :project AND t.id = :id"),
    @NamedQuery(name = "Serving.findByCreated", query = "SELECT t FROM Serving t WHERE t.created = :created"),
    @NamedQuery(name = "Serving.findLocalhostRunning", query = "SELECT t FROM Serving t WHERE t.cid != \"stopped\""),
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
  @Size(min = 1, max = 1000)
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "model_path")
  private String modelPath;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "model_name")
  private String modelName;
  @Basic(optional = false)
  @NotNull
  @Column(name = "model_version")
  private Integer modelVersion;
  @Size(min = 1, max = 255)
  @Column(name = "predictor") // <filename>.<ext>
  private String predictor;
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "model_framework")
  private ModelFramework modelFramework;
  @Basic(optional = false)
  @NotNull
  @Column(name = "optimized")
  private boolean optimized;
  @Column(name = "instances")
  private Integer instances;
  @JoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

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
  @Column(name = "cid")
  private String cid;
  @Basic(optional = true)
  @Column(name = "local_dir")
  private String localDir;
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "model_server")
  private ModelServer modelServer = ModelServer.TENSORFLOW_SERVING;
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "serving_tool")
  private ServingTool servingTool = ServingTool.DEFAULT;
  
  @Basic(optional = true)
  @Column(name = "deployed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date deployed;
  @Basic(optional = true)
  @Size(min = 1, max = 8)
  @Column(name = "revision")
  private String revision;
  @Column(name = "batching_configuration")
  @NotNull
  @Convert(converter = BatchingConfigurationConverter.class)
  private BatchingConfiguration batchingConfiguration;

  public Serving() { }

  public Serving(Integer id, String name, String description, String modelPath, String modelName, Integer modelVersion,
                 ModelFramework modelFramework, String predictor, Integer nInstances, Boolean batchingEnabled,
                 ModelServer modelServer, ServingTool servingTool, BatchingConfiguration batchingConfiguration) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.modelPath = modelPath;
    this.modelName = modelName;
    this.modelVersion = modelVersion;
    this.predictor = predictor;
    this.modelFramework = modelFramework;
    this.instances = nInstances;
    this.modelServer = modelServer;
    this.servingTool = servingTool;
    this.batchingConfiguration = batchingConfiguration;
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
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }

  public String getModelPath() { return modelPath; }
  
  public void setModelPath(String modelPath) { this.modelPath = modelPath; }
  
  public String getModelVersionPath() {
    return Paths.get(getModelPath(), String.valueOf(this.modelVersion)).toString();
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public Integer getModelVersion() { return modelVersion; }

  public void setModelVersion(Integer modelVersion) {
    this.modelVersion = modelVersion;
  }

  public String getPredictor() {
    return predictor;
  }
  
  public void setPredictor(String predictor) {
    this.predictor = predictor;
  }

  public ModelFramework getModelFramework() { return modelFramework; }
  
  public void setModelFramework(ModelFramework modelFramework) { this.modelFramework = modelFramework; }

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

  public String getCid() {
    return cid;
  }

  public void setCid(String cid) {
    this.cid = cid;
  }

  public String getLocalDir() {
    return localDir;
  }

  public void setLocalDir(String localDir) {
    this.localDir = localDir;
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

  public ModelServer getModelServer() {
    return modelServer;
  }

  public void setModelServer(ModelServer modelServer) {
    this.modelServer = modelServer;
  }
  
  public ServingTool getServingTool() { return servingTool; }
  
  public void setServingTool(ServingTool servingTool) { this.servingTool = servingTool; }
  
  public Date getDeployed() {
    return deployed;
  }
  
  public void setDeployed(Date deployed) {
    this.deployed = deployed;
  }

  public String getRevision() {
    return revision;
  }
  
  public void setRevision(String revision) {
    this.revision = revision;
  }

  public BatchingConfiguration getBatchingConfiguration() { return batchingConfiguration; }

  public void setBatchingConfiguration(BatchingConfiguration batchingConfiguration) {
    this.batchingConfiguration = batchingConfiguration;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Serving serving = (Serving) o;

    if (optimized != serving.optimized) return false;
    if (id != null ? !id.equals(serving.id) : serving.id != null) return false;
    if (created != null ? !created.equals(serving.created) : serving.created != null) return false;
    if (creator != null ? !creator.equals(serving.creator) : serving.creator != null) return false;
    if (!name.equals(serving.name)) return false;
    if (description != null ? !description.equals(serving.description) : serving.description != null) return false;
    if (modelPath != null ? !modelPath.equals(serving.modelPath) : serving.modelPath != null) return false;
    if (!modelVersion.equals(serving.modelVersion)) return false;
    if (predictor != null ? !predictor.equals(serving.predictor) : serving.predictor != null) return false;
    if (modelFramework != null ? !modelFramework.equals(serving.modelFramework) : serving.modelFramework != null)
      return false;
    if (instances != null ? !instances.equals(serving.instances) : serving.instances != null) return false;
    if (project != null ? !project.equals(serving.project) : serving.project != null) return false;
    if (lockIP != null ? !lockIP.equals(serving.lockIP) : serving.lockIP != null) return false;
    if (lockTimestamp != null ? !lockTimestamp.equals(serving.lockTimestamp) : serving.lockTimestamp != null)
      return false;
    if (kafkaTopic != null ? !kafkaTopic.equals(serving.kafkaTopic) : serving.kafkaTopic != null) return false;
    if (localPort != null ? !localPort.equals(serving.localPort) : serving.localPort != null) return false;
    if (cid != null ? !cid.equals(serving.cid) : serving.cid != null) return false;
    if (modelServer != null ? !modelServer.equals(serving.modelServer) : serving.modelServer != null) return false;
    if (servingTool != null ? !servingTool.equals(serving.servingTool) : serving.servingTool != null) return false;
    if (deployed != null ? !deployed.equals(serving.deployed) : serving.deployed != null) return false;
    if (revision != null ? !revision.equals(serving.revision) : serving.revision != null) return false;
    if (batchingConfiguration != null ? !batchingConfiguration.equals(serving.batchingConfiguration) :
        serving.batchingConfiguration != null) return false;
    return localDir != null ? localDir.equals(serving.localDir) : serving.localDir == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (creator != null ? creator.hashCode() : 0);
    result = 31 * result + name.hashCode();
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + modelPath.hashCode();
    result = 31 * result + modelVersion.hashCode();
    result = 31 * result + (predictor != null ? predictor.hashCode() : 0);
    result = 31 * result + (modelFramework != null ? modelFramework.hashCode() : 0);
    result = 31 * result + (optimized ? 1 : 0);
    result = 31 * result + (instances != null ? instances.hashCode() : 0);
    result = 31 * result + (project != null ? project.hashCode() : 0);
    result = 31 * result + (lockIP != null ? lockIP.hashCode() : 0);
    result = 31 * result + (lockTimestamp != null ? lockTimestamp.hashCode() : 0);
    result = 31 * result + (kafkaTopic != null ? kafkaTopic.hashCode() : 0);
    result = 31 * result + (localPort != null ? localPort.hashCode() : 0);
    result = 31 * result + (cid != null ? cid.hashCode() : 0);
    result = 31 * result + (localDir != null ? localDir.hashCode() : 0);
    result = 31 * result + (modelServer != null ? modelServer.hashCode() : 0);
    result = 31 * result + (servingTool != null ? servingTool.hashCode() : 0);
    result = 31 * result + (deployed != null ? deployed.hashCode() : 0);
    result = 31 * result + (revision != null ? revision.hashCode() : 0);
    result = 31 * result + (batchingConfiguration != null ? batchingConfiguration.hashCode() : 0);
    return result;
  }
}
