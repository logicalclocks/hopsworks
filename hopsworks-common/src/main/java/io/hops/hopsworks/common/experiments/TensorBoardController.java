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
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import io.hops.hopsworks.common.util.Settings;

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
  private Settings settings;
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

  public TensorBoardDTO getTensorBoard(Project project, Users user) {
    TensorBoard tb;
    try {
      tb = tensorBoardFacade.findForProjectAndUser(project, user);
      tb.setLastAccessed(new Date());
      tensorBoardFacade.update(tb);
    } catch (DatabaseException dbe) {
      throw new PersistenceException("Failed to get TensorBoard", dbe);
    }
    return new TensorBoardDTO(tb);
  }


  public TensorBoardDTO startTensorBoard(String elasticId, Project project, Users user) {
    //Kill existing TensorBoard
    TensorBoard tb = null;
    TensorBoardDTO tensorBoardDTO = null;
    tb = tensorBoardFacade.findForProjectAndUser(project, user);

    if(tb != null) {
      //TensorBoard could be dead, remove from DB
      if(tensorBoardProcessMgr.ping(tb.getPid()) == 1) {
        try {
          tensorBoardFacade.remove(tb);
        } catch (DatabaseException dbe) {
          throw new PersistenceException("Unable to remove TensorBoard from database" , dbe);
        }
        //TensorBoard is alive, kill it and remove from DB
      } else if (tensorBoardProcessMgr.ping(tb.getPid()) == 0) {
        if (tensorBoardProcessMgr.killTensorBoard(tb) == 0) {
          try {
            tensorBoardFacade.remove(tb);
          } catch (DatabaseException dbe) {
            throw new PersistenceException("Unable to remove TensorBoard from database" , dbe);
          }
        }
      }
    }

    String hdfsLogdir = null;
    try {
      hdfsLogdir = elasticController.getLogdirFromElastic(project, elasticId);
      //Inject correct NN host:port
      hdfsLogdir = replaceNN(hdfsLogdir);

      tensorBoardDTO = new TensorBoardDTO();

      String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
      HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);

      tensorBoardDTO.setElasticId(elasticId);
      tensorBoardDTO.setHdfsLogdir(hdfsLogdir);

      tensorBoardDTO = tensorBoardProcessMgr.startTensorBoard(project, user, hdfsUser, hdfsLogdir);

      TensorBoard newTensorBoard = new TensorBoard();
      TensorBoardPK tensorBoardPK = new TensorBoardPK();
      tensorBoardPK.setProjectId(project.getId());
      tensorBoardPK.setUserId(user.getUid());
      newTensorBoard.setTensorBoardPK(tensorBoardPK);
      newTensorBoard.setPid(tensorBoardDTO.getPid());
      newTensorBoard.setEndpoint(tensorBoardDTO.getEndpoint());
      newTensorBoard.setHdfsUser(hdfsUser);
      newTensorBoard.setElasticId(elasticId);
      newTensorBoard.setLastAccessed(new Date());
      newTensorBoard.setHdfsLogdir(hdfsLogdir);
      tensorBoardFacade.persist(newTensorBoard);
    } catch (NotFoundException nfe) {

    }
    catch(IOException | DatabaseException e) {
      LOGGER.log(Level.SEVERE, "Could not start TensorBoard", e);

    }

    return tensorBoardDTO;
  }

  public void cleanup(Project project, Users user) {
    TensorBoard tb = tensorBoardFacade.findForProjectAndUser(project, user);
    this.cleanup(tb);
  }

  public void cleanup(TensorBoard tb) {
    if(tb != null) {
      //TensorBoard could be dead, remove from DB
      if(tensorBoardProcessMgr.ping(tb.getPid()) != 0) {
        try {
          tensorBoardFacade.remove(tb);
          tensorBoardProcessMgr.cleanupLocalTBDir(tb);
        } catch (DatabaseException | IOException e) {
          LOGGER.log(Level.SEVERE, "Unable to cleanup TensorBoard" , e);
        }
        //TensorBoard is alive, kill it and remove from DB
      } else if (tensorBoardProcessMgr.ping(tb.getPid()) == 0) {
        if (tensorBoardProcessMgr.killTensorBoard(tb) == 0) {
          try {
            tensorBoardFacade.remove(tb);
            tensorBoardProcessMgr.cleanupLocalTBDir(tb);
          } catch (DatabaseException | IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to cleanup TensorBoard" , e);
          }
        }
      }
    }
  }

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
