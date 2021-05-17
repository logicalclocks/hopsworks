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
import com.google.common.base.Strings;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "send_resolved",
  "routing_key",
  "service_key",
  "url",
  "client",
  "client_url",
  "description",
  "severity",
  "details",
  "images",
  "links",
  "http_config"
  })
public class PagerdutyConfig {
  
  @JsonProperty("send_resolved")
  private Boolean sendResolved;
  @JsonProperty("routing_key")
  private String routingKey;
  @JsonProperty("service_key")
  private String serviceKey;
  @JsonProperty("url")
  private String url;
  @JsonProperty("client")
  private String client;
  @JsonProperty("client_url")
  private String clientUrl;
  @JsonProperty("description")
  private String description;
  @JsonProperty("severity")
  private String severity;
  @JsonProperty("details")
  private Map<String, String> details;
  @JsonProperty("images")
  private List<Imageconfig> images;
  @JsonProperty("links")
  private List<LinkConfig> links;
  @JsonProperty("http_config")
  private HttpConfig httpConfig;
  
  public PagerdutyConfig() {
  }
  
  /**
   * @param serviceKey
   */
  public PagerdutyConfig(String serviceKey) {
    this.serviceKey = serviceKey;
  }
  
  @JsonProperty("service_key")
  public String getServiceKey() {
    return serviceKey;
  }
  
  @JsonProperty("service_key")
  public void setServiceKey(String serviceKey) {
    this.serviceKey = serviceKey;
  }
  
  public PagerdutyConfig withServiceKey(String serviceKey) {
    if (!Strings.isNullOrEmpty(this.routingKey)) {
      throw new IllegalArgumentException("The two options serviceKey and routingKey are mutually exclusive.");
    }
    this.serviceKey = serviceKey;
    return this;
  }
  
  @JsonProperty("send_resolved")
  public Boolean getSendResolved() {
    return sendResolved;
  }
  
  @JsonProperty("send_resolved")
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }
  
  public PagerdutyConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }
  
  @JsonProperty("routing_key")
  public String getRoutingKey() {
    return routingKey;
  }
  
  @JsonProperty("routing_key")
  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }
  
  public PagerdutyConfig withRoutingKey(String routingKey) {
    if (!Strings.isNullOrEmpty(this.serviceKey)) {
      throw new IllegalArgumentException("The two options serviceKey and routingKey are mutually exclusive.");
    }
    this.routingKey = routingKey;
    return this;
  }
  
  @JsonProperty("url")
  public String getUrl() {
    return url;
  }
  
  @JsonProperty("url")
  public void setUrl(String url) {
    this.url = url;
  }
  
  public PagerdutyConfig withUrl(String url) {
    this.url = url;
    return this;
  }
  
  @JsonProperty("client")
  public String getClient() {
    return client;
  }
  
  @JsonProperty("client")
  public void setClient(String client) {
    this.client = client;
  }
  
  public PagerdutyConfig withClient(String client) {
    this.client = client;
    return this;
  }
  
  @JsonProperty("client_url")
  public String getClientUrl() {
    return clientUrl;
  }
  
  @JsonProperty("client_url")
  public void setClientUrl(String clientUrl) {
    this.clientUrl = clientUrl;
  }
  
  public PagerdutyConfig withClientUrl(String clientUrl) {
    this.clientUrl = clientUrl;
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
  
  public PagerdutyConfig withDescription(String description) {
    this.description = description;
    return this;
  }
  
  @JsonProperty("severity")
  public String getSeverity() {
    return severity;
  }
  
  @JsonProperty("severity")
  public void setSeverity(String severity) {
    this.severity = severity;
  }
  
  public PagerdutyConfig withSeverity(String severity) {
    this.severity = severity;
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
  
  public PagerdutyConfig withDetails(Map<String, String> details) {
    this.details = details;
    return this;
  }
  
  @JsonProperty("images")
  public List<Imageconfig> getImages() {
    return images;
  }
  
  @JsonProperty("images")
  public void setImages(List<Imageconfig> images) {
    this.images = images;
  }
  
  public PagerdutyConfig withImages(List<Imageconfig> images) {
    this.images = images;
    return this;
  }
  
  @JsonProperty("links")
  public List<LinkConfig> getLinks() {
    return links;
  }
  
  @JsonProperty("links")
  public void setLinks(List<LinkConfig> links) {
    this.links = links;
  }
  
  public PagerdutyConfig withLinks(List<LinkConfig> links) {
    this.links = links;
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
  
  public PagerdutyConfig withHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
    return this;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PagerdutyConfig that = (PagerdutyConfig) o;
    return Objects.equals(routingKey, that.routingKey) &&
        Objects.equals(serviceKey, that.serviceKey) && Objects.equals(url, that.url);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(routingKey, serviceKey, url);
  }
  
  @Override
  public String toString() {
    return "PagerdutyConfig{" +
      "sendResolved=" + sendResolved +
      ", routingKey='" + routingKey + '\'' +
      ", serviceKey='" + serviceKey + '\'' +
      ", url='" + url + '\'' +
      ", client='" + client + '\'' +
      ", clientUrl='" + clientUrl + '\'' +
      ", description='" + description + '\'' +
      ", severity='" + severity + '\'' +
      ", details=" + details +
      ", images=" + images +
      ", links=" + links +
      ", httpConfig=" + httpConfig +
      '}';
  }
}
