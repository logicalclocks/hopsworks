package io.hops.hopsworks.common.dao.role;

import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.Status;
import java.io.Serializable;
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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "hopsworks.roles")
@NamedQueries({
  @NamedQuery(name = "Roles.findAll",
      query = "SELECT r from Roles r")
  ,
  @NamedQuery(name = "Roles.findClusters",
      query = "SELECT DISTINCT r.cluster FROM Roles r")
  ,
  @NamedQuery(name = "Roles.findServicesBy-Cluster",
      query
      = "SELECT DISTINCT r.service FROM Roles r WHERE r.cluster = :cluster")
  ,
  @NamedQuery(name = "Roles.find",
      query
      = "SELECT r FROM Roles r WHERE r.cluster = :cluster AND r.service = :service "
      + "AND r.role = :role AND r.host.hostname = :hostname")
  ,
  @NamedQuery(name = "Roles.findOnHost",
      query
      = "SELECT r FROM Roles r WHERE r.service = :service "
      + "AND r.role = :role AND r.host.hostname = :hostname")
  ,
  @NamedQuery(name = "Roles.findBy-HostId",
      query
      = "SELECT r FROM Roles r WHERE r.host.hostname = :hostname ORDER BY r.cluster, r.service, r.role")
  ,
  @NamedQuery(name = "Roles.findBy-Cluster-Service-Role",
      query
      = "SELECT r FROM Roles r WHERE r.cluster = :cluster AND r.service = :service "
      + "AND r.role = :role")
  ,
  @NamedQuery(name = "Roles.findBy-Service",
      query = "SELECT r FROM Roles r WHERE r.service = :service ")
  ,
  @NamedQuery(name = "Roles.findBy-Service-Role",
      query = "SELECT r FROM Roles r WHERE r.service = :service AND r.role = :role")
  ,
  @NamedQuery(name = "Roles.findBy-Role",
      query
      = "SELECT r FROM Roles r WHERE r.role = :role")
  ,
  @NamedQuery(name = "Roles.Count",
      query
      = "SELECT COUNT(r) FROM Roles r WHERE r.cluster = :cluster AND r.service = :service "
      + "AND r.role = :role")
  ,
  @NamedQuery(name = "Roles.Count-hosts",
      query
      = "SELECT count(DISTINCT r.host) FROM Roles r WHERE r.cluster = :cluster")
  ,
  @NamedQuery(name = "Roles.Count-roles",
      query
      = "SELECT COUNT(r) FROM Roles r WHERE r.cluster = :cluster AND r.service = :service")
  ,
  @NamedQuery(name = "Roles.findRoleHostBy-Cluster",
      query
      = "SELECT NEW io.hops.hopsworks.common.dao.role.RoleHostInfo(r, h) FROM Roles r, Hosts h "
      + "WHERE r.host = h AND r.cluster = :cluster")
  ,
  @NamedQuery(name = "Roles.findRoleHostBy-Cluster-Service",
      query
      = "SELECT NEW io.hops.hopsworks.common.dao.role.RoleHostInfo(r, h) FROM Roles r, Hosts h "
      + "WHERE r.host.hostname = h.hostname AND r.cluster = :cluster AND r.service = :service")
  ,
  @NamedQuery(name = "Roles.findRoleHostBy-Cluster-Service-Role",
      query
      = "SELECT NEW io.hops.hopsworks.common.dao.role.RoleHostInfo(r, h) FROM Roles r, Hosts h "
      + "WHERE r.host = h AND r.cluster = :cluster AND r.service = :service "
      + "AND r.role = :role")
  ,
  @NamedQuery(name = "Roles.findRoleHostBy-Cluster-Service-Role-Host",
      query
      = "SELECT NEW io.hops.hopsworks.common.dao.role.RoleHostInfo(r, h) FROM Roles r, Hosts h "
      + "WHERE r.host = h AND r.cluster = :cluster AND r.service = :service "
      + "AND r.role = :role AND r.host.hostname = :hostname")
  ,
  @NamedQuery(name = "Roles.DeleteBy-HostId",
      query = "DELETE FROM Roles r WHERE r.host.hostname = :hostname")
  ,
  @NamedQuery(name = "RoleHost.find.ClusterBy-Ip.WebPort",
      query
      = "SELECT r.cluster FROM Hosts h, Roles r WHERE h = r.host AND "
      + "(h.privateIp = :ip OR h.publicIp = :ip)")
  ,
  //TODO fix this: Hotname may be wrong. mysql nodes change hostname. May use hostid ?    
  @NamedQuery(name = "RoleHost.find.PrivateIpBy-Cluster.Hostname.WebPort",
      query
      = "SELECT h.privateIp FROM Hosts h, Roles r WHERE h = r.host AND r.cluster = :cluster "
      + "AND (h.hostname = :hostname OR h.hostIp = :hostname)")
  ,
  @NamedQuery(name = "RoleHost.TotalCores",
      query
      = "SELECT SUM(h2.cores) FROM Hosts h2 WHERE h2.hostname IN (SELECT h.hostname FROM Roles r, Hosts h "
      + "WHERE r.host = h AND r.cluster = :cluster GROUP BY h.hostname)")
  ,
  @NamedQuery(name = "RoleHost.TotalMemoryCapacity",
      query
      = "SELECT SUM(h2.memoryCapacity) FROM Hosts h2 WHERE h2.hostname IN "
      + "(SELECT h.hostname FROM Roles r, Hosts h WHERE r.host = h AND r.cluster "
      + "= :cluster GROUP BY h.hostname)")
  ,
  @NamedQuery(name = "RoleHost.TotalDiskCapacity",
      query
      = "SELECT SUM(h2.diskCapacity) FROM Hosts h2 WHERE h2.hostname IN (SELECT h.hostname FROM Roles r, Hosts h "
      + "WHERE r.host = h AND r.cluster = :cluster GROUP BY h.hostname)"),})
public class Roles implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Long id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 48)
  @Column(name = "cluster")
  private String cluster;
  @Column(name = "pid")
  private Integer pid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 48)
  @Column(name = "role_")
  private String role;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 48)
  @Column(name = "service")
  private String service;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "status")
  private Status status;
  @Column(name = "uptime")
  private long uptime;
  @Column(name = "webport")
  private Integer webport;
  @Column(name = "startTime")
  private long startTime;
  @Column(name = "stopTime")
  private long stopTime;
  @JoinColumn(name = "host_id",
      referencedColumnName = "id")
  @ManyToOne
  private Hosts host;

  public Roles() {
  }

  public Roles(Long id) {
    this.id = id;
  }

  public Roles(Long id, String cluster, String role, String service, Status status, Hosts host) {
    this.id = id;
    this.cluster = cluster;
    this.role = role;
    this.service = service;
    this.status = status;
    this.host = host;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public Integer getPid() {
    return pid;
  }

  public void setPid(Integer pid) {
    this.pid = pid;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public long getUptime() {
    return uptime;
  }

  public void setUptime(long uptime) {
    this.uptime = uptime;
  }

  public Integer getWebport() {
    return webport;
  }

  public void setWebport(Integer webport) {
    this.webport = webport;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getStopTime() {
    return stopTime;
  }

  public void setStopTime(long stopTime) {
    this.stopTime = stopTime;
  }

  public Hosts getHost() {
    return host;
  }

  public void setHost(Hosts host) {
    this.host = host;
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
    if (!(object instanceof Roles)) {
      return false;
    }
    Roles other = (Roles) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.role.Roles[ id=" + id + " ]";
  }

  public Health getHealth() {
    if (status == Status.Failed || status == Status.Stopped) {
      return Health.Bad;
    }
    return Health.Good;
  }
}
