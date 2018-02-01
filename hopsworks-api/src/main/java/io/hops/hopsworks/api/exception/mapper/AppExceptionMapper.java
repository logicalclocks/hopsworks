/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.exception.mapper;

import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AppExceptionMapper implements ExceptionMapper<AppException> {

  private final static Logger LOG = Logger.getLogger(AppExceptionMapper.class.
    getName());

  @Override
  public Response toResponse(AppException ex) {
    if(ex instanceof ThirdPartyException) {
      return handleThirdPartyException((ThirdPartyException)ex);
    } else {
      return handleAppException(ex);
    }
  }

  private Response handleThirdPartyException(ThirdPartyException tpe) {
    LOG.log(Level.WARNING, "Source:<{0}:{1}>ThirdPartyException: {2}",
      new Object[]{tpe.getSource(), tpe.getSourceDetails(), tpe.getMessage()});
    io.hops.hopsworks.common.util.JsonResponse jsonResponse = new io.hops.hopsworks.common.util.JsonResponse();
    jsonResponse.setStatus(Response.Status.EXPECTATION_FAILED.getReasonPhrase());
    jsonResponse.setStatusCode(Response.Status.EXPECTATION_FAILED.getStatusCode());
    jsonResponse.setErrorMsg(tpe.getSource() + ":" + tpe.getSourceDetails() + ":" + tpe.getMessage());
    return Response.status(Response.Status.EXPECTATION_FAILED).entity(jsonResponse).build();
  }
  
  private Response handleAppException(AppException ae) {
    LOG.log(Level.WARNING, "AppExceptionMapper: {0}", ae.getClass());
    JsonResponse json = new JsonResponse();
    json.setStatusCode(ae.getStatus());
    json.setErrorMsg(ae.getMessage());
    return Response.status(ae.getStatus()).entity(json).type(MediaType.APPLICATION_JSON).build();
  }
}
