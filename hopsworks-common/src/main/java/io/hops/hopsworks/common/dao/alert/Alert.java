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

package io.hops.hopsworks.common.dao.alert;

import io.hops.hopsworks.common.dao.host.Hosts;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import java.math.BigInteger;
import javax.persistence.Basic;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "hopsworks.alerts")
@NamedQueries({
  @NamedQuery(name = "Alerts.findAll",
          query
          = "SELECT a FROM Alert a WHERE a.alertTime >= :fromdate AND "
          + "a.alertTime <= :todate ORDER BY a.alertTime DESC"),
  @NamedQuery(name = "Alerts.findBy-Severity",
          query
          = "SELECT a FROM Alert a WHERE a.alertTime >= :fromdate AND "
          + "a.alertTime <= :todate AND "
          + "a.severity = :severity ORDER BY a.alertTime DESC"),
  @NamedQuery(name = "Alerts.findBy-Provider",
          query
          = "SELECT a FROM Alert a WHERE a.alertTime >= :fromdate AND "
          + "a.alertTime <= :todate AND "
          + "a.provider = :provider ORDER BY a.alertTime DESC"),
  @NamedQuery(name = "Alerts.findBy-Provider-Severity",
          query
          = "SELECT a FROM Alert a WHERE a.alertTime >= :fromdate AND "
          + "a.alertTime <= :todate AND a.severity = :severity AND "
          + "a.provider = :provider ORDER BY a.alertTime DESC"),
  @NamedQuery(name = "Alerts.removeAll",
          query = "DELETE FROM Alert a")})
public class Alert implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum Severity {
    FAILURE,
    WARNING,
    OK
  }

  public enum Provider {
    Collectd,
    Agent
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Long id;
  @Size(max = 32)
  @Column(name = "current_value")
  private String currentValue;
  @Size(max = 32)
  @Column(name = "failure_max")
  private String failureMax;
  @Size(max = 32)
  @Column(name = "failure_min")
  private String failureMin;
  @Size(max = 32)
  @Column(name = "warning_max")
  private String warningMax;
  @Size(max = 32)
  @Column(name = "warning_min")
  private String warningMin;
  @Column(name = "agent_time")
  private BigInteger agentTime;
  @Column(name = "alert_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date alertTime;
  @Size(max = 128)
  @Column(name = "data_source")
  private String dataSource;
  @JoinColumn(name = "host_id",
      referencedColumnName = "id")
  @ManyToOne
  private Hosts host;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 1024)
  @Column(name = "message")
  private String message;
    
  @Size(max = 128)
  @Column(name = "plugin")
  private String plugin;
  @Size(max = 128)
  @Column(name = "plugin_instance")
  private String pluginInstance;
  @Column(name = "provider")
  private String provider;
  @Column(name = "severity")
  private String severity;
  @Size(max = 128)
  @Column(name = "type")
  private String type;
  @Size(max = 128)
  @Column(name = "type_instance")
  private String typeInstance;

  public Alert() {
  }

  public Alert(Hosts host, String message, String plugin,
          String pluginInstance, String type, String typeInstance) {
    this.host = host;
    this.message = message;
    this.plugin = plugin;
    this.pluginInstance = pluginInstance;
    this.type = type;
    this.typeInstance = typeInstance;
  }

  public Alert(Long id) {
    this.id = id;
  }

  public Alert(Long id, String message) {
    this.id = id;
    this.message = message;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCurrentValue() {
    return currentValue;
  }

  public void setCurrentValue(String currentValue) {
    this.currentValue = currentValue;
  }

  public String getFailureMax() {
    return failureMax;
  }

  public void setFailureMax(String failureMax) {
    this.failureMax = failureMax;
  }

  public String getFailureMin() {
    return failureMin;
  }

  public void setFailureMin(String failureMin) {
    this.failureMin = failureMin;
  }

  public String getWarningMax() {
    return warningMax;
  }

  public void setWarningMax(String warningMax) {
    this.warningMax = warningMax;
  }

  public String getWarningMin() {
    return warningMin;
  }

  public void setWarningMin(String warningMin) {
    this.warningMin = warningMin;
  }

  public BigInteger getAgentTime() {
    return agentTime;
  }

  public void setAgentTime(BigInteger agentTime) {
    this.agentTime = agentTime;
  }

  public Date getAlertTime() {
    return alertTime;
  }

  public void setAlertTime(Date alertTime) {
    this.alertTime = alertTime;
  }

  public String getDataSource() {
    return dataSource;
  }

  public void setDataSource(String dataSource) {
    this.dataSource = dataSource;
  }

  public Hosts getHost() {
    return host;
  }

  public void setHost(Hosts host) {
    this.host = host;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getPlugin() {
    return plugin;
  }

  public void setPlugin(String plugin) {
    this.plugin = plugin;
  }

  public String getPluginInstance() {
    return pluginInstance;
  }

  public void setPluginInstance(String pluginInstance) {
    this.pluginInstance = pluginInstance;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTypeInstance() {
    return typeInstance;
  }

  public void setTypeInstance(String typeInstance) {
    this.typeInstance = typeInstance;
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
    if (!(object instanceof Alert)) {
      return false;
    }
    Alert other = (Alert) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Alert: " + message + "\r"
         + "Host : " + host.getHostname() + "\r"
         + "Type: " + type +  "\r"
         + "Type Instance: " + typeInstance +  "\r"
         + "Current Value: " + currentValue +  "\r"
         + "Time: " + alertTime + "\r"
        ;
  }

}
