package se.kth.hopsworks.zeppelin.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.apache.zeppelin.util.Util;
import se.kth.hopsworks.zeppelin.server.JsonResponse;

/**
 */
@Path("/")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
public class ZeppelinRestApi {

  public ZeppelinRestApi() {
  }

  /**
   * Get the root endpoint Return always 200.
   * <p/>
   * @return 200 response
   */
  @GET
  public Response getRoot() {
    return Response.ok().build();
  }
  
  @GET
  @Path("version")
  public Response getVersion() {
    return new JsonResponse<>(Response.Status.OK, "Zeppelin version", Util.getVersion()).build();
  }
}
