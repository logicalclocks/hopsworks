package io.hops.hopsworks.api.project;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetRequest;
import io.hops.hopsworks.common.dao.dataset.DatasetRequestFacade;
import io.hops.hopsworks.common.dao.dataset.RequestDTO;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.message.Message;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.message.MessageController;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;

@Path("/request")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Request Service", description = "Request Service")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RequestService {

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetRequestFacade datasetRequest;
  @EJB
  private InodeFacade inodes;
  @EJB
  private EmailBean emailBean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private UserManager userBean;
  @EJB
  private MessageController messageBean;

  private final static Logger logger = Logger.getLogger(RequestService.class.
          getName());

  @POST
  @Path("/access")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response requestAccess(RequestDTO requestDTO,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (requestDTO == null || requestDTO.getInodeId() == null
            || requestDTO.getProjectId() == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    Inode inode = inodes.findById(requestDTO.getInodeId());
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }

    Inode parent = inodes.findParent(inode);
    //requested project
    Project proj = projectFacade.findByName(parent.getInodePK().getName());
    Dataset ds = datasetFacade.findByProjectAndInode(proj, inode);

    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }
    //requesting project
    Project project = projectFacade.find(requestDTO.getProjectId());

    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    Dataset dsInRequesting = datasetFacade.findByProjectAndInode(project, inode);

    if (dsInRequesting != null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Project already contains dataset.");
    }

    ProjectTeam projectTeam = projectTeamFacade.findByPrimaryKey(project, user);

    if (projectTeam == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "You do not have any role in this project.");
    }
    ProjectTeam projTeam = projectTeamFacade.findByPrimaryKey(proj, user);
    if (projTeam != null && proj.equals(project)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "This dataset is already in the requesting project.");
    }
    DatasetRequest dsRequest = datasetRequest.findByProjectAndDataset(
            project, ds);
    //email body
    String msg = "Hi " + project.getOwner().getFname() + " " + project.
            getOwner().getLname() + ", \n\n"
            + user.getFname() + " " + user.getLname()
            + " wants access to a dataset in a project you own. \n\n"
            + "Dataset name: " + ds.getInode().getInodePK().getName() + "\n"
            + "Project name: " + proj.getName() + "\n"
            + "Attached message: " + requestDTO.getMessageContent() + "\n"
            + "After logging in to hopsworks go to : /project/" + proj.getId()
            + "/datasets "
            + " if you want to share this dataset. \n";

    //if there is a prior request by a user in the same project with the same role
    // or the prior request is from a data owner do nothing.
    if (dsRequest != null && (dsRequest.getProjectTeam().getTeamRole().equals(
            projectTeam.getTeamRole()) || dsRequest.getProjectTeam().
            getTeamRole().equals(AllowedRoles.DATA_OWNER))) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "There is a prior request for this dataset by " + projectTeam.
              getUser().getFname() + " " + projectTeam.getUser().getLname()
              + "from the same project.");
    } else if (dsRequest != null && projectTeam.getTeamRole().equals(
            AllowedRoles.DATA_OWNER)) {
      dsRequest.setProjectTeam(projectTeam);
      dsRequest.setMessageContent(requestDTO.getMessageContent());
      datasetRequest.merge(dsRequest);
    } else {
      Users from = userFacade.findByEmail(sc.getUserPrincipal().getName());
      Users to = userFacade.findByEmail(proj.getOwner().getEmail());
      String message = "Hi " + to.getFname() + "<br>"
              + "I would like to request access to a dataset in a project you own. <br>"
              + "Project name: " + proj.getName() + "<br>"
              + "Dataset name: " + ds.getInode().getInodePK().getName() + "<br>"
              + "To be shared with my project: " + project.getName() + ".<br>"
              + "Thank you in advance.";

      String preview = from.getFname()
              + " would like to have access to a dataset in a project you own.";
      String subject = Settings.MESSAGE_DS_REQ_SUBJECT;
      String path = "project/" + proj.getId() + "/datasets";
      // to, from, msg, requested path
      Message newMsg = new Message(from, to, null, message, true, false);
      newMsg.setPath(path);
      newMsg.setSubject(subject);
      newMsg.setPreview(preview);
      messageBean.send(newMsg);
      dsRequest = new DatasetRequest(ds, projectTeam, requestDTO.
              getMessageContent(), newMsg);
      try {
        datasetRequest.persistDataset(dsRequest);
      } catch (Exception ex) {
        logger.log(Level.SEVERE, "Could not persist datset request:",
                ex.getMessage());
        messageBean.remove(newMsg);
      }
    }

    try {
      emailBean.sendEmail(proj.getOwner().getEmail(), RecipientType.TO,
              "Access request for dataset "
              + ds.getInode().getInodePK().getName(), msg);
    } catch (MessagingException ex) {
      json.setErrorMsg("Could not send e-mail to " + project.getOwner().
              getEmail());
      datasetRequest.remove(dsRequest);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json).build();
    }
    json.setSuccessMessage("Request sent successfully.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/join")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response requestJoin(RequestDTO requestDTO,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (requestDTO == null || requestDTO.getProjectId() == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    //should be removed when users and user merg.
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    Project project = projectFacade.find(requestDTO.getProjectId());

    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }

    ProjectTeam projectTeam = projectTeamFacade.findByPrimaryKey(project, user);

    if (projectTeam != null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "You are already a member of this project.");
    }
    //email body
    String msg = "Hi " + project.getOwner().getFname() + " " + project.
            getOwner().getLname() + ", \n\n"
            + user.getFname() + " " + user.getLname()
            + " wants to join a project you own. \n\n"
            + "Project name: " + project.getName() + "\n"
            + "Attached message: " + requestDTO.getMessageContent() + "\n"
            + "After loging in to hopsworks go to : /project" + project.getId()
            + " and go to members tab "
            + "if you want to add this person as a member in your project. \n";

    Users from = userFacade.findByEmail(sc.getUserPrincipal().getName());
    Users to = userFacade.findByEmail(project.getOwner().getEmail());
    String message = "Hi " + to.getFname() + "<br>"
            + "I would like to join a project you own. <br>"
            + "Project name: " + project.getName() + "<br>"
            + requestDTO.getMessageContent();
    String preview = from.getFname() + " would like to join a project you own.";
    String subject = "Project join request.";
    String path = "project/" + project.getId();
    // to, from, msg, requested path
    messageBean.send(to, from, subject, preview, message, path);
    try {
      emailBean.sendEmail(project.getOwner().getEmail(), RecipientType.TO,
              "Join request for project "
              + project.getName(), msg);
    } catch (MessagingException ex) {
      json.setErrorMsg("Could not send e-mail to " + project.getOwner().
              getEmail());

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(
                      json).build();
    }
    json.setSuccessMessage("Request sent successfully.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

}
