/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.hops.bbc;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.fb.Inode;


@Entity
@Table(name = "hopsworks.consents", 
    uniqueConstraints = {
		@UniqueConstraint(columnNames = "inode_pid, inode_name") 
    }
)
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Consents.findAll", query = "SELECT c FROM Consents c"),
  @NamedQuery(name
      = "Consents.findById", query = "SELECT c FROM Consents c WHERE c.id = :id"),
  @NamedQuery(name
      = "Consents.findByFormType", query = "SELECT c FROM Consents c WHERE c.consentType = :consentType"),
  @NamedQuery(name
      = "Consents.findByStatus", query = "SELECT c FROM Consents c WHERE c.consentStatus = :consentStatus"),
  @NamedQuery(name
      = "Consents.findByProjectId", query = "SELECT c FROM Consents c WHERE c.project.id = :id"),
  @NamedQuery(name
      = "Consents.findByInodeId", query = "SELECT c FROM Consents c WHERE c.inode.id = :id"),
  @NamedQuery(name
      = "Consents.findByInodePK", query = "SELECT c FROM Consents c WHERE c.inode.inodePK.parentId "
          + "= :parentId AND c.inode.inodePK.name = :name ")})
public class Consents implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 127)
  @Column(name = "form_type")
  @Enumerated(EnumType.STRING)
  private ConsentType consentType;
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 127)
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ConsentStatus consentStatus;

  @Basic(optional = false)
  @Column(name = "last_modified")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModified;
  
  @JoinColumns({
    @JoinColumn(name = "inode_pid", referencedColumnName = "parent_id"),
    @JoinColumn(name = "inode_name", referencedColumnName = "name")})
  @ManyToOne(optional = false)
  private Inode inode;
  
  @JoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  public Consents() {
  }

  public Consents(Inode inode, Project project) {
    this.inode = inode;
    this.project = project;
    this.consentStatus = ConsentStatus.UNDEFINED;
    this.consentType = ConsentType.UNDEFINED;
  }
  
  public Consents(ConsentType formType, ConsentStatus status, Inode inode, Project project) {
    this.consentType = formType;
    this.consentStatus = status;
    this.inode = inode;
    this.project = project;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public ConsentType getConsentType() {
    return consentType;
  }

  public void setConsentType(ConsentType formType) {
    this.consentType = formType;
  }

  public ConsentStatus getConsentStatus() {
    return consentStatus;
  }

  public void setConsentStatus(ConsentStatus status) {
    this.consentStatus = status;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public Inode getInode() {
    return inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Consents)) {
      return false;
    }
    Consents other = (Consents) object;
    if ((this.inode == null && other.inode != null) || (this.inode != null && !this.inode.equals(other.inode))) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return "io.hops.bbc.Consents[ id=" + id + ", consentType: " + consentType + ", consentStatus: " +
        consentStatus + " ]";
  }

}
