/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.user.security.audit;

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
import io.hops.hopsworks.common.dao.user.Users;

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
  @NamedQuery(name = "RolesAudit.findByTarget",
          query = "SELECT r FROM RolesAudit r WHERE r.target = :target"),
  @NamedQuery(name = "RolesAudit.findByAction",
          query = "SELECT r FROM RolesAudit r WHERE r.action = :action"),
  @NamedQuery(name = "RolesAudit.findByTime",
          query = "SELECT r FROM RolesAudit r WHERE r.actionTimestamp = :actionTimestamp"),
  @NamedQuery(name = "RolesAudit.findByMessage",
          query = "SELECT r FROM RolesAudit r WHERE r.message = :message"),
  @NamedQuery(name = "RolesAudit.findByIp",
          query = "SELECT r FROM RolesAudit r WHERE r.ip = :ip"),
  @NamedQuery(name = "RolesAudit.findByUserAgent",
        query = "SELECT r FROM RolesAudit r WHERE r.userAgent = :userAgent"),
  @NamedQuery(name = "RolesAudit.findByOutcome",
          query = "SELECT r FROM RolesAudit r WHERE r.outcome = :outcome")})
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
  @Column(name = "action_timestamp")
  @Temporal(TemporalType.TIMESTAMP)
  private Date actionTimestamp;
  @Size(max = 45)
  @Column(name = "message")
  private String message;
  @Size(max = 255)
  @Column(name = "useragent")
  private String userAgent;
  @Size(max = 45)
  @Column(name = "ip")
  private String ip;
  @Size(max = 45)
  @Column(name = "outcome")
  private String outcome;
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

  public RolesAudit(String action, Date time, String message, String userAgent, String ip,
                    String outcome, Users target, Users initiator) {
    this.action = action;
    this.actionTimestamp = time;
    this.message = message;
    this.userAgent = userAgent;
    this.ip = ip;
    this.outcome = outcome;
    this.target = target;
    this.initiator = initiator;
  }

  public RolesAudit(Long logId) { this.logId = logId; }

  public Long getLogId() { return logId; }

  public void setLogId(Long logId) { this.logId = logId; }

  public Users getInitiator() { return initiator; }

  public void setInitiator(Users initiator) { this.initiator = initiator; }

  public String getAction() { return action; }

  public void setAction(String action) { this.action = action; }

  public Date getActionTimestamp() { return actionTimestamp; }

  public void setActionTimestamp(Date time) { this.actionTimestamp = time; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public String getIp() { return ip; }

  public void setIp(String ip) { this.ip = ip; }

  public String getOutcome() { return outcome; }

  public void setOutcome(String outcome) { this.outcome = outcome; }

  public String getUserAgent() { return userAgent; }

  public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

  public Users getTarget() { return target; }

  public void setTarget(Users target) { this.target = target; }

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
    return "RolesAudit{" +
        "logId=" + logId +
        ", action='" + action + '\'' +
        ", time=" + actionTimestamp +
        ", message='" + message + '\'' +
        ", userAgent='" + userAgent + '\'' +
        ", ip='" + ip + '\'' +
        ", outcome='" + outcome + '\'' +
        ", target=" + target +
        ", initiator=" + initiator +
        '}';
  }
}
