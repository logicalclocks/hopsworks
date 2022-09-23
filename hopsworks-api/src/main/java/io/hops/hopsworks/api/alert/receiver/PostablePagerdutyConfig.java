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
import io.hops.hopsworks.alerting.config.dto.Imageconfig;
import io.hops.hopsworks.alerting.config.dto.LinkConfig;
import io.hops.hopsworks.alerting.config.dto.PagerdutyConfig;
import io.hops.hopsworks.api.alert.Entry;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@XmlRootElement
public class PostablePagerdutyConfig {
  private Boolean sendResolved;
  private String routingKey;
  private String serviceKey;
  private String url;
  private String client;
  private String clientUrl;
  private String description;
  private String severity;
  private List<Entry> details;
  private List<Imageconfig> images;
  private List<LinkConfig> links;
  private HttpConfig httpConfig;

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  public String getServiceKey() {
    return serviceKey;
  }

  public void setServiceKey(String serviceKey) {
    this.serviceKey = serviceKey;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getClient() {
    return client;
  }

  public void setClient(String client) {
    this.client = client;
  }

  public String getClientUrl() {
    return clientUrl;
  }

  public void setClientUrl(String clientUrl) {
    this.clientUrl = clientUrl;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public List<Entry> getDetails() {
    return details;
  }

  public void setDetails(List<Entry> details) {
    this.details = details;
  }

  public List<Imageconfig> getImages() {
    return images;
  }

  public void setImages(List<Imageconfig> images) {
    this.images = images;
  }

  public List<LinkConfig> getLinks() {
    return links;
  }

  public void setLinks(List<LinkConfig> links) {
    this.links = links;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public PagerdutyConfig toPagerdutyConfig() {
    Map<String, String> details = null;
    if (this.getDetails() != null && !this.getDetails().isEmpty()) {
      details = this.getDetails().stream()
        .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
    return new PagerdutyConfig()
      .withSendResolved(this.getSendResolved())
      .withRoutingKey(this.getRoutingKey())
      .withServiceKey(this.getServiceKey())
      .withUrl(this.getUrl())
      .withClient(this.getClient())
      .withClientUrl(this.getClientUrl())
      .withDescription(this.getDescription())
      .withSeverity(this.getSeverity())
      .withDetails(details)
      .withImages(this.getImages())
      .withLinks(this.getLinks())
      .withHttpConfig(this.getHttpConfig());
  }
}
