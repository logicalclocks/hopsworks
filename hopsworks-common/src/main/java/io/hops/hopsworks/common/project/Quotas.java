/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.project;

import java.io.Serializable;

public class Quotas implements Serializable {

  private static final long serialVersionUID = -1L;

  private Float hdfsUsage = null;
  private Float hdfsQuota = null;
  private Long hdfsNsCount = null;
  private Long hdfsNsQuota = null;
  private Float hiveUsage = null;
  private Float hiveQuota = null;
  private Long hiveNsCount = null;
  private Long hiveNsQuota = null;
  private Float featurestoreUsage = null;
  private Float featurestoreQuota = null;
  private Long featurestoreNsCount = null;
  private Long featurestoreNsQuota = null;
  private Float yarnQuotaInSecs = null;
  private Float yarnUsedQuotaInSecs = null;
  private Integer kafkaNumTopics = null;
  private Integer kafkaMaxNumTopics = null;

  public Quotas() {
  }

  public Float getYarnQuotaInSecs() {
    return yarnQuotaInSecs;
  }

  public void setYarnQuotaInSecs(Float yarnQuotaInSecs) {
    this.yarnQuotaInSecs = yarnQuotaInSecs;
  }

  public Float getHdfsUsage() {
    return hdfsUsage;
  }

  public void setHdfsUsage(Float hdfsUsage) {
    this.hdfsUsage = hdfsUsage;
  }

  public Float getHdfsQuota() {
    return hdfsQuota;
  }

  public void setHdfsQuota(Float hdfsQuota) {
    this.hdfsQuota = hdfsQuota;
  }

  public Float getHiveUsage() {
    return hiveUsage;
  }

  public void setHiveUsage(Float hiveUsage) {
    this.hiveUsage = hiveUsage;
  }

  public Float getHiveQuota() {
    return hiveQuota;
  }

  public void setHiveQuota(Float hiveQuota) {
    this.hiveQuota = hiveQuota;
  }

  public Float getFeaturestoreUsage() {
    return featurestoreUsage;
  }

  public void setFeaturestoreUsage(Float featurestoreUsage) {
    this.featurestoreUsage = featurestoreUsage;
  }

  public Float getFeaturestoreQuota() {
    return featurestoreQuota;
  }

  public void setFeaturestoreQuota(Float featurestoreQuota) {
    this.featurestoreQuota = featurestoreQuota;
  }

  public Long getHdfsNsQuota() {
    return hdfsNsQuota;
  }

  public void setHdfsNsQuota(Long hdfsNsQuota) {
    this.hdfsNsQuota = hdfsNsQuota;
  }

  public Long getHdfsNsCount() {
    return hdfsNsCount;
  }

  public void setHdfsNsCount(Long hdfsNsCount) {
    this.hdfsNsCount = hdfsNsCount;
  }

  public Long getHiveNsCount() {
    return hiveNsCount;
  }

  public void setHiveNsCount(Long hiveNsCount) {
    this.hiveNsCount = hiveNsCount;
  }

  public Long getHiveNsQuota() {
    return hiveNsQuota;
  }

  public void setHiveNsQuota(Long hiveNsQuota) {
    this.hiveNsQuota = hiveNsQuota;
  }

  public Float getYarnUsedQuotaInSecs() {
    return yarnUsedQuotaInSecs;
  }

  public void setYarnUsedQuotaInSecs(Float yarnUsedQuotaInSecs) {
    this.yarnUsedQuotaInSecs = yarnUsedQuotaInSecs;
  }

  public Integer getKafkaNumTopics() {
    return kafkaNumTopics;
  }

  public void setKafkaNumTopics(Integer kafkaNumTopics) {
    this.kafkaNumTopics = kafkaNumTopics;
  }

  public Integer getKafkaMaxNumTopics() {
    return kafkaMaxNumTopics;
  }

  public void setKafkaMaxNumTopics(Integer kafkaMaxNumTopics) {
    this.kafkaMaxNumTopics = kafkaMaxNumTopics;
  }

  public Long getFeaturestoreNsCount() {
    return featurestoreNsCount;
  }

  public void setFeaturestoreNsCount(Long featurestoreNsCount) {
    this.featurestoreNsCount = featurestoreNsCount;
  }

  public Long getFeaturestoreNsQuota() {
    return featurestoreNsQuota;
  }

  public void setFeaturestoreNsQuota(Long featurestoreNsQuota) {
    this.featurestoreNsQuota = featurestoreNsQuota;
  }

  @Override
  public String toString() {
    return "Quotas{" +
        "hdfsUsage=" + hdfsUsage +
        ", hdfsQuota=" + hdfsQuota +
        ", hdfsNsCount=" + hdfsNsCount +
        ", hdfsNsQuota=" + hdfsNsQuota +
        ", hiveUsage=" + hiveUsage +
        ", hiveQuota=" + hiveQuota +
        ", hiveNsCount=" + hiveNsCount +
        ", hiveNsQuota=" + hiveNsQuota +
        ", featurestoreUsage=" + featurestoreUsage +
        ", featurestoreQuota=" + featurestoreQuota +
        ", featurestoreNsCount=" + featurestoreNsCount +
        ", featurestoreNsQuota=" + featurestoreNsQuota +
        ", yarnQuotaInSecs=" + yarnQuotaInSecs +
        ", yarnUsedQuotaInSecs=" + yarnUsedQuotaInSecs +
        ", kafkaNumTopics=" + kafkaNumTopics +
        ", kafkaMaxNumTopics=" + kafkaMaxNumTopics +
        '}';
  }
}
