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
package io.hops.hopsworks.api.provenance.ops;

import io.hops.hopsworks.common.provenance.core.PaginationParams;
import io.hops.hopsworks.common.provenance.ops.ProvOpsControllerIface;
import io.hops.hopsworks.common.provenance.ops.ProvOpsParamBuilder;
import io.hops.hopsworks.common.provenance.ops.ProvOpsReturnType;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvOpsBuilder {
  @Inject
  private ProvOpsControllerIface opsProvCtrl;
  
  public ProvOpsDTO build(Project project, ProvOpsBeanParams opsParams, PaginationParams pagParams)
    throws ProvenanceException, GenericException {
    ProvOpsParamBuilder params = new ProvOpsParamBuilder()
      .filterByFields(opsParams.getFileOpsFilterBy())
      .sortByFields(opsParams.getFileOpsSortBy())
      .provExpansions(opsParams.getProvExpansions())
      .withAppExpansionFilters(opsParams.getAppExpansionParams())
      .aggregations(opsParams.getAggregations())
      .paginate(pagParams.getOffset(), pagParams.getLimit());
    return build(project, params, opsParams.getReturnType());
  }
  
  public ProvOpsDTO build(Project project, ProvOpsParamBuilder params, ProvOpsReturnType returnType)
    throws ProvenanceException, GenericException {
    ProvOpsDTO result;
    switch(returnType){
      case LIST:
        result = opsProvCtrl.provFileOpsList(project, params);
        break;
      case COUNT:
        result = opsProvCtrl.provFileOpsCount(project, params);
        break;
      case AGGREGATIONS:
        result = opsProvCtrl.provFileOpsAggs(project, params);
        break;
      default:
        throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, Level.FINE,
          "unhandled return type:" + returnType);
    }
    return result;
  }
}
