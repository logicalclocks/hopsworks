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
package io.hops.hopsworks.persistence.entity.git;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandConfiguration;
import io.hops.hopsworks.persistence.entity.git.config.GitOpExecutionState;
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
import java.util.Date;

@Entity
@Table(name = "git_executions", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "GitOpExecution.findAll",
        query
            = "SELECT e FROM GitOpExecution e ORDER BY e.id DESC"),
    @NamedQuery(name = "GitOpExecution.findById",
        query
            = "SELECT e FROM GitOpExecution e WHERE e.id = :id"),
    @NamedQuery(name = "GitOpExecution.findBySubmissionTime",
        query
            = "SELECT e FROM GitOpExecution e WHERE e.submissionTime = :submissionTime"),
    @NamedQuery(name = "GitOpExecution.findAllInRepository",
        query
            = "SELECT e FROM GitOpExecution e WHERE e.repository = :repository ORDER BY e.id DESC"),
    @NamedQuery(name = "GitOpExecution.findRunningInRepository",
        query
            = "SELECT e FROM GitOpExecution e WHERE e.repository = :repository AND e.state NOT IN :finalStates " +
            "ORDER BY e.submissionTime DESC"),
    @NamedQuery(name = "GitOpExecution.findByIdAndRepository",
        query
            = "SELECT e FROM GitOpExecution e WHERE e.repository = :repository AND e.id = :id")})
public class GitOpExecution implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "submission_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date submissionTime;

  @Column(name = "execution_start")
  private long executionStart;

  @Column(name = "execution_stop")
  private long executionStop;

  @JoinColumn(name = "user",
      referencedColumnName = "uid")
  @ManyToOne(optional = false)
  @NotNull
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private Users user;

  @Column(name = "command_config")
  @Convert(converter = GitCommandConfigurationConverter.class)
  private GitCommandConfiguration gitCommandConfiguration;

  @Basic(optional = false)
  @NotNull
  @Column(name = "state")
  @Enumerated(EnumType.STRING)
  private GitOpExecutionState state;

  @JoinColumn(name = "repository",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  @NotNull
  private GitRepository repository;

  @Size(max = 11000)
  @Column(name = "final_result_message")
  private String commandResultMessage;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "config_secret")
  private String configSecret;

  public GitOpExecution(){}

  public GitOpExecution(GitCommandConfiguration gitCommandConfiguration, Date submissionTime, Users user,
                        GitRepository repository, GitOpExecutionState state, String configSecret) {
    this.submissionTime = submissionTime;
    this.user = user;
    this.gitCommandConfiguration = gitCommandConfiguration;
    this.state = state;
    this.repository = repository;
    this.configSecret = configSecret;
  }

  public Integer getId() { return id; }

  public void setId(Integer id) { this.id = id; }

  public Date getSubmissionTime() { return submissionTime; }

  public void setSubmissionTime(Date submissionTime) { this.submissionTime = submissionTime; }

  public long getExecutionStart() { return executionStart; }

  public void setExecutionStart(long executionStart) { this.executionStart = executionStart; }

  public long getExecutionStop() { return executionStop; }

  public void setExecutionStop(long executionStop) { this.executionStop = executionStop; }

  public Users getUser() { return user; }

  public void setUser(Users user) { this.user = user; }

  public GitCommandConfiguration getGitCommandConfiguration() { return gitCommandConfiguration; }

  public void setGitCommandConfiguration(GitCommandConfiguration gitCommandConfiguration) {
    this.gitCommandConfiguration = gitCommandConfiguration;
  }

  public GitRepository getRepository() { return repository; }

  public void setRepository(GitRepository repository) { this.repository = repository; }

  public String getConfigSecret() { return configSecret; }

  public void setConfigSecret(String configSecret) { this.configSecret = configSecret; }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // won't work in the case the id fields are not set
    if (!(object instanceof GitOpExecution)) {
      return false;
    }
    GitOpExecution other = (GitOpExecution) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
        equals(other.id))) {
      return false;
    }
    return true;
  }

  public GitOpExecutionState getState() { return state; }

  public void setState(GitOpExecutionState state) { this.state = state; }

  public String getCommandResultMessage() { return commandResultMessage; }

  public void setCommandResultMessage(String failMessage) { this.commandResultMessage = failMessage; }
}
