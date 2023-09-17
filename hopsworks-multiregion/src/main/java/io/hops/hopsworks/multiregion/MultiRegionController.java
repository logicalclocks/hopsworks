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

package io.hops.hopsworks.multiregion;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MultiRegionController {

  private boolean isEnabled = false;
  private boolean isPrimaryRegion = false;

  private String primaryRegionName;
  private String secondaryRegionName;

  @EJB
  private MultiRegionConfiguration multiRegionConfiguration;

  @PostConstruct
  public void init() {
    isEnabled =
        multiRegionConfiguration.getBoolean(MultiRegionConfiguration.MultiRegionConfKeys.MULTIREGION_WATCHDOG_ENABLED);
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  public boolean isPrimaryRegion() {
    return isPrimaryRegion;
  }

  public void setPrimaryRegion(boolean primaryRegion) {
    isPrimaryRegion = primaryRegion;
  }

  public String getPrimaryRegionName() {
    return primaryRegionName;
  }

  public void setPrimaryRegionName(String primaryRegionName) {
    this.primaryRegionName = primaryRegionName;
  }

  public String getSecondaryRegionName() {
    return secondaryRegionName;
  }

  public void setSecondaryRegionName(String secondaryRegionName) {
    this.secondaryRegionName = secondaryRegionName;
  }

  public boolean blockSecondaryOperation() {
    if (!isEnabled) {
      return false;
    }
    return !isPrimaryRegion;
  }
}
