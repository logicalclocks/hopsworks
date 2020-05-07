/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.featuregroup;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupStorage;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;

public class FeatureGroupPreviewBeanParam {

  @QueryParam("storage")
  @ApiParam(value = "ex. storage=offline", allowableValues = "offline,online")
  private String storageStr;
  private FeatureGroupStorage storage = FeatureGroupStorage.OFFLINE;

  @QueryParam("limit")
  @ApiParam(required = false)
  private Integer limit = 20;

  @QueryParam("partition")
  @ApiParam(value = "partition=date=01/01/01")
  private String partition;

  public FeatureGroupPreviewBeanParam(
      @QueryParam("storage") String storageStr,
      @QueryParam("limit") Integer limit,
      @QueryParam("partition") String partition) {

    if (!Strings.isNullOrEmpty(storageStr)) {
      this.storage = FeatureGroupStorage.valueOf(storageStr.toUpperCase());
    }
    this.limit = limit;

    if (!Strings.isNullOrEmpty(partition) && storage.equals(FeatureGroupStorage.ONLINE)) {
      throw new IllegalArgumentException("Preview does not support partition selector for online storage");
    }
    this.partition = partition;
  }

  public FeatureGroupStorage getStorage() {
    return storage;
  }

  public void setStorage(FeatureGroupStorage storage) {
    this.storage = storage;
  }

  public String getStorageStr() {
    return storageStr;
  }

  public void setStorageStr(String storageStr) {
    this.storageStr = storageStr;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public String getPartition() {
    return partition;
  }

  public void setPartition(String partition) {
    this.partition = partition;
  }
}
