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

package io.hops.hopsworks.common.dao.jobs.description;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.JsonReduceableConverter;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 * Description of work to be executed. If the work is executed, this
 * results in an Execution. Every type of Job needs to subclass this Entity and
 * declare the @DiscriminatorValue annotation.
 */
@Entity
@Table(name = "hopsworks.jobs")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Jobs.findAll",
          query = "SELECT j FROM Jobs j"),
  @NamedQuery(name = "Jobs.findById",
          query
          = "SELECT j FROM Jobs j WHERE j.id = :id"),
  @NamedQuery(name = "Jobs.findByName",
          query
          = "SELECT j FROM Jobs j WHERE j.name = :name"),
  @NamedQuery(name = "Jobs.findByCreationTime",
          query
          = "SELECT j FROM Jobs j WHERE j.creationTime = :creationTime"),
  @NamedQuery(name = "Jobs.findByProject",
          query
          = "SELECT j FROM Jobs j WHERE j.project = :project"),
  @NamedQuery(name = "Jobs.findByNameAndProject",
          query
          = "SELECT j FROM Jobs j WHERE j.name = :name AND j.project = :project"),
  @NamedQuery(name = "Jobs.updateConfig",
          query
          = "UPDATE Jobs j SET j.jobConfig = :jobconfig  WHERE j.id = :id"),
  @NamedQuery(name = "Jobs.findByProjectAndType",
          query
          = "SELECT j FROM Jobs j WHERE j.project = :project AND j.type = :type")})
public class Jobs implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Size(max = 128)
  @Column(name = "name")
  private String name;

  @Basic(optional = false)
  @NotNull
  @Column(name = "creation_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationTime;

  @Column(name = "json_config")
  @Convert(converter = JsonReduceableConverter.class)
  private JobConfiguration jobConfig;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private JobType type;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  @JoinColumn(name = "creator",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users creator;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "job")
  private Collection<Execution> executionCollection;

  protected Jobs() {
    this.name = "Hopsworks job";
  }

  public Jobs(JobConfiguration config, Project project,
          Users creator) {
    this(config, project, creator, new Date());
  }

  public Jobs(JobConfiguration config, Project project,
          Users creator, Date creationTime) {
    this(config, project, creator, null, creationTime);
  }

  public Jobs(JobConfiguration config, Project project,
          Users creator, String jobname) {
    this(config, project, creator, jobname, new Date());
  }

  protected Jobs(JobConfiguration config, Project project,
          Users creator, String jobname, Date creationTime) {
    if (Strings.isNullOrEmpty(jobname)) {
      this.name = "Hopsworks job";
    } else {
      this.name = jobname;
    }
    this.creationTime = creationTime;
    this.jobConfig = config;
    this.project = project;
    this.creator = creator;
    this.type = config.getType();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  /**
   * Set the name of the application. Default value: "Hopsworks job".
   * <p/>
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  public Date getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Date creationTime) {
    this.creationTime = creationTime;
  }

  public JobConfiguration getJobConfig() {
    return jobConfig;
  }

  public void setJobConfig(JobConfiguration jobConfig) {
    this.jobConfig = jobConfig;
  }

  @XmlElement
  public JobType getJobType() {
    return type;
  }

  @JsonIgnore
  @XmlTransient
  public Collection<Execution> getExecutionCollection() {
    return executionCollection;
  }

  public void setExecutionCollection(
          Collection<Execution> executionCollection) {
    this.executionCollection = executionCollection;
  }

  @Override
  public final int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public final boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Jobs)) {
      return false;
    }
    Jobs other = (Jobs) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return type.toString() + "Job [" + name + ", " + id + "]";
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
  }

}
