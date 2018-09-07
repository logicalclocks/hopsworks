/*
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
 */

package io.hops.hopsworks.api.agent;


import io.hops.hopsworks.common.dao.alert.Alert;
import io.swagger.annotations.ApiModel;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ApiModel(value = "Kagent alerting protocol")
@XmlRootElement
public class AlertView {
  private Alert.Provider provider;
  private Alert.Severity severity;
  private Long time;
  private String message;
  @XmlElement(name = "host-id")
  private String hostId;
  private String plugin;
  @XmlElement(name = "plugin-instance")
  private String pluginInstance;
  private String type;
  @XmlElement(name = "type-instance")
  private String typeInstance;
  @XmlElement(name = "datasource")
  private String dataSource;
  @XmlElement(name = "current-value")
  private String currentValue;
  @XmlElement(name = "warning-min")
  private String warningMin;
  @XmlElement(name = "warning-max")
  private String warningMax;
  @XmlElement(name = "failure-min")
  private String failureMin;
  @XmlElement(name = "failure-max")
  private String failureMax;
  
  public AlertView() {
  }
  
  public Alert.Provider getProvider() {
    return provider;
  }
  
  public void setProvider(Alert.Provider provider) {
    this.provider = provider;
  }
  
  public Alert.Severity getSeverity() {
    return severity;
  }
  
  public void setSeverity(Alert.Severity severity) {
    this.severity = severity;
  }
  
  public Long getTime() {
    return time;
  }
  
  public void setTime(Long time) {
    this.time = time;
  }
  
  public String getMessage() {
    return message;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
  
  public String getHostId() {
    return hostId;
  }
  
  public void setHostId(String hostId) {
    this.hostId = hostId;
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
  
  public String getDataSource() {
    return dataSource;
  }
  
  public void setDataSource(String dataSource) {
    this.dataSource = dataSource;
  }
  
  public String getCurrentValue() {
    return currentValue;
  }
  
  public void setCurrentValue(String currentValue) {
    this.currentValue = currentValue;
  }
  
  public String getWarningMin() {
    return warningMin;
  }
  
  public void setWarningMin(String warningMin) {
    this.warningMin = warningMin;
  }
  
  public String getWarningMax() {
    return warningMax;
  }
  
  public void setWarningMax(String warningMax) {
    this.warningMax = warningMax;
  }
  
  public String getFailureMin() {
    return failureMin;
  }
  
  public void setFailureMin(String failureMin) {
    this.failureMin = failureMin;
  }
  
  public String getFailureMax() {
    return failureMax;
  }
  
  public void setFailureMax(String failureMax) {
    this.failureMax = failureMax;
  }
  
  public Alert toAlert() {
    final Alert alert = new Alert();
    alert.setAlertTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
    alert.setProvider(provider.toString());
    alert.setSeverity(severity.toString());
    alert.setAgentTime(BigInteger.valueOf(time));
    alert.setMessage(message);
    alert.setPlugin(plugin);
    alert.setPluginInstance(pluginInstance);
    alert.setType(type);
    alert.setTypeInstance(typeInstance);
    alert.setDataSource(dataSource);
    alert.setCurrentValue(currentValue);
    alert.setWarningMin(warningMin);
    alert.setWarningMax(warningMax);
    alert.setFailureMin(failureMin);
    alert.setFailureMax(failureMax);
    
    return alert;
  }
}
