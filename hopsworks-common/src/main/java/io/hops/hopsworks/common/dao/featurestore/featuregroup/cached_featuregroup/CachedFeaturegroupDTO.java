/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.hive.HiveTableType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * DTO containing the human-readable information of a cached featrue group in the Hopsworks feature store,
 * can be converted to JSON or XML representation using jaxb.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class CachedFeaturegroupDTO extends FeaturegroupDTO {

  private Long hiveTableId;
  private List<String> hdfsStorePaths;
  private String inputFormat;
  private HiveTableType hiveTableType;
  private Long inodeId;


  public CachedFeaturegroupDTO() {
    super();
  }
  
  public CachedFeaturegroupDTO(Featuregroup featuregroup) {
    super(featuregroup);
    this.hiveTableId = featuregroup.getCachedFeaturegroup().getHiveTableId();
  }

  @XmlElement
  public Long getHiveTableId() {
    return hiveTableId;
  }

  public void setHiveTableId(Long hiveTableId) {
    this.hiveTableId = hiveTableId;
  }

  @XmlElement
  public List<String> getHdfsStorePaths() {
    return hdfsStorePaths;
  }

  public void setHdfsStorePaths(List<String> hdfsStorePaths) {
    this.hdfsStorePaths = hdfsStorePaths;
  }

  @XmlElement
  public String getInputFormat() {
    return inputFormat;
  }

  public void setInputFormat(String inputFormat) {
    this.inputFormat = inputFormat;
  }

  @XmlElement
  public HiveTableType getHiveTableType() {
    return hiveTableType;
  }

  public void setHiveTableType(HiveTableType hiveTableType) {
    this.hiveTableType = hiveTableType;
  }
  
  @XmlElement
  public Long getInodeId() {
    return inodeId;
  }
  
  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }
  
  @Override
  public String toString() {
    return "CachedFeaturegroupDTO{" +
      "hiveTableId=" + hiveTableId +
      ", hdfsStorePaths=" + hdfsStorePaths +
      ", inputFormat='" + inputFormat + '\'' +
      ", hiveTableType=" + hiveTableType +
      ", inodeId=" + inodeId +
      '}';
  }
}
