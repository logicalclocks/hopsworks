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
package io.hops.hopsworks.common.cloud;

import io.hops.hopsworks.common.integrations.OnPremiseStereotype;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

@OnPremiseStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NullAWSSecurityTokenService implements AWSSecurityTokenService {
  @Override
  public boolean isAWSCloud() {
    return false;
  }

  @Override
  public Credentials assumeRole(String roleARN, String roleSessionName, int durationSeconds) throws CloudException {
    throw new CloudException(RESTCodes.CloudErrorCode.CLOUD_FEATURE, Level.FINE, "This method is not allowed in a " +
      "non AWS installation.");
  }
}
