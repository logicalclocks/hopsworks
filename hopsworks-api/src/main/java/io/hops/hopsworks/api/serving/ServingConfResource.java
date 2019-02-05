/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.serving;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.serving.KafkaServingHelper;
import io.hops.hopsworks.common.serving.tf.TfServingController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/servingconf")
@Stateless
@Api(value = "UI serving configuration", description = "Get UI serving configuration")
public class ServingConfResource {

  @EJB
  private NoCacheResponse noCacheResponse;

  @Inject
  private TfServingController tfServingController;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get UI configuration for serving", response = ServingConf.class)
  public Response getConfiguration() {
    ServingConf servingConf = new ServingConf(tfServingController.getMaxNumInstances(),
        KafkaServingHelper.SCHEMANAME, KafkaServingHelper.SCHEMAVERSION);
    GenericEntity<ServingConf> servingConfDTOGenericEntity =
        new GenericEntity<ServingConf>(servingConf) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(servingConfDTOGenericEntity).build();
  }
}
