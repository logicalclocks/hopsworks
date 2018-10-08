/*
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
 */

package io.hops.hopsworks.common.dao.tensorflow;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "tf_lib_mapping", catalog = "hopsworks", schema = "")
@NamedQueries({
    @NamedQuery(name = "TfLibMapping.findAll", query = "SELECT t FROM TfLibMapping t"),
    @NamedQuery(name = "TfLibMapping.findByTfVersion", query = "SELECT t FROM TfLibMapping t " +
        "WHERE t.tfVersion = :tfVersion")})
public class TfLibMapping implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "tf_version", insertable = false)
  private String tfVersion;

  @Column(name = "cuda_version", insertable = false)
  private String cudaVersion;

  @Column(name = "cudnn_version", insertable = false)
  private String cudnnVersion;

  @Column(name = "nccl_version", insertable = false)
  private String ncclVersion;

  public TfLibMapping() {
  }

  public TfLibMapping(String tfVersion, String cudaVersion, String cudnnVersion, String ncclVersion) {
    this.tfVersion = tfVersion;
    this.cudaVersion = cudaVersion;
    this.cudnnVersion = cudnnVersion;
    this.ncclVersion = ncclVersion;
  }

  public String getTfVersion() {
    return tfVersion;
  }

  public void setTfVersion(String tfVersion) {
    this.tfVersion = tfVersion;
  }

  public String getCudaVersion() {
    return cudaVersion;
  }

  public void setCudaVersion(String cudaVersion) {
    this.cudaVersion = cudaVersion;
  }

  public String getCudnnVersion() {
    return cudnnVersion;
  }

  public void setCudnnVersion(String cudnnVersion) {
    this.cudnnVersion = cudnnVersion;
  }

  public String getNcclVersion() {
    return ncclVersion;
  }

  public void setNcclVersion(String ncclVersion) {
    this.ncclVersion = ncclVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TfLibMapping that = (TfLibMapping) o;

    if (tfVersion != null ? !tfVersion.equals(that.tfVersion) : that.tfVersion != null) return false;
    if (cudaVersion != null ? !cudaVersion.equals(that.cudaVersion) : that.cudaVersion != null) return false;
    if (cudnnVersion != null ? !cudnnVersion.equals(that.cudnnVersion) : that.cudnnVersion != null) return false;
    return ncclVersion != null ? ncclVersion.equals(that.ncclVersion) : that.ncclVersion == null;
  }

  @Override
  public int hashCode() {
    int result = tfVersion != null ? tfVersion.hashCode() : 0;
    result = 31 * result + (cudaVersion != null ? cudaVersion.hashCode() : 0);
    result = 31 * result + (cudnnVersion != null ? cudnnVersion.hashCode() : 0);
    result = 31 * result + (ncclVersion != null ? ncclVersion.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TfLibMapping{" +
        "tfVersion='" + tfVersion + '\'' +
        ", cudaVersion='" + cudaVersion + '\'' +
        ", cudnnVersion='" + cudnnVersion + '\'' +
        ", ncclVersion='" + ncclVersion + '\'' +
        '}';
  }
}
