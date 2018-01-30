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
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/banner")
@Stateless
@Api(value = "Banner Service",
    description = "Banner Service")
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
  public Response findUserBanner(@Context HttpServletRequest req) throws AppException {
    Users user = userFacade.findByEmail(req.getRemoteUser());
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("");
    if (user != null && (user.getSalt() == null || user.getSalt().isEmpty())) {
      json.setSuccessMessage("For security purposes, we highly recommend you change your password.");
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("firsttime")
  @Produces(MediaType.TEXT_PLAIN)
  public Response isFirstTimeLogin(@Context HttpServletRequest req) throws AppException {
    if (maintenanceController.isFirstTimeLogin()) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
  }
  
  @GET
  @Path("firstlogin")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response firstLogin(@Context HttpServletRequest req) throws AppException {
    settings.updateVariable("first_time_login", "0");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }  

  @GET
  @Path("admin_pwd_changed")  
  @Produces(MediaType.TEXT_PLAIN)
  public Response adminPwdChanged(@Context HttpServletRequest req) throws AppException {
    if (settings.isDefaultAdminPasswordChanged()) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
  }
}
