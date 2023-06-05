/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.hdfs.command;

import io.hops.hopsworks.persistence.entity.jobs.history.Execution;

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
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "hdfs_command_execution",
  catalog = "hopsworks",
  schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "HdfsCommandExecution.findAll",
    query = "SELECT c FROM HdfsCommandExecution c"),
  @NamedQuery(name = "HdfsCommandExecution.findById",
    query = "SELECT c FROM HdfsCommandExecution c WHERE c.id = :id"),
  @NamedQuery(name = "HdfsCommandExecution.findByExecution",
    query = "SELECT c FROM HdfsCommandExecution c WHERE c.execution = :execution"),
  @NamedQuery(name = "HdfsCommandExecution.findBySrcPath",
    query = "SELECT c FROM HdfsCommandExecution c WHERE c.srcPath = :srcPath")})
public class HdfsCommandExecution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "execution_id",
    referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Execution execution;
  @Column(name = "command",
    nullable = false,
    length = 45)
  @Basic(optional = false)
  @Enumerated(EnumType.STRING)
  private Command command;
  @Basic(optional = false)
  @NotNull
  @Column(name = "submitted")
  @Temporal(TemporalType.TIMESTAMP)
  private Date submitted;

  @Column(name = "src_path",
    nullable = false,
    length = 1000)
  private String srcPath;
  
  public HdfsCommandExecution() {
  }
  
  public HdfsCommandExecution(Execution execution, Command command, String srcPath) {
    this.execution = execution;
    this.command = command;
    this.submitted = new Date();
    this.srcPath = srcPath;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Execution getExecution() {
    return execution;
  }

  public void setExecution(Execution execution) {
    this.execution = execution;
  }

  public Command getCommand() {
    return command;
  }

  public void setCommand(Command command) {
    this.command = command;
  }

  public Date getSubmitted() {
    return submitted;
  }

  public void setSubmitted(Date submitted) {
    this.submitted = submitted;
  }
  
  public String getSrcPath() {
    return srcPath;
  }
  
  public void setSrcPath(String srcPath) {
    this.srcPath = srcPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HdfsCommandExecution that = (HdfsCommandExecution) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "HdfsCommandExecution{" +
      "id=" + id +
      ", execution=" + execution +
      ", command=" + command +
      ", submitted=" + submitted +
      ", srcPath=" + srcPath +
      '}';
  }
}
