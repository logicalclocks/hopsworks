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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AccessLocalException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * AuthExceptionMapper maps all Access exceptions to authorization exception and
 * sends unauthorized status code to the client.
 */
@Provider
public class AuthExceptionMapper implements
        ExceptionMapper<AccessLocalException> {

  private final static Logger log = Logger.getLogger(AuthExceptionMapper.class.
          getName());

  @Override
  public Response toResponse(AccessLocalException ex) {
    log.log(Level.INFO, "AuthExceptionMapper: {0}", ex.getClass());
    JsonResponse json = new JsonResponse();
    json.setStatusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    json.setErrorMsg(ex.getMessage());
    return Response.status(Response.Status.UNAUTHORIZED)
            .entity(json)
            .type(MediaType.APPLICATION_JSON).
            build();
  }

}
