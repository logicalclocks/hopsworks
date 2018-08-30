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

import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoardFacade;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardProcessMgr;
import io.hops.hopsworks.common.exception.TensorBoardCleanupException;
import io.hops.hopsworks.common.util.Settings;

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
  @EJB
  private TensorBoardController tensorBoardController;

  @Schedule(persistent = false,
          minute = "*/20",
          hour = "*")
  public void rotate(Timer timer) {

    Collection<TensorBoard> tensorBoardCollection = tensorBoardFacade.findAll();
    for (TensorBoard tensorBoard : tensorBoardCollection) {
      //Standard case, TB have been idle for a given amount of time
      Date accessed = tensorBoard.getLastAccessed();
      Date current = Calendar.getInstance().getTime();
      if ((current.getTime() - accessed.getTime()) > settings.getTensorBoardMaxLastAccessed()) {
        try {
          tensorBoardController.cleanup(tensorBoard);
          LOG.log(Level.INFO, "Killed TensorBoard " + tensorBoard.toString() + " not accessed in the last " +
              settings.getTensorBoardMaxLastAccessed() + " milliseconds");
        } catch (TensorBoardCleanupException tbce) {
          LOG.log(Level.SEVERE, "Failed to clean up running TensorBoard", tbce);
        }
      }
    }

    //sanity check to make sure that all .pid files have a corresponding TB
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
              // do not kill TBs which are in the DB
              boolean tbExists = false;
              for (TensorBoard tb : TBs) {
                if (tb.getPid().equals(pid)) {
                  tbExists = true;
                  continue;
                }
              }

              if (!tbExists) {
                LOG.log(Level.SEVERE, "MANUAL CERTIFICATE CLEANUP NEEDED: Detected a stray TensorBoard with pid "
                    + pid.toString() + " in directory " + file.getAbsolutePath() + " killing it for now...");
                tensorBoardProcessMgr.killTensorBoard(pid);
              }
            }
          }
        }
      }
    } catch(IOException | NumberFormatException e) {
      LOG.log(Level.SEVERE, "Exception while reading .pid files", e);
    }
  }
}
