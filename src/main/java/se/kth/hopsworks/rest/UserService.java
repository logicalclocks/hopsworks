package se.kth.hopsworks.rest;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.controller.UsersController;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.SshKeyDTO;
import se.kth.hopsworks.users.UserCardDTO;
import se.kth.hopsworks.users.UserDTO;
import se.kth.hopsworks.users.UserFacade;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Path("/user")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UserService {

  @EJB
  private UserFacade userBean;
  @EJB
  private UsersController userController;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Path("allcards")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response findAllByUser(@Context SecurityContext sc,
          @Context HttpServletRequest req) {

    List<Users> users = userBean.findAllUsers();
    List<UserCardDTO> userCardDTOs = new ArrayList<>();

    for (Users user : users) {
      UserCardDTO userCardDTO = new UserCardDTO(user);
      userCardDTOs.add(userCardDTO);
    }

    GenericEntity<List<UserCardDTO>> userCards
            = new GenericEntity<List<UserCardDTO>>(userCardDTOs) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            userCards).build();
  }

  @GET
  @Path("profile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserProfile(@Context SecurityContext sc) throws
          AppException {
    Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.USER_WAS_NOT_FOUND);
    }

    UserDTO userDTO = new UserDTO(user);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            userDTO).build();
  }

  @POST
  @Path("updateProfile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateProfile(@FormParam("firstName") String firstName,
          @FormParam("lastName") String lastName,
          @FormParam("telephoneNum") String telephoneNum,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();

    UserDTO userDTO = userController.updateProfile(sc.getUserPrincipal().
            getName(), firstName, lastName, telephoneNum);

    json.setStatus("OK");
    json.setSuccessMessage(ResponseMessages.PROFILE_UPDATED);
    json.setData(userDTO);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            userDTO).build();
  }

  @POST
  @Path("changeLoginCredentials")
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeLoginCredentials(
          @FormParam("oldPassword") String oldPassword,
          @FormParam("newPassword") String newPassword,
          @FormParam("confirmedPassword") String confirmedPassword,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    userController.changePassword(sc.getUserPrincipal().getName(), oldPassword,
            newPassword, confirmedPassword);

    json.setStatus("OK");
    json.setSuccessMessage(ResponseMessages.PASSWORD_CHANGED);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("changeSecurityQA")
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeSecurityQA(@FormParam("oldPassword") String oldPassword,
          @FormParam("securityQuestion") String securityQuestion,
          @FormParam("securityAnswer") String securityAnswer,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    userController.changeSecQA(sc.getUserPrincipal().getName(), oldPassword,
            securityQuestion, securityAnswer);

    json.setStatus("OK");
    json.setSuccessMessage(ResponseMessages.SEC_QA_CHANGED);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }


    @POST
    @Path("addSshKey")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSshkey(@FormParam("name") String name,
                                  @FormParam("publicKey") String publicKey,
                                  @Context SecurityContext sc,
                                  @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
        int id = user.getUid();
        SshKeyDTO dto = userController.addSshKey(id, name, publicKey);
        json.setStatus("OK");
        json.setSuccessMessage(ResponseMessages.SSH_KEY_ADDED);
        json.setData(dto);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @POST
    @Path("removeSshKey")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeSshkey(@FormParam("name") String name,
                              @Context SecurityContext sc,
                              @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
        int id = user.getUid();
        userController.removeSshKey(id, name);
        json.setStatus("OK");
        json.setSuccessMessage(ResponseMessages.SSH_KEY_REMOVED);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }

    @POST
    @Path("getSshKeys")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSshkeys(@Context SecurityContext sc,
                                 @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();

        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
        int id = user.getUid();
        List<SshKeyDTO> sshkeys = userController.getSshKeys(id);

        json.setStatus("OK");
        json.setSuccessMessage(ResponseMessages.SSH_KEYS_LISTED);
        json.setData(sshkeys);

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
                json).build();
    }
}
