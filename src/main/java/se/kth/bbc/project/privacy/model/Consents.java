/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project.privacy.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "hopsworks.consents")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Consents.findAll", query = "SELECT c FROM Consents c"),
  @NamedQuery(name = "Consents.findById", query = "SELECT c FROM Consents c WHERE c.id = :id"),
  @NamedQuery(name = "Consents.findByInodePid", query = "SELECT c FROM Consents c WHERE c.inodePid = :inodePid"),
  @NamedQuery(name = "Consents.findByInodeName", query = "SELECT c FROM Consents c WHERE c.inodeName = :inodeName"),
  @NamedQuery(name = "Consents.findByFormType", query = "SELECT c FROM Consents c WHERE c.formType = :formType"),
  @NamedQuery(name = "Consents.findByStatus", query = "SELECT c FROM Consents c WHERE c.status = :status"),
  @NamedQuery(name = "Consents.findByLastModified", query = "SELECT c FROM Consents c WHERE c.lastModified = :lastModified")})
public class Consents implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_pid")
  private int inodePid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "inode_name")
  private String inodeName;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 17)
  @Column(name = "form_type")
  private String formType;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 9)
  @Column(name = "status")
  private String status;
  @Basic(optional = false)
  @NotNull
  @Column(name = "last_modified")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModified;

  public Consents() {
  }

  public Consents(Integer id) {
    this.id = id;
  }

  public Consents(Integer id, int inodePid, String inodeName, String formType, String status, Date lastModified) {
    this.id = id;
    this.inodePid = inodePid;
    this.inodeName = inodeName;
    this.formType = formType;
    this.status = status;
    this.lastModified = lastModified;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getInodePid() {
    return inodePid;
  }

  public void setInodePid(int inodePid) {
    this.inodePid = inodePid;
  }

  public String getInodeName() {
    return inodeName;
  }

  public void setInodeName(String inodeName) {
    this.inodeName = inodeName;
  }

  public String getFormType() {
    return formType;
  }

  public void setFormType(String formType) {
    this.formType = formType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
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
    if (!(object instanceof Consents)) {
      return false;
    }
    Consents other = (Consents) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.Consents[ id=" + id + " ]";
  }
  
}
