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
package io.hops.hopsworks.common.provenance.state;

import io.hops.hopsworks.common.provenance.core.PaginationParams;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProvStateBuilder {
  @EJB
  private ProvStateController stateProvCtrl;
  
  public ProvStateDTO build(Project project, ProvStateParams stateParams, PaginationParams pagParams)
    throws ProvenanceException {
    ProvStateParamBuilder paramBuilder = new ProvStateParamBuilder()
      .filterByField(ProvStateParser.FieldsP.PROJECT_I_ID, project.getInode().getId())
      .filterByFields(stateParams.getFileStateFilterBy())
      .sortByFields(stateParams.getFileStateSortBy())
      .filterByXAttrs(stateParams.getExactXAttrParams())
      .filterLikeXAttrs(stateParams.getLikeXAttrParams())
      .hasXAttrs(stateParams.getFilterByHasXAttrs())
      .sortByXAttrs(stateParams.getXattrSortBy())
      .withExpansions(stateParams.getExpansions())
      .withAppExpansionFilter(stateParams.getAppExpansionParams())
      .paginate(pagParams.getOffset(), pagParams.getLimit());
  
    switch (stateParams.getReturnType()) {
      case LIST: return stateProvCtrl.provFileStateList(project, paramBuilder);
      case COUNT: return stateProvCtrl.provFileStateCount(project, paramBuilder);
      default:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
          "return type: " + stateParams.getReturnType() + " is not managed");
    }
  }
}
