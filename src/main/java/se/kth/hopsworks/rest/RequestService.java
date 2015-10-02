package se.kth.hopsworks.rest;

import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.User;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.dataset.DatasetRequest;
import se.kth.hopsworks.dataset.DatasetRequestFacade;
import se.kth.hopsworks.dataset.RequestDTO;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.message.controller.MessageController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;

@Path("/request")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
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
    User user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
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
    if (projTeam != null && projTeam.getTeamRole().equalsIgnoreCase(AllowedRoles.DATA_OWNER)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "You already have full access to this dataset.");
    } else if (projTeam != null && proj.equals(project)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "This dataset is already in the requesting project.");
    }
    DatasetRequest dsRequest = datasetRequest.findByProjectAndDataset(
            project, ds);
    //if there is a prior request by a user in the same project with the same role
    // or the prior request is from a data owner do nothing.
    if (dsRequest != null && (dsRequest.getProjectTeam().getTeamRole().equals(
            projectTeam.getTeamRole()) || dsRequest.getProjectTeam().
            getTeamRole().equals(AllowedRoles.DATA_OWNER))) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "There is a prior request for this dataset by" + projectTeam.
              getUser().getFname() + " " + projectTeam.getUser().getLname()
              + "from the same project.");
    } else if (dsRequest != null && projectTeam.getTeamRole().equals(
            AllowedRoles.DATA_OWNER)) {
      dsRequest.setProjectTeam(projectTeam);
      dsRequest.setMessage(requestDTO.getMessage());
      datasetRequest.merge(dsRequest);
    } else {
      dsRequest = new DatasetRequest(ds, projectTeam, requestDTO.
              getMessage());
      datasetRequest.persistDataset(dsRequest);
    }

    //email body
    String msg = "Hi " + project.getOwner().getFname() + " " + project.
            getOwner().getLname() + ", \n\n"
            + user.getFname() + " " + user.getLname()
            + " wants access to a dataset in a project you own. \n\n"
            + "Dataset name: " + ds.getInode().getInodePK().getName() + "\n"
            + "Project name: " + proj.getName() + "\n"
            + "Atached messag: " + requestDTO.getMessage() + "\n"
            + "After loging in to hopsworks go to : /project/" + proj.getId()
            + "/datasets "
            + " if you want to share this dataset. \n";

    Users from = userFacade.findByEmail(sc.getUserPrincipal().getName());
    Users to = userFacade.findByEmail(proj.getOwner().getEmail());
    String message = "Hi " + to.getFname() + "<br>"
            + "I would like to request access to a dataset in a project you own. <br>"
            + "Project name: " + proj.getName() + "<br>"
            + "Dataset name: " + ds.getInode().getInodePK().getName() + "<br>"
            + "To be shared with my project: " + project.getName() + ".<br>"
            + "Thank you in advance."
            + requestDTO.getMessage();
    String preview = from.getFname() + " would like to have access to a dataset in a project you own.";
    String subject = "Dataset access request.";
    String path = "project/" + proj.getId() + "/datasets";
    // to, from, msg, requested path
    messageBean.send(to, from, subject, preview, message, path);
    try {
      emailBean.sendEmail(proj.getOwner().getEmail(),
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
    User user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
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
            + "Atached messag: " + requestDTO.getMessage() + "\n"
            + "After loging in to hopsworks go to : /project" + project.getId()
            + " and go to members tab "
            + "if you want to add this person as a member in your project. \n";

    Users from = userFacade.findByEmail(sc.getUserPrincipal().getName());
    Users to = userFacade.findByEmail(project.getOwner().getEmail());
    String message = "Hi " + to.getFname() + "<br>"
            + "I would like to join a project you own. <br>"
            + "Project name: " + project.getName() + "<br>"
            + requestDTO.getMessage();
    String preview = from.getFname() + " would like to join a project you own.";
    String subject = "Project join request.";
    String path = "project/" + project.getId();
    // to, from, msg, requested path
    messageBean.send(to, from, subject, preview, message, path);
    try {
      emailBean.sendEmail(project.getOwner().getEmail(),
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
