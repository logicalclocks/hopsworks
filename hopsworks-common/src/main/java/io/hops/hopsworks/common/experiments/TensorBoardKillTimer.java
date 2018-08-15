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

package io.hops.hopsworks.common.experiments;

import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoardFacade;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardProcessMgr;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.DependsOn;
import javax.ejb.TimerService;
import javax.ejb.EJB;
import javax.ejb.Timer;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
public class TensorBoardKillTimer {
  private final static Logger LOG = Logger.getLogger(TensorBoardKillTimer.class.getName());

  @Resource
  private TimerService timerService;
  @EJB
  private Settings settings;
  @EJB
  private TensorBoardFacade tensorBoardFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private TensorBoardProcessMgr tensorBoardProcessMgr;

  @Schedule(persistent = false,
          minute = "*/4",
          hour = "*")
  public void rotate(Timer timer) {
    LOG.log(Level.FINEST, "Killing TensorBoards not accessed in " + settings.getTensorBoardMaxLastAccessed());
    Collection<TensorBoard> tensorBoardCollection = tensorBoardFacade.findAll();
    for (TensorBoard tensorBoard : tensorBoardCollection) {
      //Standard case, TB have been idle for a given amount of time
      Date accessed = tensorBoard.getLastAccessed();
      Date current = Calendar.getInstance().getTime();
      if ((current.getTime() - accessed.getTime()) > settings.getTensorBoardMaxLastAccessed()) {
        if (tensorBoardProcessMgr.ping(tensorBoard.getPid()) == 0) {
          if (tensorBoardProcessMgr.killTensorBoard(tensorBoard) == 0) {
            try {
              tensorBoardFacade.remove(tensorBoard);
              HdfsUsers hdfsUser = tensorBoard.getHdfsUser();
              String tbPath = settings.getStagingDir() + Settings.TENSORBOARD_DIRS + File.separator +
                      DigestUtils.sha256Hex(tensorBoard.getProject().getName() + "_" + hdfsUser.getName());
              File tbDir = new File(tbPath);
              if(tbDir.exists()) {
                FileUtils.deleteDirectory(tbDir);
              }
            } catch (DatabaseException | IOException e) {
              LOG.log(Level.SEVERE, "Failed while trying to kill TensorBoard", e);
            }
          } else {
            LOG.log(Level.SEVERE, "Unable to kill TensorBoard with pid " + tensorBoard.getPid());
          }
        }
        continue;
      }

      //TensorBoard might have been killed manually (i.e we have entry in DB but no actual running process)
      if (tensorBoardProcessMgr.ping(tensorBoard.getPid()) != 0) {
        try {
          tensorBoardFacade.remove(tensorBoard);
          HdfsUsers hdfsUser = tensorBoard.getHdfsUser();
          String tbPath = settings.getStagingDir() + Settings.TENSORBOARD_DIRS + File.separator +

                  DigestUtils.sha256Hex(tensorBoard.getProject().getName() + "_" + hdfsUser.getName());
          File tbDir = new File(tbPath);
          if(tbDir.exists()) {
            FileUtils.deleteDirectory(tbDir);
          }
        } catch (DatabaseException | IOException e) {
          LOG.log(Level.SEVERE, "Failed while trying to kill stray TensorBoard", e);
        }
        continue;
      }
    }
    //handle case where TB might be running but with no corresponding entry in DB
    try {
      List<TensorBoard> TBs = tensorBoardFacade.findAll();
      String tbDirPath = settings.getStagingDir() + Settings.TENSORBOARD_DIRS;
      File tbDir = new File(tbDirPath);
      //For each project_projectmember directory try to find .pid file
      for (File file : tbDir.listFiles()) {
        for (File possiblePidFile : file.listFiles()) {
          if (possiblePidFile.getName().endsWith(".pid")) {
            String pidContents = com.google.common.io.Files.readFirstLine(possiblePidFile, Charset.defaultCharset());
            BigInteger pid = BigInteger.valueOf(Long.parseLong(pidContents));

            if(pid != null) {
              boolean tbExists = false;
              for (TensorBoard tb : TBs) {
                if (tb.getPid().equals(pid)) {
                  tbExists = true;
                  continue;
                }
              }

              if (!tbExists) {
                if (tensorBoardProcessMgr.ping(pid) == 0) {
                  tensorBoardProcessMgr.killTensorBoard(pid);
                  FileUtils.deleteDirectory(file);
                }
              }
            }
          }
        }
      }
    } catch(IOException | NumberFormatException e) {
      LOG.log(Level.SEVERE, "Exception while trying to kill stray TensorBoards", e);
    }
  }
}
