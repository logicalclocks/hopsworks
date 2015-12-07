/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.audit.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.hopsworks.user.model.Users;
 
@Entity
@Table(name = "hopsworks.roles_audit")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "RolesAudit.findAll",
          query = "SELECT r FROM RolesAudit r"),
  @NamedQuery(name = "RolesAudit.findByLogId",
          query = "SELECT r FROM RolesAudit r WHERE r.logId = :logId"),
  @NamedQuery(name = "RolesAudit.findByInitiator",
          query = "SELECT r FROM RolesAudit r WHERE r.initiator = :initiator"),
  @NamedQuery(name = "RolesAudit.findByAction",
          query = "SELECT r FROM RolesAudit r WHERE r.action = :action"),
  @NamedQuery(name = "RolesAudit.findByTime",
          query = "SELECT r FROM RolesAudit r WHERE r.time = :time"),
  @NamedQuery(name = "RolesAudit.findByMessage",
          query = "SELECT r FROM RolesAudit r WHERE r.message = :message"),
  @NamedQuery(name = "RolesAudit.findByIp",
          query = "SELECT r FROM RolesAudit r WHERE r.ip = :ip"),
  @NamedQuery(name = "RolesAudit.findByOs",
          query = "SELECT r FROM RolesAudit r WHERE r.os = :os"),
  @NamedQuery(name = "RolesAudit.findByOutcome",
          query = "SELECT r FROM RolesAudit r WHERE r.outcome = :outcome"),
  @NamedQuery(name = "RolesAudit.findByBrowser",
          query = "SELECT r FROM RolesAudit r WHERE r.browser = :browser"),
  @NamedQuery(name = "RolesAudit.findByMac",
          query = "SELECT r FROM RolesAudit r WHERE r.mac = :mac")})
public class RolesAudit implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "log_id")
  private Long logId;
  @Size(max = 45)
  @Column(name = "action")
  private String action;
  @Column(name = "time")
  @Temporal(TemporalType.DATE)
  private Date time;
  @Size(max = 45)
  @Column(name = "message")
  private String message;
  @Size(max = 45)
  @Column(name = "ip")
  private String ip;
  @Size(max = 45)
  @Column(name = "outcome")
  private String outcome;
  @Size(max = 45)
  @Column(name = "browser")
  private String browser;
  @Size(max = 45)
  @Column(name = "os")
  private String os;
  @Size(max = 254)
  @Column(name = "email")
  private String email;
  @Size(max = 45)
  @Column(name = "mac")
  private String mac;
  @JoinColumn(name = "target",
          referencedColumnName = "uid")
  @ManyToOne
  private Users target;

  @JoinColumn(name = "initiator",
          referencedColumnName = "uid")
  @ManyToOne
  private Users initiator;
  
  public RolesAudit() {
  }

  public RolesAudit(Long logId) {
    this.logId = logId;
  }

  public Long getLogId() {
    return logId;
  }

  public void setLogId(Long logId) {
    this.logId = logId;
  }

  public Users getInitiator() {
    return initiator;
  }

  public void setInitiator(Users initiator) {
    this.initiator = initiator;
  }

 
  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getBrowser() {
    return browser;
  }

  public void setBrowser(String browser) {
    this.browser = browser;
  }

  public String getMac() {
    return mac;
  }

  public void setMac(String mac) {
    this.mac = mac;
  }

  public Users getTarget() {
    return target;
  }

  public void setTarget(Users target) {
    this.target = target;
  }

  public String getOs() {
    return os;
  }

  public void setOs(String os) {
    this.os = os;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (logId != null ? logId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof RolesAudit)) {
      return false;
    }
    RolesAudit other = (RolesAudit) object;
    if ((this.logId == null && other.logId != null) || (this.logId != null
            && !this.logId.equals(other.logId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bb.security.audit.model.RolesAudit[ logId=" + logId + " ]";
  }

}
