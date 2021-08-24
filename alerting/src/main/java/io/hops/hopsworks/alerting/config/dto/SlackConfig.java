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
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "send_resolved",
  "api_url",
  "channel",
  "icon_emoji",
  "icon_url",
  "link_names",
  "username",
  "actions",
  "callback_id",
  "color",
  "fallback",
  "fields",
  "footer",
  "mrkdwn_in",
  "pretext",
  "short_fields",
  "text",
  "title",
  "title_link",
  "image_url",
  "thumb_url",
  "http_config"
  })
public class SlackConfig {
  
  @JsonProperty("send_resolved")
  private Boolean sendResolved;
  @JsonProperty("api_url")
  private String apiUrl;
  @JsonProperty("channel")
  private String channel;
  @JsonProperty("icon_emoji")
  private String iconEmoji;
  @JsonProperty("icon_url")
  private String iconUrl;
  @JsonProperty("link_names")
  private Boolean linkNames;
  @JsonProperty("username")
  private String username;
  @JsonProperty("actions")
  private List<ActionConfig> actions;
  @JsonProperty("callback_id")
  private String callbackId;
  @JsonProperty("color")
  private String color;
  @JsonProperty("fallback")
  private String fallback;
  @JsonProperty("fields")
  private List<FieldConfig> fields;
  @JsonProperty("footer")
  private String footer;
  @JsonProperty("mrkdwn_in")
  private List<String> mrkdwnIn;
  @JsonProperty("short_fields")
  private Boolean shortFields;
  @JsonProperty("text")
  private String text;
  @JsonProperty("title")
  private String title;
  @JsonProperty("title_link")
  private String titleLink;
  @JsonProperty("image_url")
  private String imageUrl;
  @JsonProperty("thumb_url")
  private String thumbUrl;
  @JsonProperty("http_config")
  private HttpConfig httpConfig;
  
  public SlackConfig() {
  }
  
  public SlackConfig(String apiUrl, String channel) {
    this.apiUrl = apiUrl;
    this.channel = channel;
  }
  
  @JsonProperty("send_resolved")
  public Boolean getSendResolved() {
    return sendResolved;
  }
  
  @JsonProperty("send_resolved")
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }
  
  public SlackConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
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
  
  @JsonProperty("channel")
  public String getChannel() {
    return channel;
  }
  
  @JsonProperty("channel")
  public void setChannel(String channel) {
    this.channel = channel;
  }
  
  public SlackConfig withChannel(String channel) {
    this.channel = channel;
    return this;
  }
  
  @JsonProperty("icon_emoji")
  public String getIconEmoji() {
    return iconEmoji;
  }
  
  @JsonProperty("icon_emoji")
  public void setIconEmoji(String iconEmoji) {
    this.iconEmoji = iconEmoji;
  }
  
  public SlackConfig withIconEmoji(String iconEmoji) {
    this.iconEmoji = iconEmoji;
    return this;
  }
  
  @JsonProperty("icon_url")
  public String getIconUrl() {
    return iconUrl;
  }
  
  @JsonProperty("icon_url")
  public void setIconUrl(String iconUrl) {
    this.iconUrl = iconUrl;
  }
  
  public SlackConfig withIconUrl(String iconUrl) {
    this.iconUrl = iconUrl;
    return this;
  }
  
  @JsonProperty("link_names")
  public Boolean getLinkNames() {
    return linkNames;
  }
  
  @JsonProperty("link_names")
  public void setLinkNames(Boolean linkNames) {
    this.linkNames = linkNames;
  }
  
  public SlackConfig withLinkNames(Boolean linkNames) {
    this.linkNames = linkNames;
    return this;
  }
  
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }
  
  @JsonProperty("username")
  public void setUsername(String username) {
    this.username = username;
  }
  
  public SlackConfig withUsername(String username) {
    this.username = username;
    return this;
  }
  
  @JsonProperty("actions")
  public List<ActionConfig> getActions() {
    return actions;
  }
  
  @JsonProperty("actions")
  public void setActions(List<ActionConfig> actions) {
    this.actions = actions;
  }
  
  public SlackConfig withActions(List<ActionConfig> actions) {
    this.actions = actions;
    return this;
  }
  
  @JsonProperty("callback_id")
  public String getCallbackId() {
    return callbackId;
  }
  
  @JsonProperty("callback_id")
  public void setCallbackId(String callbackId) {
    this.callbackId = callbackId;
  }
  
  public SlackConfig withCallbackId(String callbackId) {
    this.callbackId = callbackId;
    return this;
  }
  
  @JsonProperty("color")
  public String getColor() {
    return color;
  }
  
  @JsonProperty("color")
  public void setColor(String color) {
    this.color = color;
  }
  
  public SlackConfig withColor(String color) {
    this.color = color;
    return this;
  }
  
  @JsonProperty("fallback")
  public String getFallback() {
    return fallback;
  }
  
  @JsonProperty("fallback")
  public void setFallback(String fallback) {
    this.fallback = fallback;
  }
  
  public SlackConfig withFallback(String fallback) {
    this.fallback = fallback;
    return this;
  }
  
  @JsonProperty("fields")
  public List<FieldConfig> getFields() {
    return fields;
  }
  
  @JsonProperty("fields")
  public void setFields(List<FieldConfig> fields) {
    this.fields = fields;
  }
  
  public SlackConfig withFields(List<FieldConfig> fields) {
    this.fields = fields;
    return this;
  }
  
  @JsonProperty("footer")
  public String getFooter() {
    return footer;
  }
  
  @JsonProperty("footer")
  public void setFooter(String footer) {
    this.footer = footer;
  }
  
  public SlackConfig withFooter(String footer) {
    this.footer = footer;
    return this;
  }
  
  @JsonProperty("mrkdwn_in")
  public List<String> getMrkdwnIn() {
    return mrkdwnIn;
  }
  
  @JsonProperty("mrkdwn_in")
  public void setMrkdwnIn(List<String> mrkdwnIn) {
    this.mrkdwnIn = mrkdwnIn;
  }
  
  public SlackConfig withMrkdwnIn(List<String> mrkdwnIn) {
    this.mrkdwnIn = mrkdwnIn;
    return this;
  }
  
  @JsonProperty("short_fields")
  public Boolean getShortFields() {
    return shortFields;
  }
  
  @JsonProperty("short_fields")
  public void setShortFields(Boolean shortFields) {
    this.shortFields = shortFields;
  }
  
  public SlackConfig withShortFields(Boolean shortFields) {
    this.shortFields = shortFields;
    return this;
  }
  
  @JsonProperty("text")
  public String getText() {
    return text;
  }
  
  @JsonProperty("text")
  public void setText(String text) {
    this.text = text;
  }
  
  public SlackConfig withText(String text) {
    this.text = text;
    return this;
  }
  
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }
  
  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }
  
  public SlackConfig withTitle(String title) {
    this.title = title;
    return this;
  }
  
  @JsonProperty("title_link")
  public String getTitleLink() {
    return titleLink;
  }
  
  @JsonProperty("title_link")
  public void setTitleLink(String titleLink) {
    this.titleLink = titleLink;
  }
  
  public SlackConfig withTitleLink(String titleLink) {
    this.titleLink = titleLink;
    return this;
  }
  
  @JsonProperty("image_url")
  public String getImageUrl() {
    return imageUrl;
  }
  
  @JsonProperty("image_url")
  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }
  
  public SlackConfig withImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
    return this;
  }
  
  @JsonProperty("thumb_url")
  public String getThumbUrl() {
    return thumbUrl;
  }
  
  @JsonProperty("thumb_url")
  public void setThumbUrl(String thumbUrl) {
    this.thumbUrl = thumbUrl;
  }
  
  public SlackConfig withThumbUrl(String thumbUrl) {
    this.thumbUrl = thumbUrl;
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
  
  public SlackConfig withHttpConfig(HttpConfig httpConfig) {
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
    SlackConfig that = (SlackConfig) o;
    return Objects.equals(apiUrl, that.apiUrl) && Objects.equals(channel, that.channel);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(apiUrl, channel);
  }
  
  @Override
  public String toString() {
    return "SlackConfig{" +
      "sendResolved=" + sendResolved +
      ", apiUrl='" + apiUrl + '\'' +
      ", channel='" + channel + '\'' +
      ", iconEmoji='" + iconEmoji + '\'' +
      ", iconUrl='" + iconUrl + '\'' +
      ", linkNames=" + linkNames +
      ", username='" + username + '\'' +
      ", actions=" + actions +
      ", callback_id='" + callbackId + '\'' +
      ", color='" + color + '\'' +
      ", fallback='" + fallback + '\'' +
      ", fields=" + fields +
      ", footer='" + footer + '\'' +
      ", mrkdwnIn=" + mrkdwnIn +
      ", shortFields=" + shortFields +
      ", text='" + text + '\'' +
      ", title='" + title + '\'' +
      ", titleLink='" + titleLink + '\'' +
      ", image_url='" + imageUrl + '\'' +
      ", thumb_url='" + thumbUrl + '\'' +
      ", httpConfig=" + httpConfig +
      '}';
  }
}
