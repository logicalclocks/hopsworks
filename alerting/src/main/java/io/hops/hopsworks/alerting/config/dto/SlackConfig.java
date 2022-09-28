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
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlackConfig {

  @JsonAlias({"send_resolved"})
  private Boolean sendResolved;
  @JsonAlias({"api_url"})
  private String apiUrl;
  private String channel;
  @JsonAlias({"icon_emoji"})
  private String iconEmoji;
  @JsonAlias({"icon_url"})
  private String iconUrl;
  @JsonAlias({"link_names"})
  private Boolean linkNames;
  private String username;
  private List<ActionConfig> actions;
  @JsonAlias({"callback_id"})
  private String callbackId;
  private String color;
  private String fallback;
  private List<FieldConfig> fields;
  private String footer;
  @JsonAlias({"mrkdwn_in"})
  private List<String> mrkdwnIn;
  @JsonAlias({"short_fields"})
  private Boolean shortFields;
  private String text;
  private String title;
  @JsonAlias({"title_link"})
  private String titleLink;
  @JsonAlias({"image_url"})
  private String imageUrl;
  @JsonAlias({"thumb_url"})
  private String thumbUrl;
  @JsonAlias({"http_config"})
  private HttpConfig httpConfig;

  public SlackConfig() {
  }

  public SlackConfig(String apiUrl, String channel) {
    this.apiUrl = apiUrl;
    this.channel = channel;
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public SlackConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public SlackConfig withChannel(String channel) {
    this.channel = channel;
    return this;
  }

  public String getIconEmoji() {
    return iconEmoji;
  }

  public void setIconEmoji(String iconEmoji) {
    this.iconEmoji = iconEmoji;
  }

  public SlackConfig withIconEmoji(String iconEmoji) {
    this.iconEmoji = iconEmoji;
    return this;
  }

  public String getIconUrl() {
    return iconUrl;
  }

  public void setIconUrl(String iconUrl) {
    this.iconUrl = iconUrl;
  }

  public SlackConfig withIconUrl(String iconUrl) {
    this.iconUrl = iconUrl;
    return this;
  }

  public Boolean getLinkNames() {
    return linkNames;
  }

  public void setLinkNames(Boolean linkNames) {
    this.linkNames = linkNames;
  }

  public SlackConfig withLinkNames(Boolean linkNames) {
    this.linkNames = linkNames;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public SlackConfig withUsername(String username) {
    this.username = username;
    return this;
  }

  public List<ActionConfig> getActions() {
    return actions;
  }

  public void setActions(List<ActionConfig> actions) {
    this.actions = actions;
  }

  public SlackConfig withActions(List<ActionConfig> actions) {
    this.actions = actions;
    return this;
  }

  public String getCallbackId() {
    return callbackId;
  }

  public void setCallbackId(String callbackId) {
    this.callbackId = callbackId;
  }

  public SlackConfig withCallbackId(String callbackId) {
    this.callbackId = callbackId;
    return this;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public SlackConfig withColor(String color) {
    this.color = color;
    return this;
  }

  public String getFallback() {
    return fallback;
  }

  public void setFallback(String fallback) {
    this.fallback = fallback;
  }

  public SlackConfig withFallback(String fallback) {
    this.fallback = fallback;
    return this;
  }

  public List<FieldConfig> getFields() {
    return fields;
  }

  public void setFields(List<FieldConfig> fields) {
    this.fields = fields;
  }

  public SlackConfig withFields(List<FieldConfig> fields) {
    this.fields = fields;
    return this;
  }

  public String getFooter() {
    return footer;
  }

  public void setFooter(String footer) {
    this.footer = footer;
  }

  public SlackConfig withFooter(String footer) {
    this.footer = footer;
    return this;
  }

  public List<String> getMrkdwnIn() {
    return mrkdwnIn;
  }

  public void setMrkdwnIn(List<String> mrkdwnIn) {
    this.mrkdwnIn = mrkdwnIn;
  }

  public SlackConfig withMrkdwnIn(List<String> mrkdwnIn) {
    this.mrkdwnIn = mrkdwnIn;
    return this;
  }

  public Boolean getShortFields() {
    return shortFields;
  }

  public void setShortFields(Boolean shortFields) {
    this.shortFields = shortFields;
  }

  public SlackConfig withShortFields(Boolean shortFields) {
    this.shortFields = shortFields;
    return this;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public SlackConfig withText(String text) {
    this.text = text;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public SlackConfig withTitle(String title) {
    this.title = title;
    return this;
  }

  public String getTitleLink() {
    return titleLink;
  }

  public void setTitleLink(String titleLink) {
    this.titleLink = titleLink;
  }

  public SlackConfig withTitleLink(String titleLink) {
    this.titleLink = titleLink;
    return this;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public SlackConfig withImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
    return this;
  }

  public String getThumbUrl() {
    return thumbUrl;
  }

  public void setThumbUrl(String thumbUrl) {
    this.thumbUrl = thumbUrl;
  }

  public SlackConfig withThumbUrl(String thumbUrl) {
    this.thumbUrl = thumbUrl;
    return this;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

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
