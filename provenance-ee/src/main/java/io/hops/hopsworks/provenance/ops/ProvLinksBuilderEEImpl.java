/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.provenance.core.PaginationParams;
import io.hops.hopsworks.common.provenance.ops.ProvLinksBuilderIface;
import io.hops.hopsworks.common.provenance.ops.ProvLinksParams;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
@EnterpriseStereotype
public class ProvLinksBuilderEEImpl implements ProvLinksBuilderIface {
  @EJB
  private ProvOpsController opsProvCtrl;
  
  public ProvLinksDTO build(Project project, ProvLinksParams opsParams, PaginationParams pagParams)
    throws ProvenanceException {
    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
      .filterByFields(opsParams.getFilterBy())
      .onlyApps(opsParams.isOnlyApps())
      .linkType(opsParams.isFullLink())
      .paginate(pagParams.getOffset(), pagParams.getLimit());
    return opsProvCtrl.provLinks(project, paramBuilder, true);
  }
}
