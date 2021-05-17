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
package io.hops.hopsworks.alerting.config.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "send_resolved",
  "api_key",
  "api_url",
  "message",
  "description",
  "source",
  "details",
  "responders",
  "tags",
  "note",
  "priority",
  "http_config"
  })
public class OpsgenieConfig {
  @JsonProperty("send_resolved")
  private Boolean sendResolved;
  @JsonProperty("api_key")
  private String apiKey;
  @JsonProperty("api_url")
  private String apiUrl;
  @JsonProperty("message")
  private String message;
  @JsonProperty("description")
  private String description;
  @JsonProperty("source")
  private String source;
  @JsonProperty("details")
  private Map<String, String> details;
  @JsonProperty("responders")
  private List<Responder> responders;
  @JsonProperty("tags")
  private String tags;
  @JsonProperty("note")
  private String note;
  @JsonProperty("priority")
  private String priority;
  @JsonProperty("http_config")
  private HttpConfig httpConfig;
  
  public OpsgenieConfig() {
  }
  
  public OpsgenieConfig(List<Responder> responders) {
    this.responders = responders;
  }
  
  @JsonProperty("send_resolved")
  public Boolean getSendResolved() {
    return sendResolved;
  }
  
  @JsonProperty("send_resolved")
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }
  
  public OpsgenieConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }
  
  @JsonProperty("api_key")
  public String getApiKey() {
    return apiKey;
  }
  
  @JsonProperty("api_key")
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
  
  public OpsgenieConfig withApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }
  
  @JsonProperty("api_url")
  public String getApiUrl() {
    return apiUrl;
  }
  
  @JsonProperty("api_url")
  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }
  
  public OpsgenieConfig withApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }
  
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }
  
  @JsonProperty("message")
  public void setMessage(String message) {
    this.message = message;
  }
  
  public OpsgenieConfig withMessage(String message) {
    this.message = message;
    return this;
  }
  
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }
  
  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }
  
  public OpsgenieConfig withDescription(String description) {
    this.description = description;
    return this;
  }
  
  @JsonProperty("source")
  public String getSource() {
    return source;
  }
  
  @JsonProperty("source")
  public void setSource(String source) {
    this.source = source;
  }
  
  public OpsgenieConfig withSource(String source) {
    this.source = source;
    return this;
  }
  
  @JsonProperty("details")
  public Map<String, String> getDetails() {
    return details;
  }
  
  @JsonProperty("details")
  public void setDetails(Map<String, String> details) {
    this.details = details;
  }
  
  public OpsgenieConfig withDetails(Map<String, String> details) {
    this.details = details;
    return this;
  }
  
  @JsonProperty("responders")
  public List<Responder> getResponders() {
    return responders;
  }
  
  @JsonProperty("responders")
  public void setResponders(List<Responder> responders) {
    this.responders = responders;
  }
  
  public OpsgenieConfig withResponders(List<Responder> responders) {
    this.responders = responders;
    return this;
  }
  
  @JsonProperty("tags")
  public String getTags() {
    return tags;
  }
  
  @JsonProperty("tags")
  public void setTags(String tags) {
    this.tags = tags;
  }
  
  public OpsgenieConfig withTags(String tags) {
    this.tags = tags;
    return this;
  }
  
  @JsonProperty("note")
  public String getNote() {
    return note;
  }
  
  @JsonProperty("note")
  public void setNote(String note) {
    this.note = note;
  }
  
  public OpsgenieConfig withNote(String note) {
    this.note = note;
    return this;
  }
  
  @JsonProperty("priority")
  public String getPriority() {
    return priority;
  }
  
  @JsonProperty("priority")
  public void setPriority(String priority) {
    this.priority = priority;
  }
  
  public OpsgenieConfig withPriority(String priority) {
    this.priority = priority;
    return this;
  }
  
  @JsonProperty("http_config")
  public HttpConfig getHttpConfig() {
    return httpConfig;
  }
  
  @JsonProperty("http_config")
  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }
  
  public OpsgenieConfig withHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
    return this;
  }
  
  @Override
  public String toString() {
    return "OpsgenieConfig{" +
      "sendResolved=" + sendResolved +
      ", apiKey='" + apiKey + '\'' +
      ", apiUrl='" + apiUrl + '\'' +
      ", message='" + message + '\'' +
      ", description='" + description + '\'' +
      ", source='" + source + '\'' +
      ", details=" + details +
      ", responders=" + responders +
      ", tags='" + tags + '\'' +
      ", note='" + note + '\'' +
      ", priority='" + priority + '\'' +
      ", httpConfig=" + httpConfig +
      '}';
  }
}
