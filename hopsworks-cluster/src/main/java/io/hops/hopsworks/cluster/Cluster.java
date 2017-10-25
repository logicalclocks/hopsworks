package io.hops.hopsworks.cluster;

import io.hops.hopsworks.cluster.controller.ClusterController;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.mail.MessagingException;
import javax.security.cert.CertificateException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("cluster")
@Api(value = "Cluster registration Service", description = "Cluster registration Service")
public class Cluster {

  private final static Logger LOGGER = Logger.getLogger(Cluster.class.getName());
  @EJB
  private ClusterController clusterController;

  public Cluster() {
  }

  @POST
  @Path("register")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response register(ClusterDTO cluster, @Context HttpServletRequest req) throws MessagingException {
    LOGGER.log(Level.INFO, "Registering : {0}", cluster);
    clusterController.register(cluster, req);
    JsonResponse res = new JsonResponse();
    res.setStatusCode(Response.Status.OK.getStatusCode());
    res.setSuccessMessage("Cluster registerd. Please validate your email before installing your cluster.");
    return Response.ok().entity(res).build();
  }

  @POST
  @Path("unregister")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response unregister(ClusterDTO cluster, @Context HttpServletRequest req) throws MessagingException {
    LOGGER.log(Level.INFO, "Unregistering : {0}", cluster);
    clusterController.unregister(cluster, req);
    JsonResponse res = new JsonResponse();
    res.setStatusCode(Response.Status.OK.getStatusCode());
    res.setSuccessMessage("Cluster unregisterd. Please validate your email to complite the unregistration.");
    return Response.ok().entity(res).build();
  }

  @GET
  @Path("register/confirm/{validationKey}")
  public Response confirmRegister(@PathParam("validationKey") String validationKey, @Context HttpServletRequest req) {
    JsonResponse res = new JsonResponse();
    try {
      clusterController.validateRequest(validationKey, req, ClusterController.OP_TYPE.REGISTER);
    } catch (ParseException | IOException | InterruptedException | CertificateException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      res.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
      res.setSuccessMessage("Could not validate registration.");
      return Response.ok().entity(res).build();
    }
    res.setStatusCode(Response.Status.OK.getStatusCode());
    res.setSuccessMessage("Cluster registration validated.");
    return Response.ok().entity(res).build();
  }

  @GET
  @Path("unregister/confirm/{validationKey}")
  public Response confirmUnregister(@PathParam("validationKey") String validationKey, @Context HttpServletRequest req) {
    JsonResponse res = new JsonResponse();
    try {
      clusterController.validateRequest(validationKey, req, ClusterController.OP_TYPE.UNREGISTER);
    } catch (ParseException | IOException | InterruptedException | CertificateException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      res.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
      res.setSuccessMessage("Could not validate unregistration.");
      return Response.ok("Could not validate unregistration.").build();
    }
    res.setStatusCode(Response.Status.OK.getStatusCode());
    res.setSuccessMessage("Cluster unregistration validated.");
    return Response.ok().entity(res).build();
  }
}
