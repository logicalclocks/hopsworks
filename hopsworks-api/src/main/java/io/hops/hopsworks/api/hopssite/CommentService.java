package io.hops.hopsworks.api.hopssite;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.hopssite.dto.CommentIssueReqDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.hopssite.CommentDTO;
import io.hops.hopsworks.dela.dto.hopssite.CommentIssueDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.hopssite.HopsSite;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.util.SettingsHelper;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CommentService {

  private final static Logger LOG = Logger.getLogger(CommentService.class.getName());
  @EJB
  private HopssiteController hopsSite;
  @EJB
  protected UsersController usersController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private Settings settings;
  @EJB
  private NoCacheResponse noCacheResponse;
  private String publicDSId;

  public CommentService() {
  }

  public void setPublicDSId(String publicDSId) {
    this.publicDSId = publicDSId;
  }

  @GET
  public Response getAllComments() throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:get:all {0}", publicDSId);
    List<CommentDTO.RetrieveComment> comments = hopsSite.getDatasetAllComments(publicDSId);
    GenericEntity<List<CommentDTO.RetrieveComment>> commentsJson
      = new GenericEntity<List<CommentDTO.RetrieveComment>>(comments) {
      };
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:get:all - done{0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(commentsJson).build();
  }

  @POST
  public Response addComment(@Context SecurityContext sc, String content) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:add {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = SettingsHelper.getUser(userFacade, sc.getUserPrincipal().getName());
    CommentDTO.Publish comment = new CommentDTO.Publish(user.getEmail(), content);
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws ThirdPartyException {
        hopsSite.addComment(publicCId, publicDSId, comment);
        return "ok";
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:add - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @PUT
  @Path("{commentId}")
  public Response updateComment(@Context SecurityContext sc, @PathParam("commentId") Integer commentId, String content)
    throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:update {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = SettingsHelper.getUser(userFacade, sc.getUserPrincipal().getName());
    CommentDTO.Publish comment = new CommentDTO.Publish(user.getEmail(), content);
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws ThirdPartyException {
        hopsSite.updateComment(publicCId, publicDSId, commentId, comment);
        return "ok";
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @DELETE
  @Path("{commentId}")
  public Response deleteComment(@Context SecurityContext sc, @PathParam("commentId") Integer commentId)
    throws ThirdPartyException, ThirdPartyException, ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:delete {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = SettingsHelper.getUser(userFacade, sc.getUserPrincipal().getName());
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws ThirdPartyException {
        hopsSite.removeComment(publicCId, publicDSId, commentId, user.getEmail());
        return "ok";
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:delete - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{commentId}/report")
  public Response reportAbuse(@Context SecurityContext sc, @PathParam("commentId") Integer commentId,
    CommentIssueReqDTO commentReqIssue) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:report {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = SettingsHelper.getUser(userFacade, sc.getUserPrincipal().getName());
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:report issue:{0}", commentReqIssue);
//    CommentIssueDTO commentIssue
//      = new CommentIssueDTO(commentReqIssue.getType(), commentReqIssue.getMsg(), user.getEmail());
    CommentIssueDTO commentIssue = new CommentIssueDTO("default-type", "default-issue", user.getEmail());
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws ThirdPartyException {
        hopsSite.reportComment(publicCId, publicDSId, commentId, commentIssue);
        return "ok";
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:report - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
}
