/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.hops.hopsworks.common.dao.jupyter;

import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
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

@Entity
@Table(name = "jupyter_project",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JupyterProject.findAll",
          query
          = "SELECT j FROM JupyterProject j"),
  @NamedQuery(name = "JupyterProject.findByPort",
          query
          = "SELECT j FROM JupyterProject j WHERE j.port = :port"),
  @NamedQuery(name = "JupyterProject.findByHdfsUserId",
          query
          = "SELECT j FROM JupyterProject j WHERE j.hdfsUserId = :hdfsUserId"),
  @NamedQuery(name = "JupyterProject.findByCreated",
          query
          = "SELECT j FROM JupyterProject j WHERE j.created = :created"),
  @NamedQuery(name = "JupyterProject.findByHostIp",
          query
          = "SELECT j FROM JupyterProject j WHERE j.hostIp = :hostIp"),
  @NamedQuery(name = "JupyterProject.findByHashedPasswd",
          query
          = "SELECT j FROM JupyterProject j WHERE j.hashedPasswd = :hashedPasswd"),
  @NamedQuery(name = "JupyterProject.findByPid",
          query
          = "SELECT j FROM JupyterProject j WHERE j.pid = :pid")})
public class JupyterProject implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "port")
  private Integer port;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "host_ip")
  private String hostIp;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "hashed_passwd")
  private String hashedPasswd;
  @Basic(optional = false)
  @NotNull
  @Column(name = "pid")
  private Long pid;
  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project projectId;
//  @JoinColumn(name = "hdfs_user_id",
//          referencedColumnName = "id")
//  @ManyToOne(optional = false)
//  private HdfsUsers user;
  @Basic(optional = false)
  @NotNull
  @Column(name = "hdfs_user_id")
  private int hdfsUserId;

  public JupyterProject() {
  }

  public JupyterProject(Integer port) {
    this.port = port;
  }

  public JupyterProject(Integer port, int hdfsUserId, Date created,
          String hostIp, String hashedPasswd, Long pid) {
    this.port = port;
    this.hdfsUserId = hdfsUserId;
    this.created = created;
    this.hostIp = hostIp;
    this.hashedPasswd = hashedPasswd;
    this.pid = pid;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public int getHdfsUserId() {
    return hdfsUserId;
  }

  public void setHdfsUserId(int hdfsUserId) {
    this.hdfsUserId = hdfsUserId;
  }
  

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public String getHashedPasswd() {
    return hashedPasswd;
  }

  public void setHashedPasswd(String hashedPasswd) {
    this.hashedPasswd = hashedPasswd;
  }

  public Long getPid() {
    return pid;
  }

  public void setPid(Long pid) {
    this.pid = pid;
  }

  public Project getProjectId() {
    return projectId;
  }

  public void setProjectId(Project projectId) {
    this.projectId = projectId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (port != null ? port.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JupyterProject)) {
      return false;
    }
    JupyterProject other = (JupyterProject) object;
    if ((this.port == null && other.port != null) ||
            (this.port != null && !this.port.equals(other.port))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.JupyterProject[ port=" + port +
            " ]";
  }

}
