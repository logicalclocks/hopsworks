/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.auth.api.exception.mapper;

import com.auth0.jwt.exceptions.JWTVerificationException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.exception.AccessException;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.remote.user.auth.api.RESTApiJsonResponse;
import java.util.logging.Level;
import io.hops.hopsworks.restutils.ThrowableMapper;

import javax.ejb.EJB;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class RESTApiThrowableMapper extends ThrowableMapper {

  @EJB
  Settings settings;

  @Override
  public Response handleRESTException(Response.StatusType status, RESTException ex) {
    return Response.status(status)
        .entity(ex.buildJsonResponse(new RESTApiJsonResponse(), settings.getHopsworksRESTLogLevel()))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  @Override
  public Response handleUnknownException(Throwable exception) {
    if (exception instanceof JWTException) {
      if (exception instanceof AccessException) {
        return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.REST_ACCESS_CONTROL,
            Level.FINE, null, exception.getMessage(), exception));
      } else if (exception instanceof InvalidationException) {
        return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.INVALIDATION_ERROR, 
            Level.WARNING, null, exception.getMessage(), exception));
      } else if (exception instanceof NotRenewableException) {
        return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.NOT_RENEWABLE_TOKEN,
            Level.FINE, null, exception.getMessage(), exception));
      } else if (exception instanceof SigningKeyNotFoundException) {
        return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL, Level.FINE,
            null, exception.getMessage(), exception));
      } else if (exception instanceof VerificationException) {
        return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL, Level.FINE,
            null, exception.getMessage(), exception));
      } else if (exception instanceof DuplicateSigningKeyException) {
        return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.DUPLICATE_KEY_ERROR,
            Level.FINE, null, exception.getMessage(), exception));
      } else {
        return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, null,
            exception.getMessage(), exception));
      }
    } else if (exception instanceof JWTVerificationException) {
      return handleRESTException(new HopsSecurityException(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL, Level.FINE,
          null, exception.getMessage(), exception));
    } else {
      return handleRESTException(new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, null,
          exception.getMessage(), exception));
    }
  }
}
