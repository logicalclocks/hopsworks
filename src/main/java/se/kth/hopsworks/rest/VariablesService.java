
package se.kth.hopsworks.rest;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.util.Settings;

@Path("/variables")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class VariablesService {
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB 
  private Settings vf;
  
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVar(@PathParam("id") String id) throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(vf.findById(id).getValue());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }
  @GET
  @Path("twofactor")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTwofactor() throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(vf.findById("twofactor_auth").getValue());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }
}
