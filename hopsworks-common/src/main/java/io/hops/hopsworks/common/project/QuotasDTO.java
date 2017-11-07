package io.hops.hopsworks.common.project;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class QuotasDTO {

  private Long hdfsUsageInBytes;
  private Long hdfsQuotaInBytes;
  private Long hdfsNsCount;
  private Long hdfsNsQuota;
  private Long hiveHdfsUsageInBytes;
  private Long hiveHdfsQuotaInBytes;
  private Long hiveHdfsNsCount;
  private Long hiveHdfsNsQuota;
  private String yarnQuotaInSecs;

  public QuotasDTO() {
  }

  public QuotasDTO(String yarnQuotaInMins,
      Long hdfsQuotaInBytes, Long hdfsUsageInBytes,
      Long hdfsNsQuota, Long hdfsNsCount,
      Long hiveHdfsQuotaInBytes, Long hiveHdfsUsageInBytes,
      Long hiveHdfsNsQuota, Long hiveHdfsNsCount) {
    this.yarnQuotaInSecs = yarnQuotaInMins;
    this.hdfsQuotaInBytes = hdfsQuotaInBytes;
    this.hdfsUsageInBytes = hdfsUsageInBytes;
    this.hdfsNsQuota = hdfsNsQuota;
    this.hdfsNsCount = hdfsNsCount;
    this.hiveHdfsQuotaInBytes = hiveHdfsQuotaInBytes;
    this.hiveHdfsUsageInBytes = hiveHdfsUsageInBytes;
    this.hiveHdfsNsCount = hiveHdfsNsCount;
    this.hiveHdfsNsQuota = hiveHdfsNsQuota;
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

  public Long getHiveHdfsUsageInBytes() { return hiveHdfsUsageInBytes; }

  public void setHiveHdfsUsageInBytes(Long hiveHdfsUsageInBytes) {
    this.hiveHdfsUsageInBytes = hiveHdfsUsageInBytes;
  }

  public Long getHiveHdfsQuotaInBytes() { return hiveHdfsQuotaInBytes; }

  public void setHiveHdfsQuotaInBytes(Long hiveHdfsQuotaInBytes) {
    this.hiveHdfsQuotaInBytes = hiveHdfsQuotaInBytes;
  }

  public Long getHiveHdfsNsCount() { return hiveHdfsNsCount; }

  public void setHiveHdfsNsCount(Long hiveHdfsNsCount) {
    this.hiveHdfsNsCount = hiveHdfsNsCount;
  }

  public Long getHiveHdfsNsQuota() { return hiveHdfsNsQuota; }

  public void setHiveHdfsNsQuota(Long hiveHdfsNsQuota) {
    this.hiveHdfsNsQuota = hiveHdfsNsQuota;
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
        ", yarnQuotaInSecs='" + yarnQuotaInSecs + '\'' +
        '}';
  }
}
