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
import se.kth.bbc.security.ua.model.User;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "account_audit")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "AccountAudit.findAll",
          query = "SELECT a FROM AccountAudit a"),
  @NamedQuery(name = "AccountAudit.findByLogId",
          query = "SELECT a FROM AccountAudit a WHERE a.logId = :logId"),
  @NamedQuery(name = "AccountAudit.findByAction",
          query = "SELECT a FROM AccountAudit a WHERE a.action = :action"),
  @NamedQuery(name = "AccountAudit.findByTime",
          query = "SELECT a FROM AccountAudit a WHERE a.time = :time"),
  @NamedQuery(name = "AccountAudit.findByMessage",
          query = "SELECT a FROM AccountAudit a WHERE a.message = :message"),
  @NamedQuery(name = "AccountAudit.findByOutcome",
          query = "SELECT a FROM AccountAudit a WHERE a.outcome = :outcome"),
  @NamedQuery(name = "AccountAudit.findByIp",
          query = "SELECT a FROM AccountAudit a WHERE a.ip = :ip"),
  @NamedQuery(name = "AccountAudit.findByEmail",
          query = "SELECT a FROM AccountAudit a WHERE a.email = :email"),
  @NamedQuery(name = "AccountAudit.findByBrowser",
          query = "SELECT a FROM AccountAudit a WHERE a.browser = :browser"),
  @NamedQuery(name = "AccountAudit.findByOs",
          query = "SELECT a FROM AccountAudit a WHERE a.os = :os"),
  @NamedQuery(name = "AccountAudit.findByMac",
          query = "SELECT a FROM AccountAudit a WHERE a.mac = :mac")})
public class AccountAudit implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "log_id")
  private Integer logId;
  @Size(max = 45)
  @Column(name = "action")
  private String action;
  @Column(name = "time")
  @Temporal(TemporalType.DATE)
  private Date time;
  @Size(max = 100)
  @Column(name = "message")
  private String message;
  @Size(max = 45)
  @Column(name = "outcome")
  private String outcome;
  @Size(max = 45)
  @Column(name = "ip")
  private String ip;
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
  @JoinColumn(name = "initiator",
          referencedColumnName = "uid")
  @ManyToOne
  private Users initiator;

  public AccountAudit() {
  }

  public AccountAudit(Integer logId) {
    this.logId = logId;
  }

  public Integer getLogId() {
    return logId;
  }

  public void setLogId(Integer logId) {
    this.logId = logId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
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

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getBrowser() {
    return browser;
  }

  public void setBrowser(String browser) {
    this.browser = browser;
  }

  public String getOs() {
    return os;
  }

  public void setOs(String os) {
    this.os = os;
  }

  public String getMac() {
    return mac;
  }

  public void setMac(String mac) {
    this.mac = mac;
  }

  public Users getInitiator() {
    return initiator;
  }

  public void setInitiator(Users initiator) {
    this.initiator = initiator;
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
    if (!(object instanceof AccountAudit)) {
      return false;
    }
    AccountAudit other = (AccountAudit) object;
    if ((this.logId == null && other.logId != null) || (this.logId != null
            && !this.logId.equals(other.logId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bb.security.audit.model.AccountAudit[ logId=" + logId + " ]";
  }

}
