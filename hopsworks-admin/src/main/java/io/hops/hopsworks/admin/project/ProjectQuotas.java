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
package io.hops.hopsworks.admin.project;

import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.project.QuotasDTO;
import io.hops.hopsworks.common.util.HopsUtils;

import java.util.Date;

public class ProjectQuotas {

  private Project project = null;
  private String name = "";
  private int id = -1;
  private String owner = "";
  private String ownerEmail = "";
  private boolean archived = false;
  private PaymentType paymentType = null;
  private Date lastQuotaUpdate = null;

  // Quotas
  private String hdfsUsedQuota = "-1MB";
  private String hdfsQuota = "-1MB";
  private String hdfsNsQuota = "-1";
  private String hdfsUsedNsQuota = "-1";
  private String hiveHdfsQuota = "-1MB";
  private String hiveUsedHdfsQuota = "-1MB";
  private String hiveHdfsNsQuota = "-1";
  private String hiveUsedHdfsNsQuota = "-1";
  private String yarnQuota = "0:00:00:00";
  private String yarnTotalQuota = "0:00:00:00";
  private int kafkaMaxNumTopics = 0;

  public ProjectQuotas(Project project, QuotasDTO quotas) {
    this.project = project;
    this.name = project.getName();
    this.id = project.getId();
    this.owner = project.getOwner().getUsername();
    this.ownerEmail = project.getOwner().getEmail();
    this.lastQuotaUpdate = project.getLastQuotaUpdate();
    this.archived = project.getArchived();
    this.paymentType = project.getPaymentType();

    // Project Hdfs quota
    this.hdfsQuota = HopsUtils.spaceQuotaToString(quotas.getHdfsQuotaInBytes());
    this.hdfsUsedQuota = HopsUtils.spaceQuotaToString(quotas.getHdfsUsageInBytes());
    this.hdfsNsQuota = String.valueOf(quotas.getHdfsNsQuota());
    this.hdfsUsedNsQuota = String.valueOf(quotas.getHdfsNsCount());

    // Project Hive quota
    this.hiveHdfsQuota = HopsUtils.spaceQuotaToString(quotas.getHiveHdfsQuotaInBytes());
    this.hiveUsedHdfsQuota = HopsUtils.spaceQuotaToString(quotas.getHiveHdfsUsageInBytes());
    this.hiveHdfsNsQuota = String.valueOf(quotas.getHiveHdfsNsQuota());
    this.hiveUsedHdfsNsQuota = String.valueOf(quotas.getHiveHdfsNsCount());

    // Yarn quota
    this.yarnQuota = HopsUtils.procQuotaToString(quotas.getYarnQuotaInSecs());
    this.yarnTotalQuota = HopsUtils.procQuotaToString(quotas.getYarnUsedQuotaInSecs());

    // Kafka topics quota
    this.kafkaMaxNumTopics = project.getKafkaMaxNumTopics();
  }

  public QuotasDTO getNormalizedQuotas() {
    return new QuotasDTO(
        HopsUtils.spaceQuotaToLong(hdfsQuota),
        HopsUtils.spaceQuotaToLong(hdfsNsQuota),
        HopsUtils.spaceQuotaToLong(hiveHdfsQuota),
        HopsUtils.spaceQuotaToLong(hiveHdfsNsQuota),
        HopsUtils.procQuotaToFloat(yarnQuota),
        this.kafkaMaxNumTopics
    );
  }

  public Project getProject() {
    return project;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public boolean isArchived() {
    return archived;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
    project.setArchived(archived);
  }

  public PaymentType getPaymentType() {
    return paymentType;
  }

  public void setPaymentType(PaymentType paymentType) {
    this.paymentType = paymentType;
    if (project != null) {
      project.setPaymentType(paymentType);
    }
  }

  public Date getLastQuotaUpdate() {
    return lastQuotaUpdate;
  }

  public void setLastQuotaUpdate(Date lastQuotaUpdate) {
    this.lastQuotaUpdate = lastQuotaUpdate;
  }

  public String getHdfsQuota() {
    return hdfsQuota;
  }

  public void setHdfsQuota(String hdfsQuota) {
    this.hdfsQuota = hdfsQuota;
  }

  public String getHdfsUsedQuota() {
    return hdfsUsedQuota;
  }

  public void setHdfsUsedQuota(String hdfsUsedQuota) {
    this.hdfsUsedQuota = hdfsUsedQuota;
  }

  public String getHdfsNsQuota() {
    return hdfsNsQuota;
  }

  public void setHdfsNsQuota(String hdfsNsQuota) {
    this.hdfsNsQuota = hdfsNsQuota;
  }

  public String getHiveHdfsNsQuota() {
    return hiveHdfsNsQuota;
  }

  public void setHiveHdfsNsQuota(String hiveHdfsNsQuota) {
    this.hiveHdfsNsQuota = hiveHdfsNsQuota;
  }

  public String getYarnQuota() {
    return yarnQuota;
  }

  public void setYarnQuota(String yarnQuota) {
    this.yarnQuota = yarnQuota;
  }

  public String getYarnTotalQuota() {
    return yarnTotalQuota;
  }

  public void setYarnTotalQuota(String yarnTotalQuota) {
    this.yarnTotalQuota = yarnTotalQuota;
  }

  public String getHdfsUsedNsQuota() {
    return hdfsUsedNsQuota;
  }

  public void setHdfsUsedNsQuota(String hdfsUsedNsQuota) {
    this.hdfsUsedNsQuota = hdfsUsedNsQuota;
  }

  public String getHiveHdfsQuota() {
    return hiveHdfsQuota;
  }

  public void setHiveHdfsQuota(String hiveHdfsQuota) {
    this.hiveHdfsQuota = hiveHdfsQuota;
  }

  public String getHiveUsedHdfsQuota() {
    return hiveUsedHdfsQuota;
  }

  public void setHiveUsedHdfsQuota(String hiveUsedHdfsQuota) {
    this.hiveUsedHdfsQuota = hiveUsedHdfsQuota;
  }

  public String getHiveUsedHdfsNsQuota() {
    return hiveUsedHdfsNsQuota;
  }

  public void setHiveUsedHdfsNsQuota(String hiveUsedHdfsNsQuota) {
    this.hiveUsedHdfsNsQuota = hiveUsedHdfsNsQuota;
  }

  public int getId() {
    return id;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public int getKafkaMaxNumTopics() {
    return kafkaMaxNumTopics;
  }

  public void setKafkaMaxNumTopics(int kafkaMaxNumTopics) {
    this.kafkaMaxNumTopics = kafkaMaxNumTopics;
  }

}
