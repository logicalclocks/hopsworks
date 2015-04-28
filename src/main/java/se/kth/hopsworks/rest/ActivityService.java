/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.rest;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityDetail;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Path("/activity")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class ActivityService {
    @EJB
    private ActivityController activityBean;
    @EJB
    private UserFacade userBean;
    @EJB
    private NoCacheResponse noCacheResponse;
    
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAllByUser(@Context SecurityContext sc, @Context HttpServletRequest req) {
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
        List<ActivityDetail> activityDetails = activityBean.activityDetailOnUser(user.getEmail());
        GenericEntity<List<ActivityDetail>> projectActivities = new GenericEntity<List<ActivityDetail>>(activityDetails) {
        };
        
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectActivities).build();
    }
    
    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findPaginatedByUser(@QueryParam("from") int from,
		                        @QueryParam("to") int to,
                                        @Context SecurityContext sc, 
                                        @Context HttpServletRequest req) {
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
        List<ActivityDetail> activityDetails = activityBean.getPaginatedActivityDetailForUser(from, to, user.getEmail());
        GenericEntity<List<ActivityDetail>> projectActivities = new GenericEntity<List<ActivityDetail>>(activityDetails) {
        };
        
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectActivities).build();
    }
    
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response findAllByProject(@PathParam("id") String id, @Context SecurityContext sc, @Context HttpServletRequest req) {             
        List<ActivityDetail> activityDetails = activityBean.activityDetailOnStudy(id);
        GenericEntity<List<ActivityDetail>> projectActivities = new GenericEntity<List<ActivityDetail>>(activityDetails) {
        };
        
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectActivities).build();
    }
}
