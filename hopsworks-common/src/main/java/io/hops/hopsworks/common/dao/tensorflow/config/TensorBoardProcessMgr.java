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

package io.hops.hopsworks.common.dao.tensorflow.config;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoardFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * *
 * This class wraps a bash script with sudo rights that can be executed by the node['hopsworks']['user'].
 * /srv/hops/domains/domain1/bin/tfserving.sh
 * The bash script has several commands with parameters that can be executed.
 * This class provides a Java interface for executing the commands.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@DependsOn("Settings")
public class TensorBoardProcessMgr {

  private static final Logger logger = Logger.getLogger(TensorBoardProcessMgr.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private TensorBoardFacade tensorBoardFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;

  @PostConstruct
  public void init() {
  }

  @PreDestroy
  public void preDestroy() {

  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public TensorBoardDTO startTensorBoard(TensorBoard tensorBoard)
          throws IOException {

    String prog = settings.getHopsworksDomainDir() + "/bin/tensorboard.sh";

    Process process = null;
    Integer port = 0;

    BigInteger pid = null;

    String tensorBoardDir = settings.getStagingDir() + Settings.TENSORBOARD_DIRS;

    Project project = projectFacade.find(tensorBoard.getTensorBoardPK().getProjectId());
    HdfsUsers hdfsUser = hdfsUsersFacade.find(tensorBoard.getHdfsUserId());

    String localDir = DigestUtils.sha256Hex(project.getName() + "_" + hdfsUser.getName());
    tensorBoardDir = tensorBoardDir + File.separator + localDir;

    File tbDir = new File(tensorBoardDir);
    if(tbDir.exists()) {
      for(File file: tbDir.listFiles()) {
        if(file.getName().endsWith(".pid")) {
          String pidContents = com.google.common.io.Files.readFirstLine(file, Charset.defaultCharset());
          try {
            pid = BigInteger.valueOf(Long.parseLong(pidContents));
            if (pid != null && ping(pid) == 0) {
              killTensorBoard(pid);
              try {
                TensorBoard tb = tensorBoardFacade.findForProjectAndUser(project,
                        tensorBoard.getTensorBoardPK().getEmail());
                if(tb.getPid().equals(pid)) {
                  tensorBoardFacade.remove(tb);
                }
              } catch(DatabaseException dbe) {
                //skip
              }
            }
          } catch(NumberFormatException nfe) {
            logger.log(Level.WARNING, "Expected number in pidfile " +
                    file.getAbsolutePath() + " got " + pidContents);
          }
        }
      }
      FileUtils.deleteDirectory(tbDir);
    }
    tbDir.mkdirs();

    String anacondaEnvironmentPath = settings.getAnacondaProjectDir(project.getName());

    int retries = 3;

    while(retries > 0) {

      if(retries == 0) {
        throw new IOException("Failed to start TensorBoard for project=" + project.getName() + ", user="
                + tensorBoard.getTensorBoardPK().getEmail());
      }

      // use pidfile to kill any running servers
      port = ThreadLocalRandom.current().nextInt(40000, 59999);

      String[] command = new String[]{"/usr/bin/sudo", prog, "start", hdfsUser.getName(), tensorBoard.getHdfsLogdir(),
        tensorBoardDir, port.toString(), anacondaEnvironmentPath, settings.getHadoopVersion()};

      logger.log(Level.INFO, Arrays.toString(command));
      ProcessBuilder pb = new ProcessBuilder(command);

      try {
        // Send both stdout and stderr to the same stream
        pb.redirectErrorStream(true);

        process = pb.start();

        synchronized (pb) {
          try {
            // Wait until the launcher bash script has finished
            process.waitFor(20l, TimeUnit.SECONDS);
          } catch (InterruptedException ex) {
            logger.log(Level.SEVERE,
                    "Woken while waiting for the TensorBoard to start: {0}",
                    ex.getMessage());
          }
        }

        int exitValue = process.exitValue();
        String pidPath = tensorBoardDir + File.separator + port + ".pid";
        File pidFile = new File(pidPath);
        // Read the pid for TensorBoard server
        if(pidFile.exists()) {
          String pidContents = com.google.common.io.Files.readFirstLine(pidFile, Charset.defaultCharset());
          pid = BigInteger.valueOf(Long.parseLong(pidContents));
        }
        if(exitValue == 0 && pid != null) {
          int maxWait = 10;
          String logFilePath = tensorBoardDir + File.separator + port + ".log";
          File logFile = new File(logFilePath);
          while(maxWait > 0) {
            if(logFile.length() > 0) {
              Thread.currentThread().sleep(5000);
              return new TensorBoardDTO(pid, port, exitValue);
            } else {
              Thread.currentThread().sleep(1000);
              maxWait--;
            }
          }
          return new TensorBoardDTO(pid, port, exitValue);
        } else {
          logger.log(Level.SEVERE,
                  "Failed starting TensorBoard got exitcode " + exitValue + " retrying on new port");
          if(pid != null) {
            killTensorBoard(pid);
          }
          pid = null;
        }

      } catch (Exception ex) {
        logger.log(Level.SEVERE, "Problem starting TensorBoard: {0}", ex.
                toString());
        if (process != null) {
          process.destroyForcibly();
        }
      } finally {
        retries--;
      }
    }

    return null;
  }

  public int killTensorBoard(BigInteger pid) {

    String prog = settings.getHopsworksDomainDir() + "/bin/tensorboard.sh";
    int exitValue;

    String[] command = {"/usr/bin/sudo", prog, "kill", pid.toString()};
    logger.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }
      process.waitFor();
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      exitValue=2;
      logger.log(Level.SEVERE,"Failed to kill TensorBoard" , ex);
    }
    return exitValue;
  }

  public int killTensorBoard(TensorBoard tb) {

    String prog = settings.getHopsworksDomainDir() + "/bin/tensorboard.sh";
    int exitValue;

    String[] command = {"/usr/bin/sudo", prog, "kill", tb.getPid().toString()};
    logger.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }
      process.waitFor();
      exitValue = process.exitValue();
      HdfsUsers hdfsUser = hdfsUsersFacade.find(tb.getHdfsUserId());
      String tensorBoardDir = settings.getStagingDir() + Settings.TENSORBOARD_DIRS;
      String localDir = DigestUtils.sha256Hex(tb.getProject().getName() + "_" + hdfsUser.getName());
      tensorBoardDir = tensorBoardDir + File.separator + localDir;
      FileUtils.deleteDirectory(new File(tensorBoardDir));
    } catch (IOException | InterruptedException ex) {
      exitValue=2;
      logger.log(Level.SEVERE,"Failed to kill TensorBoard" , ex);
    }
    return exitValue;
  }

  public int ping(BigInteger pid) {

    String prog = settings.getHopsworksDomainDir() + "/bin/tensorboard.sh";
    int exitValue = 1;

    String[] command = {"/usr/bin/sudo", prog, "ping", pid.toString()};
    logger.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }
      process.waitFor();
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Problem pinging: {0}", ex.
              toString());
    }
    return exitValue;
  }

  public void removeProject(Project project) {
    Collection<TensorBoard> instances = project.getTensorBoardCollection();

    if (instances != null) {
      //Maybe do this in a separate thread in best-effort style
      for (TensorBoard tensorBoard : instances) {
        killTensorBoard(tensorBoard);
      }
    }
  }
}