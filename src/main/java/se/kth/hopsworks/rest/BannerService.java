package se.kth.hopsworks.rest;

import se.kth.bbc.security.ua.Maintenance;
import se.kth.hopsworks.controller.MaintenanceController;
import se.kth.hopsworks.filters.AllowedRoles;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.hopsworks.util.Settings;

@Path("/banner")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class BannerService {

    public BannerService() {
    }

    @EJB
    private MaintenanceController maintenanceController;
    @EJB
    private Settings settings;
    @EJB
    private NoCacheResponse noCacheResponse;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.ALL})
    public Response findBanner(
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException {

        Maintenance maintenance = maintenanceController.getMaintenance();
        maintenance.setOtp(settings.getTwoFactorAuth());
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                maintenance).build();
    }
}
