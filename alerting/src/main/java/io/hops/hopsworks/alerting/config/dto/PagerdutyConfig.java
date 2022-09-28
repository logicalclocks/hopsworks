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
import com.google.common.base.Strings;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagerdutyConfig {
  @JsonAlias({"send_resolved"})
  private Boolean sendResolved;
  @JsonAlias({"routing_key"})
  private String routingKey;
  @JsonAlias({"service_key"})
  private String serviceKey;
  private String url;
  private String client;
  @JsonAlias({"client_url"})
  private String clientUrl;
  private String description;
  private String severity;
  private Map<String, String> details;
  private List<Imageconfig> images;
  private List<LinkConfig> links;
  @JsonAlias({"http_config"})
  private HttpConfig httpConfig;
  
  public PagerdutyConfig() {
  }
  
  /**
   * @param serviceKey
   */
  public PagerdutyConfig(String serviceKey) {
    this.serviceKey = serviceKey;
  }

  public String getServiceKey() {
    return serviceKey;
  }
  
  public void setServiceKey(String serviceKey) {
    this.serviceKey = serviceKey;
  }
  
  public PagerdutyConfig withServiceKey(String serviceKey) {
    if (!Strings.isNullOrEmpty(this.routingKey) && !Strings.isNullOrEmpty(serviceKey)) {
      throw new IllegalArgumentException("The two options serviceKey and routingKey are mutually exclusive.");
    }
    this.serviceKey = serviceKey;
    return this;
  }
  
  public Boolean getSendResolved() {
    return sendResolved;
  }
  
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }
  
  public PagerdutyConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }
  
  public String getRoutingKey() {
    return routingKey;
  }
  
  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }
  
  public PagerdutyConfig withRoutingKey(String routingKey) {
    if (!Strings.isNullOrEmpty(this.serviceKey) && !Strings.isNullOrEmpty(routingKey)) {
      throw new IllegalArgumentException("The two options serviceKey and routingKey are mutually exclusive.");
    }
    this.routingKey = routingKey;
    return this;
  }
  
  public String getUrl() {
    return url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public PagerdutyConfig withUrl(String url) {
    this.url = url;
    return this;
  }
  
  public String getClient() {
    return client;
  }
  
  public void setClient(String client) {
    this.client = client;
  }
  
  public PagerdutyConfig withClient(String client) {
    this.client = client;
    return this;
  }
  
  public String getClientUrl() {
    return clientUrl;
  }
  
  public void setClientUrl(String clientUrl) {
    this.clientUrl = clientUrl;
  }
  
  public PagerdutyConfig withClientUrl(String clientUrl) {
    this.clientUrl = clientUrl;
    return this;
  }

  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public PagerdutyConfig withDescription(String description) {
    this.description = description;
    return this;
  }
  
  public String getSeverity() {
    return severity;
  }
  
  public void setSeverity(String severity) {
    this.severity = severity;
  }
  
  public PagerdutyConfig withSeverity(String severity) {
    this.severity = severity;
    return this;
  }
  
  public Map<String, String> getDetails() {
    return details;
  }
  
  public void setDetails(Map<String, String> details) {
    this.details = details;
  }
  
  public PagerdutyConfig withDetails(Map<String, String> details) {
    this.details = details;
    return this;
  }
  
  public List<Imageconfig> getImages() {
    return images;
  }
  
  public void setImages(List<Imageconfig> images) {
    this.images = images;
  }
  
  public PagerdutyConfig withImages(List<Imageconfig> images) {
    this.images = images;
    return this;
  }
  
  public List<LinkConfig> getLinks() {
    return links;
  }
  
  public void setLinks(List<LinkConfig> links) {
    this.links = links;
  }
  
  public PagerdutyConfig withLinks(List<LinkConfig> links) {
    this.links = links;
    return this;
  }
  
  public HttpConfig getHttpConfig() {
    return httpConfig;
  }
  
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
