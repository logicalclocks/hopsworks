package se.kth.hopsworks.zeppelin.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 */
@Path("/")
public class ZeppelinRestApi {

  /**
   * Get the root endpoint Return always 200.
   * <p>
   * @return 200 response
   */
  @GET
  public Response getRoot() {
    return Response.ok().build();
  }
}
