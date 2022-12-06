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
package io.hops.hopsworks.common.provenance.ops;

import io.hops.hopsworks.common.integrations.CommunityStereotype;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

@Stateless
@CommunityStereotype
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvOpsControllerImpl implements ProvOpsControllerIface {
  @Override
  public ProvOpsDTO provFileOpsList(Project project, ProvOpsParamBuilder params)
    throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  
  @Override
  public ProvOpsDTO provFileOpsCount(Project project, ProvOpsParamBuilder params)
    throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  
  @Override
  public ProvOpsDTO provFileOpsAggs(Project project, ProvOpsParamBuilder params)
    throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  
  @Override
  public ProvLinksDTO provLinks(Project project, ProvLinksParamBuilder params, boolean filterAlive)
    throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
}
