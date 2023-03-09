/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.connectionChecker;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;

public class ConnectionCheckerDTO extends RestDTO<ConnectionCheckerDTO> {

  private String connectionOutput;
  private FeaturestoreStorageConnectorDTO storageConnectorDTO;
  private Integer statusCode;

  public ConnectionCheckerDTO() {
  }
  
  public String getConnectionOutput() {
    return connectionOutput;
  }
  
  public void setConnectionOutput(String connectionOutput) {
    this.connectionOutput = connectionOutput;
  }
  
  public FeaturestoreStorageConnectorDTO getStorageConnectorDTO() {
    return storageConnectorDTO;
  }
  
  public void setStorageConnectorDTO(
    FeaturestoreStorageConnectorDTO storageConnectorDTO) {
    this.storageConnectorDTO = storageConnectorDTO;
  }
  
  public Integer getStatusCode() {
    return statusCode;
  }
  
  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }
}
