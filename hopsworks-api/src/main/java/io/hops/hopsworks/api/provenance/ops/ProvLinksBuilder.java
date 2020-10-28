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
import io.hops.hopsworks.common.provenance.ops.ProvLinksParamBuilder;
import io.hops.hopsworks.common.provenance.ops.ProvOpsControllerIface;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvLinksBuilder {
  @Inject
  private ProvOpsControllerIface opsProvCtrl;
  
  public ProvLinksDTO build(Project project, ProvLinksBeanParams opsParams, PaginationParams pagParams)
    throws ProvenanceException, GenericException {
    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
      .filterByFields(opsParams.getFilterBy())
      .onlyApps(opsParams.isOnlyApps())
      .linkType(opsParams.isFullLink())
      .paginate(pagParams.getOffset(), pagParams.getLimit());
    return opsProvCtrl.provLinks(project, paramBuilder, true);
  }
}
