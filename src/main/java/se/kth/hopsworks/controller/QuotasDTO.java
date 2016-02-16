package se.kth.hopsworks.controller;

import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.fb.InodeView;

/**
 *
 * @author Ermias
 */
@XmlRootElement
public class QuotasDTO {

  private Long hdfsUsageInBytes;
  private Long hdfsQuotaInBytes;
  private Integer yarnQuotaInMins;

  public QuotasDTO() {
  }

  public QuotasDTO(Integer yarnQuotaInMins,
          Long hdfsQuotaInBytes, Long hdfsUsageInBytes) {
    this.yarnQuotaInMins = yarnQuotaInMins;
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;    
    this.hdfsUsageInBytes = hdfsUsageInBytes;
  }

  public Long getHdfsQuotaInBytes() {
    return hdfsQuotaInBytes;
  }

  public Integer getYarnQuotaInMins() {
    return yarnQuotaInMins;
  }

  public void setHdfsQuotaInBytes(Long hdfsQuotaInBytes) {
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
  }

  public void setYarnQuotaInMins(Integer yarnQuotaInMins) {
    this.yarnQuotaInMins = yarnQuotaInMins;
  }

  public Long getHdfsUsageInBytes() {
    return hdfsUsageInBytes;
  }

  public void setHdfsUsageInBytes(Long hdfsUsageInBytes) {
    this.hdfsUsageInBytes = hdfsUsageInBytes;
  }
  
  
  @Override
  public String toString() {
    return "QuotasDTO{" + "hdfsUsage=" + hdfsUsageInBytes + ", hdfsQuota=" + hdfsQuotaInBytes
            + ", yarnQuota=" + yarnQuotaInMins + '}';
  }

}
