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
  private Long hdfsNsCount;
  private Long hdfsNsQuota;
  private String yarnQuotaInMins;

  public QuotasDTO() {
  }

  public QuotasDTO(String yarnQuotaInMins,
      Long hdfsQuotaInBytes, Long hdfsUsageInBytes,
       Long hdfsNsQuota, Long hdfsNsCount
  ) {
    this.yarnQuotaInMins = yarnQuotaInMins;
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
    this.hdfsUsageInBytes = hdfsUsageInBytes;
    this.hdfsNsQuota = hdfsNsQuota;
    this.hdfsNsCount = hdfsNsCount;
  }

  public Long getHdfsQuotaInBytes() {
    return hdfsQuotaInBytes;
  }

  public String getYarnQuotaInMins() {
    return yarnQuotaInMins;
  }

  public void setHdfsQuotaInBytes(Long hdfsQuotaInBytes) {
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
  }

  public void setYarnQuotaInMins(String yarnQuotaInMins) {
    this.yarnQuotaInMins = yarnQuotaInMins;
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

  @Override
  public String toString() {
    return "QuotasDTO{"
        + "hdfsUsage=" + hdfsUsageInBytes + ", hdfsQuota=" + hdfsQuotaInBytes
        + "hdfsNsUsage=" + hdfsNsCount + ", hdfsNsQuota=" + hdfsNsQuota
        + ", yarnQuota=" + yarnQuotaInMins + '}';
  }

}
