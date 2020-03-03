/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.hopssite;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.hopssite.dto.RatingValueDTO;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.hopssite.RateDTO;
import io.hops.hopsworks.dela.dto.hopssite.RatingDTO;
import io.hops.hopsworks.exceptions.DelaException;
import io.hops.hopsworks.dela.hopssite.HopsSite;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.util.SettingsHelper;
import io.swagger.annotations.ApiParam;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RatingService {

  private static final Logger LOGGER = Logger.getLogger(RatingService.class.getName());
  @EJB
  private HopssiteController hopsSite;
  @EJB
  private Settings settings;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private JWTHelper jWTHelper;

  private String publicDSId;
  
  public void setPublicDSId(String publicDSId) {
    this.publicDSId = publicDSId;
  }

  public static enum RatingFilter {

    USER,
    DATASET
  }

  @GET
  public Response getRating(@ApiParam(required = true) @QueryParam("filter") RatingFilter filter,
      @Context SecurityContext sc) throws DelaException {
    switch (filter) {
      case DATASET:
        return getDatasetAllRating();
      case USER:
        return getDatasetUserRating(sc);
      default:
        throw new DelaException(RESTCodes.DelaErrorCode.ILLEGAL_ARGUMENT, Level.FINE, DelaException.Source.HOPS_SITE,
          "unknown filter value:" + filter + " - accepted dataset/user");
    }
  }

  private Response getDatasetAllRating() throws DelaException {
    LOGGER.log(Settings.DELA_DEBUG, "hops-site:rating:get:all {0}", publicDSId);
    RatingDTO rating = hopsSite.getDatasetAllRating(publicDSId);
    LOGGER.log(Settings.DELA_DEBUG, "hops-site:rating:get:all - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(rating).build();
  }

  private Response getDatasetUserRating(SecurityContext sc) throws DelaException {
    LOGGER.log(Settings.DELA_DEBUG, "hops-site:rating:get:user {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = jWTHelper.getUserPrincipal(sc);
    RatingDTO rating = hopsSite.getDatasetUserRating(publicCId, publicDSId, user.getEmail());
    LOGGER.log(Settings.DELA_DEBUG, "hops-site:rating:get:user - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(rating).build();
  }

  @POST
  public Response addRating(@Context SecurityContext sc, RatingValueDTO rating) throws DelaException {
    LOGGER.log(Settings.DELA_DEBUG, "hops-site:rating:add {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = jWTHelper.getUserPrincipal(sc);
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws DelaException {
        RateDTO datasetRate = new RateDTO(user.getEmail(), rating.getValue());
        hopsSite.addRating(publicCId, publicDSId, datasetRate);
        return "ok";
      }
    });
    LOGGER.log(Settings.DELA_DEBUG, "hops-site:rating:add - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
}
