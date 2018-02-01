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

package io.hops.hopsworks.api.admin.dto;

import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.project.QuotasDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@XmlRootElement
public class ProjectAdminInfoDTO implements Serializable {

  private static final long serialVersionUID = -1L;

  private int projectId = -1;
  private String projectName = "";
  private String projectOwner = "";
  private Boolean archived = null;
  private PaymentType paymentType = PaymentType.PREPAID;
  private Date lastQuotaUpdate = new Date();

  @XmlElement
  private QuotasDTO projectQuotas = null;

  public ProjectAdminInfoDTO() { }

  public ProjectAdminInfoDTO(Project project, QuotasDTO projectQuotas) {
    this.projectId = project.getId();
    this.projectName = project.getName();
    this.projectOwner = project.getOwner().getUsername();
    this.archived = project.getArchived();
    this.paymentType = project.getPaymentType();
    this.lastQuotaUpdate = project.getLastQuotaUpdate();
    this.projectQuotas = projectQuotas;
  }

  public int getProjectId() { return projectId; }

  public void setProjectId(int projectId) { this.projectId = projectId; }

  public String getProjectName() { return projectName; }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getProjectOwner() { return projectOwner; }

  public void setProjectOwner(String projectOwner) {
    this.projectOwner = projectOwner;
  }

  public Boolean getArchived() { return archived; }

  public PaymentType getPaymentType() { return paymentType; }

  public Date getLastQuotaUpdate() { return lastQuotaUpdate; }

  public void setPaymentType(PaymentType paymentType) {
    this.paymentType = paymentType;
  }

  public void setLastQuotaUpdate(Date lastQuotaUpdate) {
    this.lastQuotaUpdate = lastQuotaUpdate;
  }

  public QuotasDTO getProjectQuotas() { return projectQuotas; }

  public void setProjectQuotas(QuotasDTO projectQuotas) { this.projectQuotas = projectQuotas; }

  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  @Override
  public String toString() {
    return "ProjectAdminInfoDTO{" +
        "projectId=" + projectId +
        ", projectName='" + projectName + '\'' +
        ", projectOwner='" + projectOwner + '\'' +
        ", archived=" + archived +
        ", paymentType=" + paymentType +
        ", lastQuotaUpdate=" + lastQuotaUpdate +
        ", projectQuotas=" + projectQuotas +
        '}';
  }
}
