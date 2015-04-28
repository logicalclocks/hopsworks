package se.kth.hopsworks.rest;

import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.controller.UserStatusValidator;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.hopsworks.controller.UsersController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserDTO;
import se.kth.hopsworks.users.UserFacade;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Path("/auth")
@Stateless
public class AuthService {

    @EJB
    private UserFacade userBean;
    @EJB
    private UsersController userController;
    @EJB
    private UserStatusValidator statusValidator;
    @EJB
    private NoCacheResponse noCacheResponse;

    @GET
    @Path("session")
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response session(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();
        req.getServletContext().log("SESSIONID: " + req.getSession().getId());
        try {
            json.setStatus("SUCCESS");
            json.setData(sc.getUserPrincipal().getName());
        } catch (Exception e) {
            throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                    ResponseMessages.AUTHENTICATION_FAILURE);
        }
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @POST
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("email") String email, @FormParam("password") String password,
            @Context HttpServletRequest req, @Context HttpHeaders httpHeaders) throws AppException {

        req.getServletContext().log("email: " + email);
        req.getServletContext().log("SESSIONID@login: " + req.getSession().getId());

        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(email);

        //only login if not already logged in...
        if (req.getUserPrincipal() == null) {
            if (user != null && statusValidator.checkStatus(user.getStatus())) {
                try {
                    req.login(email, password);
                    req.getServletContext().log("Authentication: successfully logged in " + email);
                } catch (ServletException e) {
                    throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                            ResponseMessages.AUTHENTICATION_FAILURE);
                }
            }
        } else {
            req.getServletContext().log("Skip logged because already logged in: " + email);
        }

        //read the user data from db and return to caller
        json.setStatus("SUCCESS");
        json.setSessionID(req.getSession().getId());

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @GET
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest req) throws AppException {
        req.getServletContext().log("Logging out...");
        JsonResponse json = new JsonResponse();

        try {
            req.logout();
            json.setStatus("SUCCESS");
            req.getSession().invalidate();
        } catch (ServletException e) {
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Logout failed on backend");
        }
        return Response.ok().entity(json).build();
    }

    @POST
    @Path("register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(UserDTO newUser, @Context HttpServletRequest req) throws AppException {

        req.getServletContext().log("Registering..." + newUser.getEmail() + ", " + newUser.getFirstName());

        JsonResponse json = new JsonResponse();

        userController.registerUser(newUser);
        req.getServletContext().log("successfully registered new user: '" + newUser.getEmail() + "'");

        json.setSuccessMessage(ResponseMessages.CREATED_ACCOUNT);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @POST
    @Path("recoverPassword")
    @Produces(MediaType.APPLICATION_JSON)
    public Response recoverPassword(@FormParam("email") String email,
            @FormParam("securityQuestion") String securityQuestion,
            @FormParam("securityAnswer") String securityAnswer,
            @Context SecurityContext sc) throws AppException {
        JsonResponse json = new JsonResponse();
        
        userController.recoverPassword(email, securityQuestion, securityAnswer);
        json.setStatus("OK");
        json.setSuccessMessage(ResponseMessages.PASSWORD_RESET_SUCCESSFUL);

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

}
