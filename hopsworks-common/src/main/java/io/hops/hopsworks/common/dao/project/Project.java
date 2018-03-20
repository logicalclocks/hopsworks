/*
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
 *
 */
package io.hops.hopsworks.common.dao.project;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
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

import io.hops.hopsworks.common.dao.tfserving.TfServing;
import io.hops.hopsworks.common.util.Settings;
import org.codehaus.jackson.annotate.JsonIgnore;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDep;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

@Entity
@Table(name = "hopsworks.project")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Project.findAll",
      query = "SELECT t FROM Project t")
  ,
  @NamedQuery(name = "Project.findByName",
      query = "SELECT t FROM Project t WHERE t.name = :name")
  ,
  @NamedQuery(name = "Project.findByOwner",
      query = "SELECT t FROM Project t WHERE t.owner = :owner")
  ,
  @NamedQuery(name = "Project.findByCreated",
      query = "SELECT t FROM Project t WHERE t.created = :created")
  ,
  @NamedQuery(name = "Project.findByRetentionPeriod",
      query
      = "SELECT t FROM Project t WHERE t.retentionPeriod = :retentionPeriod")
  ,
  @NamedQuery(name = "Project.countProjectByOwner",
      query
      = "SELECT count(t) FROM Project t WHERE t.owner = :owner")
  ,
  @NamedQuery(name = "Project.findByOwnerAndName",
      query
      = "SELECT t FROM Project t WHERE t.owner = :owner AND t.name = :name")
  ,
  @NamedQuery(name = "Project.findByInodeId",
      query
      = "SELECT t FROM Project t WHERE t.inode.inodePK.parentId = :parentid "
      + "AND t.inode.inodePK.name = :name")})
public class Project implements Serializable {

  @Column(name = "conda")
  private Boolean conda = false;
  @Column(name = "archived")
  private Boolean archived = false;
  @Column(name = "logs")
  private Boolean logs = false;
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
      mappedBy = "project")
  private Collection<Dataset> datasetCollection;

  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "projectId")
  private Collection<CondaCommands> condaCommandsCollection;
  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "project")
  private Collection<JupyterSettings> jupyterSettingsCollection;
  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "project")
  private Collection<TfServing> tfServingCollection;
  
//  @OneToMany(cascade = CascadeType.ALL,
//      mappedBy = "projectId")
//  private Collection<Pia> piaCollection;

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 88)
  @Column(name = "projectname")
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "username",
      referencedColumnName = "email")
  private Users owner;

  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Column(name = "retention_period")
  @Temporal(TemporalType.DATE)
  private Date retentionPeriod;

  @Column(name = "deleted")
  private Boolean deleted;

  @NotNull
  @Column(name = "payment_type")
  @Enumerated(EnumType.STRING)
  private PaymentType paymentType;

  @Column(name = "python_version")
  private String pythonVersion;

  @Size(max = 2000)
  @Column(name = "description")
  private String description;

  @Basic(optional = false)
  @NotNull
  @Column(name = "last_quota_update")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastQuotaUpdate;

  @JoinColumns({
    @JoinColumn(name = "inode_pid",
        referencedColumnName = "parent_id")
    ,
    @JoinColumn(name = "inode_name",
        referencedColumnName = "name")
    ,
    @JoinColumn(name = "partition_id",
        referencedColumnName = "partition_id")})
  @OneToOne(optional = false)
  private Inode inode;

  @JoinTable(name = "hopsworks.project_pythondeps",
      joinColumns
      = {
        @JoinColumn(name = "project_id",
            referencedColumnName = "id")},
      inverseJoinColumns
      = {
        @JoinColumn(name = "dep_id",
            referencedColumnName = "id")})
  @ManyToMany
  private Collection<PythonDep> pythonDepCollection;

  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "projectId")
  private Collection<JupyterProject> jupyterProjectCollection;

  public Project() {
  }

  public Project(String name) {
    this.name = name;
    this.archived = false;
  }

  public Project(String name, Inode inode) {
    this(name);
    this.inode = inode;
  }

  public Project(String name, Users owner, Date timestamp, PaymentType paymentType) {
    this.name = name;
    this.owner = owner;
    this.created = timestamp;
    this.archived = false;
    this.paymentType = paymentType;
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

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public Inode getInode() {
    return this.inode;
  }

  public Users getOwner() {
    return owner;
  }

  public void setOwner(Users owner) {
    this.owner = owner;
  }

  public Date getRetentionPeriod() {
    return retentionPeriod;
  }

  public void setRetentionPeriod(Date retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }

  public String getPythonVersion() {
    return pythonVersion;
  }

  public void setPythonVersion(String pythonVersion) {
    this.pythonVersion = pythonVersion;
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

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
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

  public Boolean getConda() {
    return conda;
  }

  public void setConda(Boolean conda) {
    this.conda = conda;
  }

  public Boolean getArchived() {
    return archived;
  }

  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  public Boolean getLogs() {
    return logs;
  }

  public void setLogs(Boolean logs) {
    this.logs = logs;
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
  public Collection<PythonDep> getPythonDepCollection() {
    return pythonDepCollection;
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
  public Collection<JupyterSettings> getJupyterSettingsCollection() {
    return jupyterSettingsCollection;
  }

  public void setJupyterSettingsCollection(
      Collection<JupyterSettings> jupyterSettingsCollection) {
    this.jupyterSettingsCollection = jupyterSettingsCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TfServing> getTfServingCollection() {
    return tfServingCollection;
  }

  public void setTfServingCollection(Collection<TfServing> tfServingCollection) {
    this.tfServingCollection = tfServingCollection;
  }

  public String getProjectGenericUser() {
    return name + Settings.PROJECT_GENERIC_USER_SUFFIX;
  }

  public Date getLastQuotaUpdate() {
    return lastQuotaUpdate;
  }

  public void setLastQuotaUpdate(Date lastQuotaUpdate) {
    this.lastQuotaUpdate = lastQuotaUpdate;
  }

//  public Collection<Pia> getPiaCollection() {
//    return piaCollection;
//  }
//
//  public void setPiaCollection(Collection<Pia> piaCollection) {
//    this.piaCollection = piaCollection;
//  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.Project[ name=" + this.name + ", id=" + this.id
        + ", parentId=" + this.inode.getInodePK().getParentId() + " ]";
  }
}
