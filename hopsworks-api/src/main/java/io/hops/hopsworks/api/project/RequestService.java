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

package io.hops.hopsworks.api.project;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
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
import io.hops.hopsworks.common.exception.DatasetException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.message.MessageController;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.SecurityContext;

@Path("/request")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Request Service", description = "Request Service")
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
  private MessageController messageBean;
  @EJB
  private JWTHelper jWTHelper;

  private final static Logger logger = Logger.getLogger(RequestService.class.
          getName());

  @POST
  @Path("/access")
  @Produces(MediaType.APPLICATION_JSON)
  public Response requestAccess(RequestDTO requestDTO, @Context SecurityContext sc) throws DatasetException,
      ProjectException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    if (requestDTO == null || requestDTO.getInodeId() == null || requestDTO.getProjectId() == null) {
      throw new IllegalArgumentException("requestDTO was not provided or was incomplete!");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    Inode inode = inodes.findById(requestDTO.getInodeId());
    Inode parent = inodes.findParent(inode);
    //requested project
    Project proj = projectFacade.findByName(parent.getInodePK().getName());
    Dataset ds = datasetFacade.findByProjectAndInode(proj, inode);

    //requesting project
    Project project = projectFacade.find(requestDTO.getProjectId());
    Dataset dsInRequesting = datasetFacade.findByProjectAndInode(project, inode);

    if (dsInRequesting != null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.INFO);
    }

    ProjectTeam projectTeam = projectTeamFacade.findByPrimaryKey(project, user);
    ProjectTeam projTeam = projectTeamFacade.findByPrimaryKey(proj, user);
    if (projTeam != null && proj.equals(project)) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.TEAM_MEMBER_ALREADY_EXISTS, Level.FINE);
    }
    DatasetRequest dsRequest = datasetRequest.findByProjectAndDataset(
            project, ds);
    //email body
    String msg = "Hi " + proj.getOwner().getFname() + " " + proj.
            getOwner().getLname() + ", \n\n"
            + user.getFname() + " " + user.getLname()
            + " wants access to a dataset in a project you own. \n\n"
            + "Dataset name: " + ds.getInode().getInodePK().getName() + "\n"
            + "Project name: " + proj.getName() + "\n";

    if(!Strings.isNullOrEmpty(requestDTO.getMessageContent())) {
      msg += "Attached message: " + requestDTO.getMessageContent() + "\n";
    }

    msg += "After logging in to Hopsworks go to : /project/" + proj.getId()
        + "/datasets "
        + " if you want to share this dataset. \n";

    //if there is a prior request by a user in the same project with the same role
    // or the prior request is from a data owner do nothing.
    if (dsRequest != null && (dsRequest.getProjectTeam().getTeamRole().equals(
            projectTeam.getTeamRole()) || dsRequest.getProjectTeam().
            getTeamRole().equals(AllowedProjectRoles.DATA_OWNER))) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_REQUEST_EXISTS, Level.FINE);
    } else if (dsRequest != null && projectTeam.getTeamRole().equals(
            AllowedProjectRoles.DATA_OWNER)) {
      dsRequest.setProjectTeam(projectTeam);
      dsRequest.setMessageContent(requestDTO.getMessageContent());
      datasetRequest.merge(dsRequest);
    } else {
      Users from = user;
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
        logger.log(Level.SEVERE, "Could not persist dataset request", ex);
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
  public Response requestJoin(RequestDTO requestDTO, @Context SecurityContext sc) throws ProjectException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    if (requestDTO == null || requestDTO.getProjectId() == null) {
      throw new IllegalArgumentException("requestDTO wast not provided or was incomplete.");
    }
    //should be removed when users and user merg.
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = projectFacade.find(requestDTO.getProjectId());
    if(project == null){
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE);
    }
    ProjectTeam projectTeam = projectTeamFacade.findByPrimaryKey(project, user);

    if (projectTeam != null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.TEAM_MEMBER_ALREADY_EXISTS, Level.FINE);
    }
    //email body
    String msg = "Hi " + project.getOwner().getFname() + " " + project.
            getOwner().getLname() + ", \n\n"
            + user.getFname() + " " + user.getLname()
            + " wants to join a project you own. \n\n"
            + "Project name: " + project.getName() + "\n";

    if(!Strings.isNullOrEmpty(requestDTO.getMessageContent())) {
      msg += "Attached message: " + requestDTO.getMessageContent() + "\n";
    }

    msg += "After logging in to Hopsworks go to : /project" + project.getId()
        + " and go to members tab "
        + "if you want to add this person as a member in your project. \n";

    Users from = user;
    Users to = userFacade.findByEmail(project.getOwner().getEmail());
    String message = "Hi " + to.getFname() + "<br>"
            + "I would like to join a project you own. <br>"
            + "Project name: " + project.getName() + "<br>";

    if(!Strings.isNullOrEmpty(requestDTO.getMessageContent())) {
      message += requestDTO.getMessageContent();
    }

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
