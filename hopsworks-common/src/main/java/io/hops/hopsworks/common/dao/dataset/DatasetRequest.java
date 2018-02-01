/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.dataset;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.message.Message;

@Entity
@Table(name = "hopsworks.dataset_request")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "DatasetRequest.findAll",
          query
          = "SELECT d FROM DatasetRequest d"),
  @NamedQuery(name = "DatasetRequest.findById",
          query
          = "SELECT d FROM DatasetRequest d WHERE d.id = :id"),
  @NamedQuery(name = "DatasetRequest.findByDataset",
          query
          = "SELECT d FROM DatasetRequest d WHERE d.dataset = :dataset"),
  @NamedQuery(name = "DatasetRequest.findByProjectTeam",
          query
          = "SELECT d FROM DatasetRequest d WHERE d.projectTeam = :projectTeam"),
  @NamedQuery(name = "DatasetRequest.findByProjectTeamAndDataset",
          query
          = "SELECT d FROM DatasetRequest d WHERE d.projectTeam = :projectTeam AND d.dataset = :dataset"),
  @NamedQuery(name = "DatasetRequest.findByProjectAndDataset",
          query
          = "SELECT d FROM DatasetRequest d WHERE d.projectTeam.project = :project AND d.dataset = :dataset"),
  @NamedQuery(name = "DatasetRequest.findByRequested",
          query
          = "SELECT d FROM DatasetRequest d WHERE d.requested = :requested"),
  @NamedQuery(name = "DatasetRequest.findByMessage",
          query
          = "SELECT d FROM DatasetRequest d WHERE d.messageContent = :message"),
  @NamedQuery(name = "DatasetRequest.findByMessageId",
          query
          = "SELECT d FROM DatasetRequest d WHERE d.message.id = :message_id")})
public class DatasetRequest implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "requested")
  @Temporal(TemporalType.TIMESTAMP)
  private Date requested;
  @Size(max = 2000)
  @Column(name = "message")
  private String messageContent;
  @JoinColumn(name = "dataset",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Dataset dataset;
  @JoinColumns({
    @JoinColumn(name = "projectId",
            referencedColumnName
            = "project_id"),
    @JoinColumn(name = "user_email",
            referencedColumnName = "team_member")})
  @ManyToOne(optional = false)
  private ProjectTeam projectTeam;

  @JoinColumn(name = "message_id",
          referencedColumnName = "id")
  @OneToOne
  private Message message;

  public DatasetRequest() {
  }

  public DatasetRequest(Integer id) {
    this.id = id;
  }

  public DatasetRequest(Integer id, Date requested) {
    this.id = id;
    this.requested = requested;
  }

  public DatasetRequest(Dataset dataset, ProjectTeam projectTeam,
          String messageContent) {
    this.messageContent = messageContent;
    this.dataset = dataset;
    this.projectTeam = projectTeam;
  }

  public DatasetRequest(Dataset dataset, ProjectTeam projectTeam,
          String messageContent, Message message) {
    this.messageContent = messageContent;
    this.message = message;
    this.dataset = dataset;
    this.projectTeam = projectTeam;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getRequested() {
    return requested;
  }

  public void setRequested(Date requested) {
    this.requested = requested;
  }

  public String getMessageContent() {
    return messageContent;
  }

  public void setMessageContent(String message) {
    this.messageContent = message;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public Dataset getDataset() {
    return dataset;
  }

  public void setDataset(Dataset dataset) {
    this.dataset = dataset;
  }

  public ProjectTeam getProjectTeam() {
    return projectTeam;
  }

  public void setProjectTeam(ProjectTeam projectTeam) {
    this.projectTeam = projectTeam;
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
    if (!(object instanceof DatasetRequest)) {
      return false;
    }
    DatasetRequest other = (DatasetRequest) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.dataset.DatasetRequest[ id=" + id + " ]";
  }

}
