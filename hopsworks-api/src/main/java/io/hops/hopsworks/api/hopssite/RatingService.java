/*
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
 *
 */

package io.hops.hopsworks.api.hopssite;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.hopssite.dto.RatingValueDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.hopssite.RateDTO;
import io.hops.hopsworks.dela.dto.hopssite.RatingDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.hopssite.HopsSite;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.util.SettingsHelper;
import io.swagger.annotations.ApiParam;
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

  private final static Logger LOG = Logger.getLogger(RatingService.class.getName());
  @EJB
  private HopssiteController hopsSite;
  @EJB
  private UserFacade userFacade;
  @EJB
  private Settings settings;
  @EJB
  private NoCacheResponse noCacheResponse;

  private String publicDSId;

  public RatingService() {
  }

  public void setPublicDSId(String publicDSId) {
    this.publicDSId = publicDSId;
  }

  public static enum RatingFilter {

    USER,
    DATASET
  }

  @GET
  public Response getRating(@Context SecurityContext sc,
    @ApiParam(required = true) @QueryParam("filter") RatingFilter filter) throws ThirdPartyException {
    switch (filter) {
      case DATASET:
        return getDatasetAllRating();
      case USER:
        return getDatasetUserRating(sc);
      default:
        throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "bad request",
          ThirdPartyException.Source.HOPS_SITE, "unknown filter value:" + filter + " - accepted dataset/user");
    }
  }

  public Response getDatasetAllRating() throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:all {0}", publicDSId);
    RatingDTO rating = hopsSite.getDatasetAllRating(publicDSId);
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:all - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(rating).build();
  }

  public Response getDatasetUserRating(@Context SecurityContext sc) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:user {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = SettingsHelper.getUser(userFacade, sc.getUserPrincipal().getName());
    RatingDTO rating = hopsSite.getDatasetUserRating(publicCId, publicDSId, user.getEmail());
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:user - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(rating).build();
  }

  @POST
  public Response addRating(@Context SecurityContext sc, RatingValueDTO rating) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:add {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = SettingsHelper.getUser(userFacade, sc.getUserPrincipal().getName());
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws ThirdPartyException {
        RateDTO datasetRate = new RateDTO(user.getEmail(), rating.getValue());
        hopsSite.addRating(publicCId, publicDSId, datasetRate);
        return "ok";
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:add - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
}
