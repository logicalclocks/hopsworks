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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpsgenieConfig {
  @JsonAlias({"send_resolved"})
  private Boolean sendResolved;
  @JsonAlias({"api_key"})
  private String apiKey;
  @JsonAlias({"api_url"})
  private String apiUrl;
  private String message;
  private String description;
  private String source;
  private Map<String, String> details;
  private List<Responder> responders;
  private String tags;
  private String note;
  private String priority;
  @JsonAlias({"http_config"})
  private HttpConfig httpConfig;

  public OpsgenieConfig() {
  }

  public OpsgenieConfig(List<Responder> responders) {
    this.responders = responders;
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public OpsgenieConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public OpsgenieConfig withApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public OpsgenieConfig withApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public OpsgenieConfig withMessage(String message) {
    this.message = message;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public OpsgenieConfig withDescription(String description) {
    this.description = description;
    return this;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public OpsgenieConfig withSource(String source) {
    this.source = source;
    return this;
  }

  public Map<String, String> getDetails() {
    return details;
  }

  public void setDetails(Map<String, String> details) {
    this.details = details;
  }

  public OpsgenieConfig withDetails(Map<String, String> details) {
    this.details = details;
    return this;
  }

  public List<Responder> getResponders() {
    return responders;
  }

  public void setResponders(List<Responder> responders) {
    this.responders = responders;
  }

  public OpsgenieConfig withResponders(List<Responder> responders) {
    this.responders = responders;
    return this;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public OpsgenieConfig withTags(String tags) {
    this.tags = tags;
    return this;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public OpsgenieConfig withNote(String note) {
    this.note = note;
    return this;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public OpsgenieConfig withPriority(String priority) {
    this.priority = priority;
    return this;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

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
