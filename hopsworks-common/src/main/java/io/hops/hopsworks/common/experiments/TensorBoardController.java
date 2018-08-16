package io.hops.hopsworks.common.experiments;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptors;
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
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.TensorBoardCleanupException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.PersistenceException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class TensorBoardController {
  @EJB
  TensorBoardFacade tensorBoardFacade;
  @EJB
  TensorBoardProcessMgr tensorBoardProcessMgr;
  @EJB
  ElasticController elasticController;
  @EJB
  HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;
  @EJB
  HdfsUsersFacade hdfsUsersFacade;
  @EJB
  HdfsUsersController hdfsUsersController;

  private static final Logger LOGGER = Logger.getLogger(TensorBoardController.class.getName());

  /**
   * Fetch the TensorBoard from the database for the user in this project
   * @param project
   * @param user
   * @return
   */
  public TensorBoardDTO getTensorBoard(Project project, Users user) {
    TensorBoard tb;
    try {
      tb = tensorBoardFacade.findForProjectAndUser(project, user);
      if(tb == null) {
        return null;
      }
      tb.setLastAccessed(new Date());
      tensorBoardFacade.update(tb);
    } catch (DatabaseException dbe) {
      throw new PersistenceException("Failed to get TensorBoard", dbe);
    }
    return new TensorBoardDTO(tb);
  }


  /**
   * Start the TensorBoard for the specific user in this project with the specified elasticId containing the logdir
   * @param elasticId
   * @param project
   * @param user
   * @return
   * @throws TensorBoardCleanupException
   */
  public TensorBoardDTO startTensorBoard(String elasticId, Project project, Users user)
      throws TensorBoardCleanupException {
    //Kill existing TensorBoard
    TensorBoard tb = null;
    TensorBoardDTO tensorBoardDTO = null;
    tb = tensorBoardFacade.findForProjectAndUser(project, user);

    if(tb != null) {
      cleanup(tb);
    }

    String hdfsLogdir = null;
    try {
      hdfsLogdir = elasticController.getLogdirFromElastic(project, elasticId);
    } catch (NotFoundException nfe) {
      LOGGER.log(Level.SEVERE, "Could not locate logdir from elastic ", nfe);
      return null;
    }

    //Inject an alive NN host:port
    hdfsLogdir = replaceNN(hdfsLogdir);

    try {
      String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
      HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);

      tensorBoardDTO = tensorBoardProcessMgr.startTensorBoard(project, user, hdfsUser, hdfsLogdir);

      TensorBoard newTensorBoard = new TensorBoard();
      TensorBoardPK tensorBoardPK = new TensorBoardPK();
      tensorBoardPK.setProjectId(project.getId());
      tensorBoardPK.setUserId(user.getUid());
      newTensorBoard.setTensorBoardPK(tensorBoardPK);
      newTensorBoard.setPid(tensorBoardDTO.getPid());
      newTensorBoard.setEndpoint(tensorBoardDTO.getEndpoint());
      newTensorBoard.setHdfsUserId(hdfsUser.getId());
      newTensorBoard.setElasticId(elasticId);
      newTensorBoard.setLastAccessed(new Date());
      newTensorBoard.setHdfsLogdir(hdfsLogdir);
      tensorBoardFacade.persist(newTensorBoard);
    } catch(IOException | DatabaseException e) {
      LOGGER.log(Level.SEVERE, "Could not start TensorBoard", e);
    }
    return tensorBoardDTO;
  }

  /**
   * Stop and cleanup a TensorBoard for the given project and user
   * @param project
   * @param user
   */
  public void cleanup(Project project, Users user) throws TensorBoardCleanupException {
    TensorBoard tb = tensorBoardFacade.findForProjectAndUser(project, user);
    this.cleanup(tb);
  }

  /**
   * Stop and cleanup a TensorBoard
   * @param tb
   */
  public void cleanup(TensorBoard tb) throws TensorBoardCleanupException {
    if(tb != null) {
      //TensorBoard could be dead, remove from DB
      if(tensorBoardProcessMgr.ping(tb.getPid()) != 0) {
        try {
          tensorBoardFacade.remove(tb);
          tensorBoardProcessMgr.cleanupLocalTBDir(tb);
        } catch (DatabaseException | IOException e) {
          throw new TensorBoardCleanupException("Exception while cleaning up after TensorBoard" , e);
        }
        //TensorBoard is alive, kill it and remove from DB
      } else if (tensorBoardProcessMgr.ping(tb.getPid()) == 0) {
        if (tensorBoardProcessMgr.killTensorBoard(tb) == 0) {
          try {
            tensorBoardFacade.remove(tb);
            tensorBoardProcessMgr.cleanupLocalTBDir(tb);
          } catch (DatabaseException | IOException e) {
            throw new TensorBoardCleanupException("Exception while cleaning up after TensorBoard" , e);
          }
        }
      }
    }
  }



  /**
   * Replace the namenode host:port which may be old (the path is previously read from elastic)
   * @param hdfsPath
   * @return HDFS path with updated namenode host:port
   */
  private String replaceNN(String hdfsPath)  {
    HdfsLeDescriptors descriptor = hdfsLeDescriptorsFacade.findEndpoint();

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
}
