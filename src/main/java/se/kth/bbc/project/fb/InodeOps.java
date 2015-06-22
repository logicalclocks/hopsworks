/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project.fb;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author vangelis
 */
@Entity
@Table(name = "meta_inodes_ops")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "InodeOps.findAll",
          query = "SELECT i FROM InodeOps i"),
  @NamedQuery(name = "InodeOps.findByInodeid",
          query = "SELECT i FROM InodeOps i WHERE i.inodeid = :inodeid"),
  @NamedQuery(name = "InodeOps.findByInodePid",
          query = "SELECT i FROM InodeOps i WHERE i.inodePid = :inodePid"),
  @NamedQuery(name = "InodeOps.findByInodeRoot",
          query = "SELECT i FROM InodeOps i WHERE i.inodeRoot = :inodeRoot"),
  @NamedQuery(name = "InodeOps.findByModified",
          query = "SELECT i FROM InodeOps i WHERE i.modified = :modified")})
public class InodeOps implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "inodeid")
  private Integer inodeid;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_pid")
  private int inodePid;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_root")
  private int inodeRoot;

  @Basic(optional = false)
  @NotNull
  @Column(name = "modified")
  @Temporal(TemporalType.TIMESTAMP)
  private Date modified;

  @Basic(optional = false)
  @NotNull
  @Column(name = "operationn")
  private int operationn;

  @Basic(optional = false)
  @NotNull
  @Column(name = "processed")
  private int processed;

  public InodeOps() {
  }

  public InodeOps(Integer inodeid) {
    this.inodeid = inodeid;
  }

  public InodeOps(Integer inodeid, int inodePid, int inodeRoot,
          Date modified, int operationn, int processed) {
    this.inodeid = inodeid;
    this.inodePid = inodePid;
    this.inodeRoot = inodeRoot;
    this.modified = modified;
    this.operationn = operationn;
    this.processed = processed;
  }

  public Integer getInodeid() {
    return inodeid;
  }

  public void setInodeid(Integer inodeid) {
    this.inodeid = inodeid;
  }

  public int getInodePid() {
    return inodePid;
  }

  public void setInodePid(int inodePid) {
    this.inodePid = inodePid;
  }

  public int getInodeRoot() {
    return inodeRoot;
  }

  public void setInodeRoot(int inodeRoot) {
    this.inodeRoot = inodeRoot;
  }

  public Date getModified() {
    return modified;
  }

  public void setModified(Date modified) {
    this.modified = modified;
  }

  public int getOperationn() {
    return this.operationn;
  }

  public void setOperation(int operationn) {
    this.operationn = operationn;
  }

  public int getProcessed() {
    return this.processed;
  }

  public void setProcessed(int processed) {
    this.processed = processed;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (inodeid != null ? inodeid.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof InodeOps)) {
      return false;
    }
    InodeOps other = (InodeOps) object;
    if ((this.inodeid == null && other.inodeid != null) || (this.inodeid != null
            && !this.inodeid.equals(other.inodeid))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {

    return "se.kth.bbc.project.fb.InodesOps[ inodeid=" + inodeid + ","
            + " inodepid=" + inodePid + ", inoderoot=" + inodeRoot
            + ", operation=" + operationn + ", processed=" + processed + ""
            + " modified=" + modified + " ]";
  }

}
