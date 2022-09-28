/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alerting.config.dto.ActionConfig;
import io.hops.hopsworks.alerting.config.dto.FieldConfig;
import io.hops.hopsworks.alerting.config.dto.HttpConfig;
import io.hops.hopsworks.alerting.config.dto.SlackConfig;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class PostableSlackConfig {
  private Boolean sendResolved;
  private String apiUrl;
  private String channel;
  private String iconEmoji;
  private String iconUrl;
  private Boolean linkNames;
  private String username;
  private List<ActionConfig> actions;
  private String callbackId;
  private String color;
  private String fallback;
  private List<FieldConfig> fields;
  private String footer;
  private List<String> mrkdwnIn;
  private Boolean shortFields;
  private String text;
  private String title;
  private String titleLink;
  private String imageUrl;
  private String thumbUrl;
  private HttpConfig httpConfig;

  public PostableSlackConfig() {
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
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

  public String getIconEmoji() {
    return iconEmoji;
  }

  public void setIconEmoji(String iconEmoji) {
    this.iconEmoji = iconEmoji;
  }

  public String getIconUrl() {
    return iconUrl;
  }

  public void setIconUrl(String iconUrl) {
    this.iconUrl = iconUrl;
  }

  public Boolean getLinkNames() {
    return linkNames;
  }

  public void setLinkNames(Boolean linkNames) {
    this.linkNames = linkNames;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public List<ActionConfig> getActions() {
    return actions;
  }

  public void setActions(List<ActionConfig> actions) {
    this.actions = actions;
  }

  public String getCallbackId() {
    return callbackId;
  }

  public void setCallbackId(String callbackId) {
    this.callbackId = callbackId;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getFallback() {
    return fallback;
  }

  public void setFallback(String fallback) {
    this.fallback = fallback;
  }

  public List<FieldConfig> getFields() {
    return fields;
  }

  public void setFields(List<FieldConfig> fields) {
    this.fields = fields;
  }

  public String getFooter() {
    return footer;
  }

  public void setFooter(String footer) {
    this.footer = footer;
  }

  public List<String> getMrkdwnIn() {
    return mrkdwnIn;
  }

  public void setMrkdwnIn(List<String> mrkdwnIn) {
    this.mrkdwnIn = mrkdwnIn;
  }

  public Boolean getShortFields() {
    return shortFields;
  }

  public void setShortFields(Boolean shortFields) {
    this.shortFields = shortFields;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitleLink() {
    return titleLink;
  }

  public void setTitleLink(String titleLink) {
    this.titleLink = titleLink;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getThumbUrl() {
    return thumbUrl;
  }

  public void setThumbUrl(String thumbUrl) {
    this.thumbUrl = thumbUrl;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public SlackConfig toSlackConfig(Boolean defaultTemplate) {
    SlackConfig slackConfig = new SlackConfig(this.apiUrl, this.channel);
    slackConfig.withSendResolved(this.sendResolved)
      .withIconEmoji(this.iconEmoji)
      .withIconUrl(this.iconUrl)
      .withLinkNames(this.linkNames)
      .withUsername(this.username)
      .withActions(this.actions)
      .withCallbackId(this.callbackId)
      .withColor(this.color)
      .withFallback(this.fallback)
      .withFields(this.fields)
      .withFooter(this.footer)
      .withMrkdwnIn(this.mrkdwnIn)
      .withShortFields(this.shortFields)
      .withText(this.text)
      .withTitle(this.title)
      .withTitleLink(this.titleLink)
      .withImageUrl(this.imageUrl)
      .withThumbUrl(this.thumbUrl)
      .withHttpConfig(this.httpConfig);

    if (defaultTemplate) {
      if (Strings.isNullOrEmpty(slackConfig.getIconUrl())) {
        slackConfig.setIconUrl(Constants.DEFAULT_SLACK_ICON_URL);
      }
      if (Strings.isNullOrEmpty(slackConfig.getText())) {
        slackConfig.setText(Constants.DEFAULT_SLACK_TEXT);
      }
      if (Strings.isNullOrEmpty(slackConfig.getTitle())) {
        slackConfig.setTitle(Constants.DEFAULT_SLACK_TITLE);
      }
    }
    return slackConfig;
  }

  @Override
  public String toString() {
    return "PostableSlackConfig{" +
      "sendResolved=" + sendResolved +
      ", channel='" + channel + '\'' +
      ", iconEmoji='" + iconEmoji + '\'' +
      ", iconUrl='" + iconUrl + '\'' +
      ", linkNames=" + linkNames +
      ", username='" + username + '\'' +
      ", actions=" + actions +
      ", callbackId='" + callbackId + '\'' +
      ", color='" + color + '\'' +
      ", fallback='" + fallback + '\'' +
      ", fields=" + fields +
      ", footer='" + footer + '\'' +
      ", mrkdwnIn=" + mrkdwnIn +
      ", shortFields=" + shortFields +
      ", text='" + text + '\'' +
      ", title='" + title + '\'' +
      ", titleLink='" + titleLink + '\'' +
      ", imageUrl='" + imageUrl + '\'' +
      ", thumbUrl='" + thumbUrl + '\'' +
      ", httpConfig=" + httpConfig +
      '}';
  }
}
