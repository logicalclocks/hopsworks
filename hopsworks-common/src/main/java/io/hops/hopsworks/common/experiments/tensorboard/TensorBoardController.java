/*
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
 */

package io.hops.hopsworks.common.experiments.tensorboard;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoardFacade;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoardPK;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardDTO;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardProcessMgr;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.tensorflow.TfLibMappingUtil;
import io.hops.hopsworks.exceptions.TensorBoardException;
import org.apache.commons.codec.digest.DigestUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TensorBoardController {
  @EJB
  private TensorBoardFacade tensorBoardFacade;
  @EJB
  private TensorBoardProcessMgr tensorBoardProcessMgr;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private TfLibMappingUtil tfLibMappingUtil;

  private static final Logger LOGGER = Logger.getLogger(TensorBoardController.class.getName());

  /**
   * Fetch the TensorBoard from the database for the user in this project
   * @param project
   * @param user
   * @return
   */
  public TensorBoardDTO getTensorBoard(Project project, Users user) {
    TensorBoard tb;
    tb = tensorBoardFacade.findForProjectAndUser(project, user);
    if (tb == null) {
      return null;
    }
    tb.setLastAccessed(new Date());
    tensorBoardFacade.update(tb);
    return new TensorBoardDTO(tb);
  }

  /**
   * Start the TensorBoard for the specific user in this project with the specified elasticId containing the logdir
   * @param mlId
   * @param project
   * @param user
   * @param tensorBoardLogdir
   * @return
   * @throws IOException
   */
  public TensorBoardDTO startTensorBoard(String mlId, Project project, Users user, String tensorBoardLogdir)
    throws TensorBoardException {

    tensorBoardLogdir = prependNameNode(tensorBoardLogdir);

    TensorBoardDTO tensorBoardDTO = null;

    TensorBoard tb = tensorBoardFacade.findForProjectAndUser(project, user);

    if(tb != null) {
      cleanup(tb);
    }

    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);

    String tfLdLibraryPath = tfLibMappingUtil.getTfLdLibraryPath(project);
    String tensorBoardDirectory = DigestUtils.sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt()));

    tensorBoardDTO = tensorBoardProcessMgr.startTensorBoard(project, user, hdfsUser, tensorBoardLogdir,
        tfLdLibraryPath,
            tensorBoardDirectory);
    Date lastAccessed = new Date();
    tensorBoardDTO.setMlId(mlId);
    tensorBoardDTO.setLastAccessed(lastAccessed);
    tensorBoardDTO.setHdfsLogdir(tensorBoardLogdir);

    TensorBoard newTensorBoard = new TensorBoard();
    TensorBoardPK tensorBoardPK = new TensorBoardPK();
    tensorBoardPK.setProjectId(project.getId());
    tensorBoardPK.setUserId(user.getUid());
    newTensorBoard.setTensorBoardPK(tensorBoardPK);
    newTensorBoard.setPid(tensorBoardDTO.getPid());
    newTensorBoard.setEndpoint(tensorBoardDTO.getEndpoint());
    newTensorBoard.setHdfsUserId(hdfsUser.getId());
    newTensorBoard.setMlId(mlId);
    newTensorBoard.setLastAccessed(lastAccessed);
    newTensorBoard.setHdfsLogdir(tensorBoardLogdir);
    newTensorBoard.setSecret(tensorBoardDirectory);
    tensorBoardFacade.persist(newTensorBoard);

    return tensorBoardDTO;
  }

  /**
   * Stop and cleanup a TensorBoard for the given project and user
   * @param project
   * @param user
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void cleanup(Project project, Users user) throws TensorBoardException {
    TensorBoard tb = tensorBoardFacade.findForProjectAndUser(project, user);
    this.cleanup(tb);
  }

  /**
   * Stop and cleanup a TensorBoard
   * @param tb
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void cleanup(TensorBoard tb) throws TensorBoardException {
    if (tb != null) {
      if(tensorBoardProcessMgr.ping(tb)) {
        tensorBoardProcessMgr.killTensorBoard(tb);
      }
      tensorBoardFacade.remove(tb);
      tensorBoardProcessMgr.cleanup(tb);
    }
  }

  /**
   * Remove and cleanup all running TensorBoards for this project
   * @param project
   * @throws IOException
   */
  public void removeProject(Project project) throws TensorBoardException {
    Collection<TensorBoard> instances = project.getTensorBoardCollection();
    if(instances != null) {
      for(TensorBoard tensorBoard: instances) {
        this.cleanup(tensorBoard);
      }
    }
  }

  /**
   * Prepend hdfs://namenode_ip:port to hdfs path
   * @param hdfsPath
   * @return HDFS path with namenode authority
   */
  public String prependNameNode(String hdfsPath)  {
    String endPoint = hdfsLeDescriptorsFacade.getRPCEndpoint();
    return "hdfs://" + endPoint + hdfsPath;
  }
}
