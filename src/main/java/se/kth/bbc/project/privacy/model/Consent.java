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
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "vangelis_kthfs.consent")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Consent.findAll",
          query = "SELECT c FROM Consent c"),
  @NamedQuery(name = "Consent.findById",
          query = "SELECT c FROM Consent c WHERE c.id = :id"),
  @NamedQuery(name = "Consent.findByDate",
          query = "SELECT c FROM Consent c WHERE c.date = :date"),
  @NamedQuery(name = "Consent.findByStatus",
          query = "SELECT c FROM Consent c WHERE c.status = :status"),
  @NamedQuery(name = "Consent.findByName",
          query = "SELECT c FROM Consent c WHERE c.name = :name"),
  @NamedQuery(name = "Consent.findByProject",
          query = "SELECT c FROM Consent c WHERE c.project = :project"),
  @NamedQuery(name = "Consent.findByType",
          query = "SELECT c FROM Consent c WHERE c.type = :type")})
public class Consent implements Serializable {

  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Long id;
  @Column(name = "date")
  @Temporal(TemporalType.DATE)
  private Date date;
  @Lob
  @Column(name = "consent_form")
  private byte[] consentForm;
  @Size(max = 30)
  @Column(name = "status")
  private String status;
  @Size(max = 80)
  @Column(name = "name")
  private String name;
  @Size(max = 20)
  @Column(name = "type")
  private String type;

  public Consent() {
  }

  public Consent(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public byte[] getConsentForm() {
    return consentForm;
  }

  public void setConsentForm(byte[] consentForm) {
    this.consentForm = consentForm;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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
    if (!(object instanceof Consent)) {
      return false;
    }
    Consent other = (Consent) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.privacy.model.Consent[ id=" + id + " ]";
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }
}
