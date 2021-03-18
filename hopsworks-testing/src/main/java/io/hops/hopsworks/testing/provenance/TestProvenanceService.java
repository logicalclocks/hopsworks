/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.testing.provenance;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.provenance.app.ProvAParser;
import io.hops.hopsworks.common.provenance.app.ProvAppController;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.ProvenanceCleanerController;
import io.hops.hopsworks.common.provenance.util.dto.WrapperDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.testing.provenance.app.ProvAppBeanParams;
import io.hops.hopsworks.testing.provenance.app.ProvAppDTO;
import io.hops.hopsworks.testing.provenance.core.Pagination;
import io.swagger.annotations.Api;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/test/provenance")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Provenance Service", description = "Provenance Service")
public class TestProvenanceService {
  @EJB
  private Settings settings;
  @EJB
  private ProvenanceCleanerController cleanerCtrl;
  @EJB
  private ProvAppController provAppController;
  
  @POST
  @Path("/cleanup")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response cleanup(
    @QueryParam("size")
      Integer size) throws ProvenanceException, ElasticException {
    if (size == null) {
      size = settings.getProvCleanupSize();
    }
    Pair<Integer, String> result = cleanerCtrl.indexCleanupRound("", size);
    return Response.ok().entity(new WrapperDTO(result.getValue0())).build();
  }
  
  @GET
  @Path("/app")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response appOps(
    @BeanParam ProvAppBeanParams params,
    @BeanParam Pagination pagination)
    throws ProvenanceException {
    Map<ProvParser.Field, ProvParser.FilterVal> filterBy = new HashMap<>();
    for (String param : params.getFilterBy()) {
      ProvParser.addToFilters(filterBy, ProvAParser.extractFilter(param));
    }

    List<Pair<ProvParser.Field, SortOrder>> sortBy = new ArrayList<>();
    for(String param : params.getSortBy()) {
      sortBy.add(ProvAParser.extractSort(param));
    }

    Integer offset = pagination.getOffset();
    if(offset == null) {
      offset = 0;
    }
    Integer limit = pagination.getLimit();
    if(limit == null) {
      limit = Settings.PROVENANCE_ELASTIC_PAGE_DEFAULT_SIZE;
    }
    ProvAppDTO result = ProvAppDTO.withAppStates(provAppController.provAppState(filterBy, sortBy, offset, limit));
    return Response.ok().entity(result).build();
  }
}