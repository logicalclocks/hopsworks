package io.hops.hopsworks.common.project;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class QuotasDTO {

  private Long hdfsUsageInBytes;
  private Long hdfsQuotaInBytes;
  private Long hdfsNsCount;
  private Long hdfsNsQuota;
  private String yarnQuotaInSecs;

  public QuotasDTO() {
  }

  public QuotasDTO(String yarnQuotaInMins,
      Long hdfsQuotaInBytes, Long hdfsUsageInBytes,
      Long hdfsNsQuota, Long hdfsNsCount) {
    this.yarnQuotaInSecs = yarnQuotaInMins;
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
    this.hdfsUsageInBytes = hdfsUsageInBytes;
    this.hdfsNsQuota = hdfsNsQuota;
    this.hdfsNsCount = hdfsNsCount;
  }

  public Long getHdfsQuotaInBytes() {
    return hdfsQuotaInBytes;
  }

  public String getYarnQuotaInSecs() {
    return yarnQuotaInSecs;
  }

  public void setHdfsQuotaInBytes(Long hdfsQuotaInBytes) {
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
  }

  public void setYarnQuotaInSecs(String yarnQuotaInSecs) {
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

  @Override
  public String toString() {
    return "QuotasDTO{"
        + "hdfsUsage=" + hdfsUsageInBytes + ", hdfsQuota="
        + hdfsQuotaInBytes
        + "hdfsNsUsage=" + hdfsNsCount + ", hdfsNsQuota=" + hdfsNsQuota
        + ", yarnQuota=" + yarnQuotaInSecs + '}';
  }

}
