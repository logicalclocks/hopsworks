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
package io.hops.hopsworks.api.maggy;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.dao.maggy.MaggyFacade;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.maggy.MaggyDriver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/maggy")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Maggy Service", description = "Register and retrieve Maggy Driver Endpoints, used in logging by " +
  "sparkmagic")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MaggyService {
  
  @EJB
  private MaggyFacade maggyFacade;
  
  @GET
  @Path("drivers/{appId}")
  @ApiOperation(value = "Get the latest Maggy Driver Endpoint for this YARN appId", response = MaggyDriver.class)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDriver( @PathParam("appId") String appId, @Context HttpServletRequest req) {
    if (Strings.isNullOrEmpty(appId)) {
      throw new IllegalArgumentException("appId was not provided or was empty");
    }
    List<MaggyDriver> md = maggyFacade.findByAppId(appId);
    if (md == null || md.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    MaggyDriver latest_md = md.get(0);
    GenericEntity<MaggyDriver> driver = new GenericEntity<MaggyDriver>(latest_md) {};
    return Response.ok().entity(driver).build();
  }
  
  @POST
  @Path("drivers")
  @ApiOperation(value = "Register a Maggy Driver Endpoint for this YARN appId (called by Spark Driver in maggy).")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response register(MaggyDriver driver, @Context SecurityContext sc) throws ServiceException {
    if (driver == null || driver.getAppId() == null) {
      throw new IllegalArgumentException("Driver was null or had no appId");
    }
    maggyFacade.add(driver);
    return Response.noContent().build();
  }
  
  @DELETE
  @Path("drivers/{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Deletes all Maggy endpoints associated with an AppId")
  public Response deleteByAppId(@PathParam("appId") String appId, @Context SecurityContext sc){
    if (Strings.isNullOrEmpty(appId)) {
      throw new IllegalArgumentException("appId was not provided or was empty");
    }
    maggyFacade.removeByAppId(appId);
    return Response.noContent().build();
  }
}
