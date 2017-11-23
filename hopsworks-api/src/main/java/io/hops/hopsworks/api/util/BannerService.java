package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.maintenance.Maintenance;
import io.hops.hopsworks.common.maintenance.MaintenanceController;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/banner")
@Stateless
@Api(value = "Banner Service", description = "Banner Service")
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
  @EJB
  private UserFacade userFacade;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response findBanner() throws AppException {
    Maintenance maintenance = maintenanceController.getMaintenance();
    maintenance.setOtp(settings.getTwoFactorAuth());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(maintenance).build();
  }
  
  @GET
  @Path("user")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response findUserBanner(@Context SecurityContext sc) throws AppException {
    Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("");
    if (user != null && (user.getSalt() == null || user.getSalt().isEmpty())) {
      json.setSuccessMessage("For security purposes, we highly recommend you change your password.");
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }
}
