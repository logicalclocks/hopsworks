/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.tag;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiParam;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TagResource {

  @PUT
  @Path("/{name}")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response putTag(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @ApiParam(value = "Name of the tag")
      @PathParam("name")
          String name,
      @ApiParam(value = "Value of the tag")
          String value) {
    return Response.ok().entity(new TagsDTO()).build();

  }

  @PUT
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response putTags(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @ApiParam(value = "TagsDTO containing multiple tags")
          TagsDTO tags) {
    return Response.ok().entity(new TagsDTO()).build();

  }

  //TagsExpansionBeanParam
  @GET
  @Path("/{name}")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTag(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @ApiParam(value = "Name of the tag")
      @PathParam("name")
          String name) {
    return Response.ok().entity(new TagsDTO()).build();

  }

  // TagsExpansionBeanParam
  @GET
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTags(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo) {
    return Response.ok().entity(new TagsDTO()).build();

  }

  @DELETE
  @Path("/{name}")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTag(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @ApiParam(value = "Name of the tag")
      @PathParam("name")
          String name) {
    return Response.ok().entity(new TagsDTO()).build();

  }

  @DELETE
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTags(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo) {
    return Response.ok().entity(new TagsDTO()).build();

  }

  public void setFeatureView(String name, Integer version) {
  }

}
