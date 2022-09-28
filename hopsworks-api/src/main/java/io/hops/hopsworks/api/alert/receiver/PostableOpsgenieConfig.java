/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.alert.receiver;

import io.hops.hopsworks.alerting.config.dto.HttpConfig;
import io.hops.hopsworks.alerting.config.dto.OpsgenieConfig;
import io.hops.hopsworks.alerting.config.dto.Responder;
import io.hops.hopsworks.api.alert.Entry;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@XmlRootElement
public class PostableOpsgenieConfig {
  private Boolean sendResolved;
  private String apiKey;
  private String apiUrl;
  private String message;
  private String description;
  private String source;
  private List<Entry> details;
  private List<Responder> responders;
  private String tags;
  private String note;
  private String priority;
  private HttpConfig httpConfig;
  
  public Boolean getSendResolved() {
    return sendResolved;
  }
  
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }
  
  public String getApiKey() {
    return apiKey;
  }
  
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
  
  public String getApiUrl() {
    return apiUrl;
  }
  
  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }
  
  public String getMessage() {
    return message;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getSource() {
    return source;
  }
  
  public void setSource(String source) {
    this.source = source;
  }
  
  public List<Entry> getDetails() {
    return details;
  }
  
  public void setDetails(List<Entry> details) {
    this.details = details;
  }
  
  public List<Responder> getResponders() {
    return responders;
  }
  
  public void setResponders(List<Responder> responders) {
    this.responders = responders;
  }
  
  public String getTags() {
    return tags;
  }
  
  public void setTags(String tags) {
    this.tags = tags;
  }
  
  public String getNote() {
    return note;
  }
  
  public void setNote(String note) {
    this.note = note;
  }
  
  public String getPriority() {
    return priority;
  }
  
  public void setPriority(String priority) {
    this.priority = priority;
  }
  
  public HttpConfig getHttpConfig() {
    return httpConfig;
  }
  
  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }
  
  public OpsgenieConfig toOpsgenieConfig() {
    Map<String, String> details = null;
    if (this.getDetails() != null && !this.getDetails().isEmpty()) {
      details = this.getDetails().stream()
        .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
    return new OpsgenieConfig()
      .withSendResolved(this.getSendResolved())
      .withApiKey(this.getApiKey())
      .withApiUrl(this.getApiUrl())
      .withMessage(this.getMessage())
      .withDescription(this.getDescription())
      .withSource(this.getSource())
      .withDetails(details)
      .withResponders(this.getResponders())
      .withTags(this.getTags())
      .withNote(this.getNote())
      .withPriority(this.getPriority())
      .withHttpConfig(this.getHttpConfig());
  }
}
