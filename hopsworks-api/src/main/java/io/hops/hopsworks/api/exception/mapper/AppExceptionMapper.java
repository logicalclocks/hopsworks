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
