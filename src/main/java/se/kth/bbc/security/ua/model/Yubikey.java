/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.hopsworks.user.model.Users;
 
@Entity
@Table(name = "hopsworks.yubikey")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Yubikey.findAll",
          query = "SELECT y FROM Yubikey y"),
  @NamedQuery(name = "Yubikey.findByNotes",
          query = "SELECT y FROM Yubikey y WHERE y.notes = :notes"),
  @NamedQuery(name = "Yubikey.findByCounter",
          query = "SELECT y FROM Yubikey y WHERE y.counter = :counter"),
  @NamedQuery(name = "Yubikey.findByLow",
          query = "SELECT y FROM Yubikey y WHERE y.low = :low"),
  @NamedQuery(name = "Yubikey.findByHigh",
          query = "SELECT y FROM Yubikey y WHERE y.high = :high"),
  @NamedQuery(name = "Yubikey.findBySessionUse",
          query = "SELECT y FROM Yubikey y WHERE y.sessionUse = :sessionUse"),
  @NamedQuery(name = "Yubikey.findByCreated",
          query = "SELECT y FROM Yubikey y WHERE y.created = :created"),
  @NamedQuery(name = "Yubikey.findByAesSecret",
          query = "SELECT y FROM Yubikey y WHERE y.aesSecret = :aesSecret"),
  @NamedQuery(name = "Yubikey.findByPublicId",
          query = "SELECT y FROM Yubikey y WHERE y.publicId = :publicId"),
  @NamedQuery(name = "Yubikey.findByAccessed",
          query = "SELECT y FROM Yubikey y WHERE y.accessed = :accessed"),
  @NamedQuery(name = "Yubikey.findByStatus",
          query = "SELECT y FROM Yubikey y WHERE y.status = :status"),
  @NamedQuery(name = "Yubikey.findByYubidnum",
          query = "SELECT y FROM Yubikey y WHERE y.yubidnum = :yubidnum")})
public class Yubikey implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "yubidnum")
  private Integer yubidnum;
  @Size(max = 100)
  @Column(name = "notes")
  private String notes;
  @Column(name = "counter")
  private Integer counter;
  @Column(name = "low")
  private Integer low;
  @Column(name = "high")
  private Integer high;
  @Column(name = "session_use")
  private Integer sessionUse;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @Size(max = 100)
  @Column(name = "aes_secret")
  private String aesSecret;
  @Size(max = 40)
  @Column(name = "public_id")
  private String publicId;
  @Column(name = "accessed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date accessed;
  @Column(name = "status")
  private Integer status;
  @JoinColumn(name = "uid",
          referencedColumnName = "uid")
  @OneToOne(optional = false)
  private Users uid;

  public Yubikey() {
  }

  public Yubikey(Integer yubidnum) {
    this.yubidnum = yubidnum;
  }

  public Yubikey(Integer yubidnum, Date created) {
    this.yubidnum = yubidnum;
    this.created = created;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Integer getCounter() {
    return counter;
  }

  public void setCounter(Integer counter) {
    this.counter = counter;
  }

  public Integer getLow() {
    return low;
  }

  public void setLow(Integer low) {
    this.low = low;
  }

  public Integer getHigh() {
    return high;
  }

  public void setHigh(Integer high) {
    this.high = high;
  }

  public Integer getSessionUse() {
    return sessionUse;
  }

  public void setSessionUse(Integer sessionUse) {
    this.sessionUse = sessionUse;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getAesSecret() {
    return aesSecret;
  }

  public void setAesSecret(String aesSecret) {
    this.aesSecret = aesSecret;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public Date getAccessed() {
    return accessed;
  }

  public void setAccessed(Date accessed) {
    this.accessed = accessed;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getYubidnum() {
    return yubidnum;
  }

  public void setYubidnum(Integer yubidnum) {
    this.yubidnum = yubidnum;
  }

  public Users getUid() {
    return uid;
  }

  public void setUid(Users uid) {
    this.uid = uid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (yubidnum != null ? yubidnum.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Yubikey)) {
      return false;
    }
    Yubikey other = (Yubikey) object;
    if ((this.yubidnum == null && other.yubidnum != null) || (this.yubidnum
            != null && !this.yubidnum.equals(other.yubidnum))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.Yubikey[ yubidnum=" + yubidnum + " ]";
  }

}