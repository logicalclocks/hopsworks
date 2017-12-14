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
