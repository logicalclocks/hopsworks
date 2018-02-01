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
