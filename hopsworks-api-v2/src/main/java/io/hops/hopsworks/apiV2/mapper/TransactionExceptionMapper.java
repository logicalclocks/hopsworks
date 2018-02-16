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

package io.hops.hopsworks.apiV2.mapper;

import io.hops.hopsworks.apiV2.ErrorResponse;

import javax.transaction.RollbackException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TransactionExceptionMapper maps all transaction related exceptions and sends
 * the
 * cause of the exception to the client.
 */
@Provider
public class TransactionExceptionMapper implements
        ExceptionMapper<RollbackException> {

  private final static Logger log = Logger.getLogger(
          TransactionExceptionMapper.class.getName());

  @Override
  public Response toResponse(RollbackException ex) {
    log.log(Level.INFO, "TransactionExceptionMapper: {0}", ex.getClass());
    ErrorResponse json = new ErrorResponse();
    //String cause = ex.getCause().getCause().getCause().getMessage();
    json.setDescription(
            "Oops! something went wrong. The last transaction did not complete as expected :(");
    return Response.status(Response.Status.CONFLICT)
            .entity(json)
            .type(MediaType.APPLICATION_JSON).
            build();
  }

}
