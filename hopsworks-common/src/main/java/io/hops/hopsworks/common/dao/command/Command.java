package io.hops.hopsworks.common.dao.command;

import io.hops.hopsworks.common.util.FormatUtils;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "hopsworks.commands")
@NamedQueries({
  @NamedQuery(name = "Command.find",
          query = "SELECT c FROM Command c"),
  @NamedQuery(name = "Command.findRecentByCluster",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND (NOT c.status = :status) ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRunningByCluster",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.status = :status ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRecentByCluster-Service",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service AND (NOT c.status = :status) ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRunningByCluster-Service",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service AND c.status = :status ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRecentByCluster-Service-Role-HostId",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service AND c.role = :role AND c.hostId = :hostId AND (NOT c.status = :status) ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findRunningByCluster-Service-Role-HostId",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service AND c.role = :role AND c.hostId = :hostId AND c.status = :status ORDER BY c.startTime DESC"),
  @NamedQuery(name = "Command.findByCluster-Service-Role-HostId",
          query
          = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.service = :service AND c.role = :role AND c.hostId = :hostId ORDER BY c.startTime DESC"),})
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
