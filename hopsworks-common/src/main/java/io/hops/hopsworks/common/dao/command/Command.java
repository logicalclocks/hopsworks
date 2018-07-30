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

package io.hops.hopsworks.common.dao.command;

import io.hops.hopsworks.common.util.FormatUtils;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;

@Entity
@Table(name = "hopsworks.commands")
@NamedQueries({
  @NamedQuery(name = "Command.find",
          query = "SELECT c FROM Command c"),
  @NamedQuery(name = "Command.findRecentByCluster",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND (NOT c.status "
          + "= :status) ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRunningByCluster",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.status = :status "
          + "ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRecentByCluster-Service",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service "
          + "AND (NOT c.status = :status) ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRunningByCluster-Service",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service "
          + "AND c.status = :status ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRecentByCluster-Service-Role-HostId",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service "
          + "AND c.role = :role AND c.hostId = :hostId AND (NOT c.status = :status) "
          + "ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRunningByCluster-Service-Role-HostId",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service "
          + "AND c.role = :role AND c.hostId = :hostId AND c.status = :status ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findByCluster-Service-Role-HostId",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service "
          + "AND c.role = :role AND c.hostId = :hostId ORDER BY c.startTime DESC"),})
public class Command implements Serializable {

  public static enum CommandStatus {

    Running,
    Succeeded,
    Failed
  }
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "id")
  private Long id;
  @Column(name = "command",
          nullable = false,
          length = 256)
  private String command;
  @Column(name = "host_id",
          nullable = false,
          length = 128)
  private String hostId;
  @Column(name = "service",
          nullable = false,
          length = 48)
  private String service;
  @Column(name = "role",
          nullable = false,
          length = 48)
  private String role;
  @Column(name = "cluster",
          nullable = false,
          length = 48)
  private String cluster;
  @Column(name = "start_time")
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  private Date startTime;
  @Column(name = "end_time")
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  private Date endTime;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private CommandStatus status;

  public Command() {
  }

  public Command(String command, String hostId, String service, String role,
          String cluster) {
    this.command = command;
    this.hostId = hostId;
    this.service = service;
    this.role = role;
    this.cluster = cluster;

    this.startTime = new Date();
    this.status = CommandStatus.Running;
  }

  public Long getId() {
    return id;
  }

  public String getCommand() {
    return command;
  }

  public String getHostId() {
    return hostId;
  }

  public String getService() {
    return service;
  }

  public String getRole() {
    return role;
  }

  public String getCluster() {
    return cluster;
  }

  public Date getStartTime() {
    return startTime;
  }

  public String getStartTimeShort() {
    return FormatUtils.date(startTime);
  }

  public Date getEndTime() {
    return endTime;
  }

  public String getEndTimeShort() {
    return FormatUtils.date(endTime);
  }

  public CommandStatus getStatus() {
    return status;
  }

  public void succeeded() {

    this.endTime = new Date();
    this.status = CommandStatus.Succeeded;

  }

  public void failed() {
    this.endTime = new Date();
    this.status = CommandStatus.Failed;
  }

  public String getCommandInfo() {
    return command + " " + cluster + "/" + service + "/" + role + " @ " + hostId;
  }

  public int getProgress() {
    if (status == Command.CommandStatus.Running) {
      return 50;
    }
    return 100;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setService(String service) {
    this.service = service;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public void setStatus(CommandStatus status) {
    this.status = status;
  }

}
