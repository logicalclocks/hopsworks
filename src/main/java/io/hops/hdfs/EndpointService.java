package io.hops.hdfs;

import se.kth.bbc.project.ProjectTeam;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.rest.JsonResponse;
import se.kth.hopsworks.rest.NoCacheResponse;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.util.List;
import java.util.StringTokenizer;

@Path("/endpoint")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EndpointService {

    public EndpointService() {
    }

    @EJB
    private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;
    @EJB
    private NoCacheResponse noCacheResponse;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findEndpoint(
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();
        HdfsLeDescriptors hdfsLeDescriptors = hdfsLeDescriptorsFacade.findEndpoint();

        json.setStatus("SUCCESS");
        json.setData(hdfsLeDescriptors.getHostname());

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }
}
