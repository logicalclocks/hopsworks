package io.hops.hdfs;

import se.kth.bbc.project.ProjectTeam;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.rest.AppException;
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

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class Endpoint {

    public Endpoint() {
    }

    @EJB
    private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;
    @EJB
    private NoCacheResponse noCacheResponse;

    @GET
    @Path("endpoint")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response findEndpoint(
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException {

        HdfsLeDescriptors hdfsLeDescriptors = hdfsLeDescriptorsFacade.findEndpoint();

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                hdfsLeDescriptors.getHostname()).build();
    }
}
