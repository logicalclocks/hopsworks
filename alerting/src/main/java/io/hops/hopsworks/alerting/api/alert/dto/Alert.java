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
package io.hops.hopsworks.alerting.api.alert.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Alert {

  private Map<String, String> labels;
  private Map<String, String> annotations;
  private List<ReceiverName> receivers;
  private AlertStatus status;
  private String fingerprint;
  private Date updatedAt;
  private Date startsAt;
  private Date endsAt;
  private URL generatorURL;

  public Alert() {
  }

  public Alert(Map<String, String> labels) {
    this.labels = labels;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public Map<String, String> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, String> annotations) {
    this.annotations = annotations;
  }

  public List<ReceiverName> getReceivers() {
    return receivers;
  }

  public void setReceivers(List<ReceiverName> receivers) {
    this.receivers = receivers;
  }

  public AlertStatus getStatus() {
    return status;
  }

  public void setStatus(AlertStatus status) {
    this.status = status;
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public void setFingerprint(String fingerprint) {
    this.fingerprint = fingerprint;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Date getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(Date startsAt) {
    this.startsAt = startsAt;
  }

  public Date getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(Date endsAt) {
    this.endsAt = endsAt;
  }

  public URL getGeneratorURL() {
    return generatorURL;
  }

  public void setGeneratorURL(URL generatorURL) {
    this.generatorURL = generatorURL;
  }

  @Override
  public String toString() {
    return "Alert{" +
        "labels=" + labels +
        ", annotations=" + annotations +
        ", receivers=" + receivers +
        ", status=" + status +
        ", fingerprint='" + fingerprint + '\'' +
        ", updatedAt=" + updatedAt +
        ", startsAt=" + startsAt +
        ", endsAt=" + endsAt +
        ", generatorURL=" + generatorURL +
        '}';
  }
}
