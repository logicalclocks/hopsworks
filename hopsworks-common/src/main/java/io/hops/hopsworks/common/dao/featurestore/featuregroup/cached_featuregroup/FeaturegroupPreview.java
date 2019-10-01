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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * DTO containing a feature group preview (offline and online)
 */
@XmlRootElement
@XmlType(propOrder = {"offlineFeaturegroupPreview", "onlineFeaturegroupPreview"})
public class FeaturegroupPreview {
  
  private List<RowValueQueryResult> offlineFeaturegroupPreview;
  private List<RowValueQueryResult> onlineFeaturegroupPreview;
  
  public FeaturegroupPreview() {
  }
  
  public FeaturegroupPreview(
    List<RowValueQueryResult> offlineFeaturegroupPreview,
    List<RowValueQueryResult> onlineFeaturegroupPreview) {
    this.offlineFeaturegroupPreview = offlineFeaturegroupPreview;
    this.onlineFeaturegroupPreview = onlineFeaturegroupPreview;
  }
  
  @XmlElement
  public List<RowValueQueryResult> getOfflineFeaturegroupPreview() {
    return offlineFeaturegroupPreview;
  }
  
  public void setOfflineFeaturegroupPreview(
    List<RowValueQueryResult> offlineFeaturegroupPreview) {
    this.offlineFeaturegroupPreview = offlineFeaturegroupPreview;
  }
  
  @XmlElement
  public List<RowValueQueryResult> getOnlineFeaturegroupPreview() {
    return onlineFeaturegroupPreview;
  }
  
  public void setOnlineFeaturegroupPreview(
    List<RowValueQueryResult> onlineFeaturegroupPreview) {
    this.onlineFeaturegroupPreview = onlineFeaturegroupPreview;
  }
  
  @Override
  public String toString() {
    return "FeaturegroupPreview{" +
      "offlineFeaturegroupPreview=" + offlineFeaturegroupPreview +
      ", onlineFeaturegroupPreview=" + onlineFeaturegroupPreview +
      '}';
  }
}
