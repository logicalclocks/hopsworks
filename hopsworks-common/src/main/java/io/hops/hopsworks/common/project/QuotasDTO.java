/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
  private Float yarnQuotaInSecs = null;
  private Float yarnUsedQuotaInSecs = null;

  public QuotasDTO() {
  }

  public QuotasDTO(Float yarnQuotaInSecs, Float yarnUsedQuotaInSecs,
                   Long hdfsQuotaInBytes, Long hdfsUsageInBytes,
                   Long hdfsNsQuota, Long hdfsNsCount,
                   Long hiveHdfsQuotaInBytes, Long hiveHdfsUsageInBytes,
                   Long hiveHdfsNsQuota, Long hiveHdfsNsCount) {
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
  }

  public QuotasDTO(Long hdfsQuotaInBytes, Long hdfsNsQuota,
                   Long hiveHdfsQuotaInBytes, Long hiveHdfsNsQuota, Float yarnQuotaInSecs) {
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
    this.hdfsNsQuota = hdfsNsQuota;
    this.hiveHdfsQuotaInBytes = hiveHdfsQuotaInBytes;
    this.hiveHdfsNsQuota = hiveHdfsNsQuota;
    this.yarnQuotaInSecs = yarnQuotaInSecs;
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
        ", yarnQuotaInSecs=" + yarnQuotaInSecs +
        ", yarnUsedQuotaInSecs =" + yarnUsedQuotaInSecs +
        '}';
  }
}
