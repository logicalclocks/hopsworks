/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.persistence.entity.project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;
import io.hops.hopsworks.persistence.entity.project.jobs.DefaultJobConfiguration;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServices;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.persistence.entity.python.PythonEnvironment;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.tensorflow.TensorBoard;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.Activity;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "project", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Project.findAll", query = "SELECT t FROM Project t"),
  @NamedQuery(name = "Project.findAllOrderByCreated", query = "SELECT t FROM Project t ORDER BY t.created"),
  @NamedQuery(name = "Project.findById",
      query = "SELECT t FROM Project t WHERE t.id = :id"),
  @NamedQuery(name = "Project.findByName",
      query = "SELECT t FROM Project t WHERE t.name = :name"),
  @NamedQuery(name = "Project.findByOwner",
      query = "SELECT t FROM Project t WHERE t.owner = :owner"),
  @NamedQuery(name = "Project.findByCreated",
      query = "SELECT t FROM Project t WHERE t.created = :created"),
  @NamedQuery(name = "Project.countProjectByOwner",
      query = "SELECT count(t) FROM Project t WHERE t.owner = :owner"),
  @NamedQuery(name = "Project.findByOwnerAndName",
      query = "SELECT t FROM Project t WHERE t.owner = :owner AND t.name = :name"),
  @NamedQuery(name = "Project.findByNameCaseInsensitive",
      query = "SELECT t FROM Project t where LOWER(t.name) = LOWER(:name)")})
public class Project implements Serializable {

  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "project")
  private Collection<ProjectTeam> projectTeamCollection;
  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "project")
  private Collection<Activity> activityCollection;
  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "project")
  private Collection<ProjectServices> projectServicesCollection;
  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "project", fetch = FetchType.LAZY)
  private Collection<Dataset> datasetCollection;
  @OneToMany(cascade = CascadeType.ALL,
    mappedBy = "project", fetch = FetchType.LAZY)
  private Collection<DatasetSharedWith> datasetSharedWithCollection;
  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "projectId")
  private Collection<CondaCommands> condaCommandsCollection;
  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "project")
  private Collection<Serving> servingCollection;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "project")
  private Collection<TensorBoard> tensorBoardCollection;
  @OneToMany(cascade = CascadeType.ALL,
    mappedBy = "project",
    orphanRemoval=true)
  private Collection<DefaultJobConfiguration> defaultJobConfigurationCollection;
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY,
      mappedBy = "project")
  private Collection<Jobs> jobsCollection;

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 88)
  @Column(name = "projectname")
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "username",
      referencedColumnName = "email")
  private Users owner;

  @OneToOne(cascade = CascadeType.ALL,
      orphanRemoval = true)
  @JoinColumn(name = "python_env_id", referencedColumnName = "id")
  private PythonEnvironment pythonEnvironment;

  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @NotNull
  @Column(name = "payment_type")
  @Enumerated(EnumType.STRING)
  private PaymentType paymentType;

  @Size(max = 2000)
  @Column(name = "description")
  private String description;

  @Basic(optional = false)
  @NotNull
  @Column(name = "kafka_max_num_topics")
  private Integer kafkaMaxNumTopics = 10;

  @Basic(optional = false)
  @NotNull
  @Column(name = "last_quota_update")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastQuotaUpdate;

  @Size(max = 255)
  @Column(name = "docker_image")
  private String dockerImage;

  @Column(name = "topic_name")
  private String topicName;

  @Basic(optional = false)
  @Column(name = "online_feature_store_available")
  private Boolean onlineFeatureStoreAvailable;

  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "creation_status")
  private CreationStatus creationStatus;

  @JoinTable(name = "hopsworks.project_pythondeps",
      joinColumns
      = {
        @JoinColumn(name = "project_id",
            referencedColumnName = "id")},
      inverseJoinColumns
      = {
        @JoinColumn(name = "dep_id",
            referencedColumnName = "id")})
  @ManyToMany(fetch = FetchType.LAZY)
  private Collection<PythonDep> pythonDepCollection;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
  private Collection<JupyterProject> jupyterProjectCollection;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
  private Collection<ProjectServiceAlert> projectServiceAlerts;

  public Project() {
  }

  public Project(String name) {
    this.name = name;
  }

  public Project(String name, Users owner, Date timestamp, PaymentType paymentType) {
    this.name = name;
    this.owner = owner;
    this.created = timestamp;
    this.paymentType = paymentType;
    this.lastQuotaUpdate = timestamp;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Users getOwner() {
    return owner;
  }

  public void setOwner(Users owner) {
    this.owner = owner;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Project(Integer id) {
    this.id = id;
  }

  public Project(Integer id, String projectname) {
    this.id = id;
    this.name = projectname;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public PaymentType getPaymentType() {
    return paymentType;
  }

  public void setPaymentType(PaymentType paymentType) {
    this.paymentType = paymentType;
  }

  public String getPaymentTypeString() {
    return paymentType.name();
  }
  
  public CreationStatus getCreationStatus() {
    return creationStatus;
  }
  
  public void setCreationStatus(CreationStatus creationStatus) {
    this.creationStatus = creationStatus;
  }

  public Boolean getOnlineFeatureStoreAvailable() {
    return onlineFeatureStoreAvailable;
  }

  public void setOnlineFeatureStoreAvailable(Boolean onlineFeatureStoreAvailable) {
    this.onlineFeatureStoreAvailable = onlineFeatureStoreAvailable;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Project)) {
      return false;
    }
    Project other = (Project) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
        equals(other.id))) {
      return false;
    }
    return true;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<ProjectTeam> getProjectTeamCollection() {
    return projectTeamCollection;
  }

  public void setProjectTeamCollection(
      Collection<ProjectTeam> projectTeamCollection) {
    this.projectTeamCollection = projectTeamCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Activity> getActivityCollection() {
    return activityCollection;
  }

  public void setActivityCollection(Collection<Activity> activityCollection) {
    this.activityCollection = activityCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<ProjectServices> getProjectServicesCollection() {
    return projectServicesCollection;
  }

  public void setProjectServicesCollection(
      Collection<ProjectServices> projectServicesCollection) {
    this.projectServicesCollection = projectServicesCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Dataset> getDatasetCollection() {
    return datasetCollection;
  }

  public void setDatasetCollection(Collection<Dataset> datasetCollection) {
    this.datasetCollection = datasetCollection;
  }
  
  @XmlTransient
  @JsonIgnore
  public Collection<DatasetSharedWith> getDatasetSharedWithCollection() {
    return datasetSharedWithCollection;
  }
  
  public void setDatasetSharedWithCollection(
    Collection<DatasetSharedWith> datasetSharedWithCollection) {
    this.datasetSharedWithCollection = datasetSharedWithCollection;
  }
  
  @XmlTransient
  @JsonIgnore
  public Collection<PythonDep> getPythonDepCollection() {
    return pythonDepCollection;
  }
  
  @XmlTransient
  @JsonIgnore
  public Map<String, PythonDep> getPythonDepMap() {
    Map<String, PythonDep> map = new HashMap<>();
    for(PythonDep dep : pythonDepCollection){
      map.put(dep.getDependency(), dep);
    }
    return map;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<CondaCommands> getCondaCommandsCollection() {
    return condaCommandsCollection;
  }

  public void setCondaCommandsCollection(
      Collection<CondaCommands> condaCommandsCollection) {
    this.condaCommandsCollection = condaCommandsCollection;
  }

  public void setPythonDepCollection(Collection<PythonDep> pythonDepCollection) {
    this.pythonDepCollection = pythonDepCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<JupyterProject> getJupyterProjectCollection() {
    return jupyterProjectCollection;
  }

  public void setJupyterProjectCollection(
      Collection<JupyterProject> jupyterProjectCollection) {
    this.jupyterProjectCollection = jupyterProjectCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Serving> getServingCollection() {
    return servingCollection;
  }

  public void setServingCollection(Collection<Serving> servingCollection) {
    this.servingCollection = servingCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TensorBoard> getTensorBoardCollection() {
    return tensorBoardCollection;
  }

  public void setTensorBoardCollection(Collection<TensorBoard> tensorBoardCollection) {
    this.tensorBoardCollection = tensorBoardCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<DefaultJobConfiguration> getDefaultJobConfigurationCollection() {
    return defaultJobConfigurationCollection;
  }

  public void setDefaultJobConfigurationCollection(
    Collection<DefaultJobConfiguration> defaultJobConfigurationCollection) {
    this.defaultJobConfigurationCollection = defaultJobConfigurationCollection;
  }

  public Date getLastQuotaUpdate() {
    return lastQuotaUpdate;
  }

  public void setLastQuotaUpdate(Date lastQuotaUpdate) {
    this.lastQuotaUpdate = lastQuotaUpdate;
  }

  public Integer getKafkaMaxNumTopics() {
    return kafkaMaxNumTopics;
  }

  public void setKafkaMaxNumTopics(Integer kafkaMaxNumTopics) {
    this.kafkaMaxNumTopics = kafkaMaxNumTopics;
  }

  public String getDockerImage() {
    return dockerImage;
  }

  public void setDockerImage(String dockerImage) {
    this.dockerImage = dockerImage;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public PythonEnvironment getPythonEnvironment() {
    return this.pythonEnvironment;
  }

  public void setPythonEnvironment(PythonEnvironment pythonEnvironment) {
    this.pythonEnvironment = pythonEnvironment;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<ProjectServiceAlert> getProjectServiceAlerts() {
    return projectServiceAlerts;
  }

  public void setProjectServiceAlerts(Collection<ProjectServiceAlert> projectServiceAlerts) {
    this.projectServiceAlerts = projectServiceAlerts;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Jobs> getJobsCollection() {
    return jobsCollection;
  }

  public void setJobsCollection(Collection<Jobs> jobsCollection) {
    this.jobsCollection = jobsCollection;
  }

  @Override
  public String toString() {
    return "Project{" +
        "projectTeamCollection=" + projectTeamCollection +
        ", activityCollection=" + activityCollection +
        ", projectServicesCollection=" + projectServicesCollection +
        ", datasetCollection=" + datasetCollection +
        ", datasetSharedWithCollection=" + datasetSharedWithCollection +
        ", condaCommandsCollection=" + condaCommandsCollection +
        ", servingCollection=" + servingCollection +
        ", tensorBoardCollection=" + tensorBoardCollection +
        ", defaultJobConfigurationCollection=" + defaultJobConfigurationCollection +
        ", jobsCollection=" + jobsCollection +
        ", id=" + id +
        ", name='" + name + '\'' +
        ", owner=" + owner +
        ", pythonEnvironment=" + pythonEnvironment +
        ", created=" + created +
        ", paymentType=" + paymentType +
        ", description='" + description + '\'' +
        ", kafkaMaxNumTopics=" + kafkaMaxNumTopics +
        ", lastQuotaUpdate=" + lastQuotaUpdate +
        ", dockerImage='" + dockerImage + '\'' +
        ", topicName='" + topicName + '\'' +
        ", creationStatus=" + creationStatus +
        ", pythonDepCollection=" + pythonDepCollection +
        ", jupyterProjectCollection=" + jupyterProjectCollection +
        ", projectServiceAlerts=" + projectServiceAlerts +
        '}';
  }
}
