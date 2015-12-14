package se.kth.bbc.security.audit.model;

import io.hops.bbc.Consents;
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
@Table(name = "consents_audit")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ConsentsAudit.findAll",
          query
          = "SELECT c FROM ConsentsAudit c"),
  @NamedQuery(name = "ConsentsAudit.findByLogId",
          query
          = "SELECT c FROM ConsentsAudit c WHERE c.logId = :logId"),
  @NamedQuery(name = "ConsentsAudit.findByAction",
          query
          = "SELECT c FROM ConsentsAudit c WHERE c.action = :action"),
  @NamedQuery(name = "ConsentsAudit.findByTime",
          query
          = "SELECT c FROM ConsentsAudit c WHERE c.time = :time"),
  @NamedQuery(name = "ConsentsAudit.findByMessage",
          query
          = "SELECT c FROM ConsentsAudit c WHERE c.message = :message"),
  @NamedQuery(name = "ConsentsAudit.findByOutcome",
          query
          = "SELECT c FROM ConsentsAudit c WHERE c.outcome = :outcome"),
  @NamedQuery(name = "ConsentsAudit.findByIp",
          query
          = "SELECT c FROM ConsentsAudit c WHERE c.ip = :ip"),
  @NamedQuery(name = "ConsentsAudit.findByBrowser",
          query
          = "SELECT c FROM ConsentsAudit c WHERE c.browser = :browser"),
  @NamedQuery(name = "ConsentsAudit.findByOs",
          query
          = "SELECT c FROM ConsentsAudit c WHERE c.os = :os"),
  @NamedQuery(name = "ConsentsAudit.findByMac",
          query
          = "SELECT c FROM ConsentsAudit c WHERE c.mac = :mac")})
public class ConsentsAudit implements Serializable {
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
  @Temporal(TemporalType.TIMESTAMP)
  private Date time;
  @Size(max = 500)
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
  @Size(max = 45)
  @Column(name = "mac")
  private String mac;

  @JoinColumn(name = "initiator",
          referencedColumnName = "uid")
  @ManyToOne
  private Users initiator;
  
  @JoinColumn(name = "consent_id",
          referencedColumnName = "id")
  @ManyToOne
  private Consents consentID;
  
  public ConsentsAudit() {
  }

  public ConsentsAudit(Long logId) {
    this.logId = logId;
  }

  public Long getLogId() {
    return logId;
  }

  public void setLogId(Long logId) {
    this.logId = logId;
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

  public Consents getConsentID() {
    return consentID;
  }

  public void setConsentID(Consents consentID) {
    this.consentID = consentID;
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
    if (!(object instanceof ConsentsAudit)) {
      return false;
    }
    ConsentsAudit other = (ConsentsAudit) object;
    if ((this.logId == null && other.logId != null) ||
            (this.logId != null && !this.logId.equals(other.logId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.audit.model.ConsentsAudit[ logId=" + logId + " ]";
  }
  
}
