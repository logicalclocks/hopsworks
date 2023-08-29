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
package io.hops.hopsworks.persistence.entity.commands;

import io.hops.hopsworks.persistence.entity.project.Project;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;

@MappedSuperclass
public abstract class Command {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Long id;
  @JoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 20)
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private CommandStatus status;
  @Size(max = 10000)
  @Column(name = "error_message")
  private String errorMsg="";
  
  public Long getId() {
    return id;
  }
  
  public void setId(Long id) {
    this.id = id;
  }
  
  public void setProject(Project project) {
    this.project = project;
  }
  
  public Project getProject() {
    return project;
  }
  
  public CommandStatus getStatus() {
    return status;
  }
  
  public void setStatus(CommandStatus status) {
    this.status = status;
  }
  
  public String getErrorMsg() {
    return errorMsg;
  }
  
  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Command)) {
      return false;
    }
    Command command = (Command) o;
    return Objects.equals(id, command.id);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    String projectName = project != null ? project.getName() : "unknown";
    return "command id=" + id + ", project=" + projectName  +  ", status=" + status;
  }
  
  public void failWith(String errorMsg) {
    setStatus(CommandStatus.FAILED);
    setErrorMsg(errorMsg);
  }
}
