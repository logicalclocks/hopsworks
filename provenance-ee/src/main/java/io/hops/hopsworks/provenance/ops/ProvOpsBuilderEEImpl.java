/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.provenance.core.PaginationParams;
import io.hops.hopsworks.common.provenance.ops.ProvOpsBuilderIface;
import io.hops.hopsworks.common.provenance.ops.ProvOpsParams;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
@EnterpriseStereotype
public class ProvOpsBuilderEEImpl implements ProvOpsBuilderIface {
  @EJB
  private ProvOpsController opsProvCtrl;
  
  public ProvOpsDTO build(Project project, ProvOpsParams opsParams, PaginationParams pagParams)
    throws ProvenanceException {
    ProvOpsParamBuilder paramBuilder = new ProvOpsParamBuilder()
      .filterByFields(opsParams.getFileOpsFilterBy())
      .sortByFields(opsParams.getFileOpsSortBy())
      .expansions(opsParams.getExpansions())
      .withAppExpansionFilters(opsParams.getAppExpansionParams())
      .aggregations(opsParams.getAggregations())
      .paginate(pagParams.getOffset(), pagParams.getLimit());
    if (opsParams.getReturnType() == ProvOpsParams.ReturnType.COUNT) {
      return opsProvCtrl.provFileOpsCount(project, paramBuilder);
    } else {
      return opsProvCtrl.provFileOpsList(project, paramBuilder);
    }
  }
}
