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

package io.hops.hopsworks.api.tensorflow;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptors;
import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoardFacade;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoardPK;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardDTO;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardProcessMgr;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TensorBoardService {

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private TensorBoardFacade tensorBoardFacade;
  @EJB
  private ElasticController elasticController;
  @EJB
  private TensorBoardProcessMgr tensorBoardProcessMgr;
  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private PathValidator pathValidator;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  private Integer projectId;
  private Project project;

  public TensorBoardService(){
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  private final static Logger LOGGER = Logger.getLogger(TensorBoardService.class.getName());

  @GET
  @Path("/running")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response isRunning(@Context SecurityContext sc,
                              @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Incomplete request!");
    }
    try {
      TensorBoard tb = tensorBoardFacade.findForProjectAndUser(project, sc.getUserPrincipal().getName());
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(tb).build();
    } catch (DatabaseException nre) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }
  }

  @GET
  @Path("/start/{elasticId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response startTensorBoardFromElasticId(@PathParam("elasticId") String elasticId,
                                            @Context SecurityContext sc,
                                            @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
      "Incomplete request!");
    }

    String loggedinemail = sc.getUserPrincipal().getName();
    Users hopsworksUser = userFacade.findByEmail(loggedinemail);
    if (hopsworksUser == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
      "You are not authorized for this invocation.");
    }

    //Kill existing TensorBoard
    TensorBoard tb = null;
    try {
      tb = tensorBoardFacade.findForProjectAndUser(project, loggedinemail);
    } catch (DatabaseException nre) {
      //skip
    }
    if(tb != null) {
      //TensorBoard could be dead, remove from DB
      if(tensorBoardProcessMgr.ping(tb.getPid()) == 1) {
        try {
          tensorBoardFacade.remove(tb);
        } catch (DatabaseException dbe) {
          LOGGER.log(Level.SEVERE, "Unable to remove TensorBoard from database" , dbe);
          throw new AppException(
                  Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                  "Unable to start TensorBoard.");
        }
        //TensorBoard is alive, kill it and remove from DB
      } else if (tensorBoardProcessMgr.ping(tb.getPid()) == 0) {
        if (tensorBoardProcessMgr.killTensorBoard(tb) == 0) {
          try {
            tensorBoardFacade.remove(tb);
          } catch (DatabaseException dbe) {
            LOGGER.log(Level.SEVERE, "Unable to remove TensorBoard from database" , dbe);
            throw new AppException(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Unable to start TensorBoard.");
          }
        }
      }
    }

    String hdfsPath = null;
    TensorBoard newTensorBoard = null;
    try {
      hdfsPath = getLogdirFromElastic(elasticId);
      //Inject correct NN host:port
      hdfsPath = replaceNN(hdfsPath);

      newTensorBoard = new TensorBoard();
      newTensorBoard.setHdfsUserId(getHdfsUserId(sc));
      newTensorBoard.setProject(this.project);
      newTensorBoard.setElasticId(elasticId);
      TensorBoardPK tensorBoardPK = new TensorBoardPK();
      tensorBoardPK.setProjectId(this.projectId);
      tensorBoardPK.setEmail(loggedinemail);
      newTensorBoard.setTensorBoardPK(tensorBoardPK);
      newTensorBoard.setHdfsLogdir(hdfsPath);

      TensorBoardDTO tensorBoardDTO = tensorBoardProcessMgr.startTensorBoard(newTensorBoard);
      newTensorBoard.setEndpoint(tensorBoardDTO.getHostIp() + ":" + tensorBoardDTO.getPort());
      newTensorBoard.setPid(tensorBoardDTO.getPid());
      newTensorBoard.setLastAccessed(new Date());
      tensorBoardFacade.persist(newTensorBoard);
    } catch (NotFoundException nfe) {
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Could not find experiment _id " + elasticId + " - is it valid?");
    }
    catch(IOException | DatabaseException e) {
      LOGGER.log(Level.SEVERE, "Could not start TensorBoard", e);
      tensorBoardProcessMgr.killTensorBoard(newTensorBoard);
      throw new AppException(
        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
        "Problem starting TensorBoard, please contact system administrator.");
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(newTensorBoard).build();
  }

  private String replaceNN(String hdfsPath) throws AppException  {

    HdfsLeDescriptors descriptor = hdfsLeDescriptorsFacade.findEndpoint();

    if(descriptor == null) {
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Problem starting TensorBoard, please contact system administrator.");
    }

    String endPoint = descriptor.getRpcAddresses().split(",")[0].replaceAll(",", "");

    Pattern urlPattern = Pattern.compile("([a-zA-Z0-9\\-\\.]{2,255}:[0-9]{1,6})(/.*$)");
    Matcher urlMatcher = urlPattern.matcher(hdfsPath);
    String elasticNNHost = "";
    if (urlMatcher.find()) {
      elasticNNHost = urlMatcher.group(1);
    }
    hdfsPath = hdfsPath.replaceFirst(elasticNNHost, endPoint);
    return hdfsPath;
  }

  @GET
  @Path("/stop")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response stop(@Context SecurityContext sc,
                                     @Context HttpServletRequest req) throws AppException {
    String loggedinemail = sc.getUserPrincipal().getName();

    TensorBoard tb = null;
    try {
      tb = tensorBoardFacade.findForProjectAndUser(this.project, sc.getUserPrincipal().getName());
    } catch (DatabaseException nre) {
      LOGGER.log(Level.SEVERE, "Could not stop TensorBoard for project=" + project.getName() + ", user=" +
              loggedinemail);
    }

    if(tb != null) {
      //TensorBoard could be dead, remove from DB
      if(tensorBoardProcessMgr.ping(tb.getPid()) != 0) {
        try {
          tensorBoardFacade.remove(tb);
          HdfsUsers hdfsUser = hdfsUsersFacade.findById(tb.getHdfsUserId());
          String tbPath = settings.getStagingDir() + Settings.TENSORBOARD_DIRS + File.separator +
                  DigestUtils.sha256Hex(project.getName() + "_" + hdfsUser.getName());
          File tbDir = new File(tbPath);
          if(tbDir.exists()) {
            FileUtils.deleteDirectory(tbDir);
          }
        } catch (DatabaseException | IOException e) {
          LOGGER.log(Level.SEVERE, "Unable to cleanup TensorBoard" , e);
          throw new AppException(
                  Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                  "Unable to start TensorBoard.");
        }
        //TensorBoard is alive, kill it and remove from DB
      } else if (tensorBoardProcessMgr.ping(tb.getPid()) == 0) {
        if (tensorBoardProcessMgr.killTensorBoard(tb) == 0) {
          try {
            tensorBoardFacade.remove(tb);
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
          } catch (DatabaseException dbe) {
            LOGGER.log(Level.SEVERE, "Unable to remove TensorBoard from database" , dbe);
            throw new AppException(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Unable to start TensorBoard.");
          }
        }
      } else {
        LOGGER.log(Level.SEVERE, "Unable to kill TensorBoard with pid " + tb.getPid());
        throw new AppException(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Unable to kill previously running TensorBoard, please contact system administrator.");
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/view")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response updateLastAccessed(@Context SecurityContext sc,
                       @Context HttpServletRequest req) throws AppException {
    String loggedinemail = sc.getUserPrincipal().getName();
    TensorBoard tb = null;
    try {
      tb = tensorBoardFacade.findForProjectAndUser(this.project, sc.getUserPrincipal().getName());
      tb.setLastAccessed(new Date());
      tensorBoardFacade.update(tb);
    } catch (DatabaseException nre) {
      LOGGER.log(Level.SEVERE, "Could not update accessed date TensorBoard for project=" +
              project.getName() + ", user=" + loggedinemail);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private Integer getHdfsUserId(SecurityContext sc) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        "Incomplete request!");
    }
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
      "You are not authorized for this invocation.");
    }
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);

    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);

    return hdfsUser.getId();
  }

  private String getLogdirFromElastic(String elasticId) throws NotFoundException {
    Map<String, String> params = new HashMap<>();
    params.put("op", "GET");

    String experimentsIndex = this.project.getName() + "_experiments";

    String templateUrl = "http://"+settings.getElasticRESTEndpoint() + "/" +
            experimentsIndex + "/experiments/" + elasticId;

    boolean foundEntry = false;
    JSONObject resp = null;
    try {
      resp = elasticController.sendELKReq(templateUrl, params, false);
      foundEntry = (boolean) resp.get("found");
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Could not find elastic index " + elasticId +
              " for TensorBoard for project " + project.getName());
      throw new NotFoundException("Could not find elastic index " + elasticId);
    }

    if(!foundEntry) {
      LOGGER.log(Level.SEVERE, "Could not find elastic index " + elasticId +
              " for TensorBoard for project " + project.getName());
      throw new NotFoundException("Could not find elastic index " + elasticId);
    }

    JSONObject source = resp.getJSONObject("_source");
    return (String)source.get("logdir");
  }
}
