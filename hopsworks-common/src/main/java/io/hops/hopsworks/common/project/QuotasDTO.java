/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.project;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class QuotasDTO implements Serializable {

  private static final long serialVersionUID = -1L;

  private Long hdfsUsageInBytes = null;
  private Long hdfsQuotaInBytes = null;
  private Long hdfsNsCount = null;
  private Long hdfsNsQuota = null;
  private Long hiveHdfsUsageInBytes = null;
  private Long hiveHdfsQuotaInBytes = null;
  private Long hiveHdfsNsCount = null;
  private Long hiveHdfsNsQuota = null;
  private Long featurestoreHdfsUsageInBytes = null;
  private Long featurestoreHdfsQuotaInBytes = null;
  private Long featurestoreHdfsNsCount = null;
  private Long featurestoreHdfsNsQuota = null;
  private Float yarnQuotaInSecs = null;
  private Float yarnUsedQuotaInSecs = null;
  private Integer kafkaMaxNumTopics = null;

  public QuotasDTO() {
  }

  public QuotasDTO(Float yarnQuotaInSecs, Float yarnUsedQuotaInSecs,
                   Long hdfsQuotaInBytes, Long hdfsUsageInBytes,
                   Long hdfsNsQuota, Long hdfsNsCount,
                   Long hiveHdfsQuotaInBytes, Long hiveHdfsUsageInBytes,
                   Long hiveHdfsNsQuota, Long hiveHdfsNsCount,
                   Long featurestoreHdfsQuotaInBytes, Long featurestoreHdfsUsageInBytes,
                   Long featurestoreHdfsNsQuota, Long featurestoreHdfsNsCount,
                   Integer kafkaMaxNumTopics) {
    this.yarnQuotaInSecs = yarnQuotaInSecs;
    this.yarnUsedQuotaInSecs = yarnUsedQuotaInSecs;
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
    this.hdfsUsageInBytes = hdfsUsageInBytes;
    this.hdfsNsQuota = hdfsNsQuota;
    this.hdfsNsCount = hdfsNsCount;
    this.hiveHdfsQuotaInBytes = hiveHdfsQuotaInBytes;
    this.hiveHdfsUsageInBytes = hiveHdfsUsageInBytes;
    this.hiveHdfsNsCount = hiveHdfsNsCount;
    this.hiveHdfsNsQuota = hiveHdfsNsQuota;
    this.featurestoreHdfsQuotaInBytes = featurestoreHdfsQuotaInBytes;
    this.featurestoreHdfsUsageInBytes = featurestoreHdfsUsageInBytes;
    this.featurestoreHdfsNsCount = featurestoreHdfsNsCount;
    this.featurestoreHdfsNsQuota = featurestoreHdfsNsQuota;
    this.kafkaMaxNumTopics = kafkaMaxNumTopics;
  }

  public QuotasDTO(Long hdfsQuotaInBytes, Long hdfsNsQuota,
                   Long hiveHdfsQuotaInBytes, Long hiveHdfsNsQuota,
                   Long featurestoreHdfsQuotaInBytes, Long featurestoreHdfsNsQuota,
                   Float yarnQuotaInSecs, Integer numKafkaTopics) {
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
    this.hdfsNsQuota = hdfsNsQuota;
    this.hiveHdfsQuotaInBytes = hiveHdfsQuotaInBytes;
    this.hiveHdfsNsQuota = hiveHdfsNsQuota;
    this.featurestoreHdfsQuotaInBytes = featurestoreHdfsQuotaInBytes;
    this.featurestoreHdfsNsQuota = featurestoreHdfsNsQuota;
    this.yarnQuotaInSecs = yarnQuotaInSecs;
    this.kafkaMaxNumTopics = numKafkaTopics;
  }

  public Long getHdfsQuotaInBytes() {
    return hdfsQuotaInBytes;
  }

  public Float getYarnQuotaInSecs() {
    return yarnQuotaInSecs;
  }

  public void setHdfsQuotaInBytes(Long hdfsQuotaInBytes) {
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
  }

  public void setYarnQuotaInSecs(Float yarnQuotaInSecs) {
    this.yarnQuotaInSecs = yarnQuotaInSecs;
  }

  public Long getHdfsUsageInBytes() {
    return hdfsUsageInBytes;
  }

  public void setHdfsUsageInBytes(Long hdfsUsageInBytes) {
    this.hdfsUsageInBytes = hdfsUsageInBytes;
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

  public Long getHiveHdfsUsageInBytes() {
    return hiveHdfsUsageInBytes;
  }

  public void setHiveHdfsUsageInBytes(Long hiveHdfsUsageInBytes) {
    this.hiveHdfsUsageInBytes = hiveHdfsUsageInBytes;
  }

  public Long getHiveHdfsQuotaInBytes() {
    return hiveHdfsQuotaInBytes;
  }

  public void setHiveHdfsQuotaInBytes(Long hiveHdfsQuotaInBytes) {
    this.hiveHdfsQuotaInBytes = hiveHdfsQuotaInBytes;
  }

  public Long getHiveHdfsNsCount() {
    return hiveHdfsNsCount;
  }

  public void setHiveHdfsNsCount(Long hiveHdfsNsCount) {
    this.hiveHdfsNsCount = hiveHdfsNsCount;
  }

  public Long getHiveHdfsNsQuota() {
    return hiveHdfsNsQuota;
  }

  public void setHiveHdfsNsQuota(Long hiveHdfsNsQuota) {
    this.hiveHdfsNsQuota = hiveHdfsNsQuota;
  }

  public Float getYarnUsedQuotaInSecs() {
    return yarnUsedQuotaInSecs;
  }

  public void setYarnUsedQuotaInSecs(Float yarnUsedQuotaInSecs) {
    this.yarnUsedQuotaInSecs = yarnUsedQuotaInSecs;
  }

  public Integer getKafkaMaxNumTopics() {
    return kafkaMaxNumTopics;
  }

  public void setKafkaMaxNumTopics(Integer kafkaMaxNumTopics) {
    this.kafkaMaxNumTopics = kafkaMaxNumTopics;
  }

  public Long getFeaturestoreHdfsUsageInBytes() {
    return featurestoreHdfsUsageInBytes;
  }

  public void setFeaturestoreHdfsUsageInBytes(Long featurestoreHdfsUsageInBytes) {
    this.featurestoreHdfsUsageInBytes = featurestoreHdfsUsageInBytes;
  }

  public Long getFeaturestoreHdfsQuotaInBytes() {
    return featurestoreHdfsQuotaInBytes;
  }

  public void setFeaturestoreHdfsQuotaInBytes(Long featurestoreHdfsQuotaInBytes) {
    this.featurestoreHdfsQuotaInBytes = featurestoreHdfsQuotaInBytes;
  }

  public Long getFeaturestoreHdfsNsCount() {
    return featurestoreHdfsNsCount;
  }

  public void setFeaturestoreHdfsNsCount(Long featurestoreHdfsNsCount) {
    this.featurestoreHdfsNsCount = featurestoreHdfsNsCount;
  }

  public Long getFeaturestoreHdfsNsQuota() {
    return featurestoreHdfsNsQuota;
  }

  public void setFeaturestoreHdfsNsQuota(Long featurestoreHdfsNsQuota) {
    this.featurestoreHdfsNsQuota = featurestoreHdfsNsQuota;
  }

  @Override
  public String toString() {
    return "QuotasDTO{" +
        "hdfsUsageInBytes=" + hdfsUsageInBytes +
        ", hdfsQuotaInBytes=" + hdfsQuotaInBytes +
        ", hdfsNsCount=" + hdfsNsCount +
        ", hdfsNsQuota=" + hdfsNsQuota +
        ", hiveHdfsUsageInBytes=" + hiveHdfsUsageInBytes +
        ", hiveHdfsQuotaInBytes=" + hiveHdfsQuotaInBytes +
        ", hiveHdfsNsCount=" + hiveHdfsNsCount +
        ", hiveHdfsNsQuota=" + hiveHdfsNsQuota +
        ", featurestoreHdfsUsageInBytes=" + featurestoreHdfsUsageInBytes +
        ", featurestoreHdfsQuotaInBytes=" + featurestoreHdfsQuotaInBytes +
        ", featurestoreHdfsNsCount=" + featurestoreHdfsNsCount +
        ", featurestoreHdfsNsQuota=" + featurestoreHdfsNsQuota +
        ", yarnQuotaInSecs=" + yarnQuotaInSecs +
        ", yarnUsedQuotaInSecs=" + yarnUsedQuotaInSecs +
        ", kafkaMaxNumTopics=" + kafkaMaxNumTopics +
        '}';
  }
}
