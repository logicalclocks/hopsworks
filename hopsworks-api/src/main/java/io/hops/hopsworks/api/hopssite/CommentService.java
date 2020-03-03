/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.hopssite;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.hopssite.dto.CommentIssueReqDTO;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.hopssite.CommentDTO;
import io.hops.hopsworks.dela.dto.hopssite.CommentIssueDTO;
import io.hops.hopsworks.exceptions.DelaException;
import io.hops.hopsworks.dela.hopssite.HopsSite;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
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
  @EJB
  private JWTHelper jWTHelper;
  private String publicDSId;

  public CommentService() {
  }
  
  public void setPublicDSId(String publicDSId) {
    this.publicDSId = publicDSId;
  }

  @GET
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAllComments(@Context SecurityContext sc) throws DelaException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:get:all {0}", publicDSId);
    List<CommentDTO.RetrieveComment> comments = hopsSite.getDatasetAllComments(publicDSId);
    GenericEntity<List<CommentDTO.RetrieveComment>> commentsJson
      = new GenericEntity<List<CommentDTO.RetrieveComment>>(comments) {
      };
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:get:all - done{0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(commentsJson).build();
  }

  @POST
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response addComment(String content, @Context SecurityContext sc) throws DelaException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:add {0}", publicDSId);
    Users user = jWTHelper.getUserPrincipal(sc);
    String publicCId = SettingsHelper.clusterId(settings);
    CommentDTO.Publish comment = new CommentDTO.Publish(user.getEmail(), content);
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws DelaException {
        hopsSite.addComment(publicCId, publicDSId, comment);
        return "ok";
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:add - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @PUT
  @Path("{commentId}")
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response updateComment(@Context SecurityContext sc, @PathParam("commentId") Integer commentId,
      String content) throws DelaException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:update {0}", publicDSId);
    Users user = jWTHelper.getUserPrincipal(sc);
    String publicCId = SettingsHelper.clusterId(settings);
    CommentDTO.Publish comment = new CommentDTO.Publish(user.getEmail(), content);
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws DelaException {
        hopsSite.updateComment(publicCId, publicDSId, commentId, comment);
        return "ok";
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @DELETE
  @Path("{commentId}")
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteComment(@Context SecurityContext sc, @PathParam("commentId") Integer commentId)
    throws DelaException, DelaException, DelaException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:delete {0}", publicDSId);
    Users user = jWTHelper.getUserPrincipal(sc);
    String publicCId = SettingsHelper.clusterId(settings);
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws DelaException {
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
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response reportAbuse(@Context SecurityContext sc, @PathParam("commentId") Integer commentId,
    CommentIssueReqDTO commentReqIssue) throws DelaException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:report {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = jWTHelper.getUserPrincipal(sc);
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:report issue:{0}", commentReqIssue);
//    CommentIssueDTO commentIssue
//      = new CommentIssueDTO(commentReqIssue.getType(), commentReqIssue.getMsg(), user.getEmail());
    CommentIssueDTO commentIssue = new CommentIssueDTO("default-type", "default-issue", user.getEmail());
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws DelaException {
        hopsSite.reportComment(publicCId, publicDSId, commentId, commentIssue);
        return "ok";
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:comment:report - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
}
