/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Response RESPONSE;
  private static final JsonResponse JSON = new JsonResponse("ERROR");

  static {
    RESPONSE = Response.status(500).entity(JSON).build();
  }

  @Override
  @Produces(MediaType.APPLICATION_JSON)
  public Response toResponse(Throwable ex) {
    System.out.println("ThrowableExceptionMapper: " + ex.getClass());
    ex.printStackTrace();
    //usually you don't pass detailed info out (don't do this here in production environments)
    JSON.setErrorMsg(ex.getMessage());

    return RESPONSE;
  }

}
