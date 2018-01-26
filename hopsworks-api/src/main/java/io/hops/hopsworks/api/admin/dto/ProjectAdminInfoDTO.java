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
