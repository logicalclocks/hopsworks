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

import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * *
 * This class wraps a bash script with sudo rights that can be executed by the node['hopsworks']['user'].
 * /srv/hops/domains/domain1/bin/tensorboard.sh
 * The bash script has several commands with parameters that can be executed.
 * This class provides a Java interface for executing the commands.
 */
@Stateless
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@DependsOn("Settings")
public class TensorBoardProcessMgr {

  private static final Logger LOGGER = Logger.getLogger(TensorBoardProcessMgr.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private CertificateMaterializer certificateMaterializer;

  /**
   * Start the TensorBoard process
   * @param project
   * @param user
   * @param hdfsUser
   * @param hdfsLogdir
   * @return
   * @throws IOException
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public TensorBoardDTO startTensorBoard(Project project, Users user, HdfsUsers hdfsUser, String hdfsLogdir)
          throws IOException {

    String prog = settings.getHopsworksDomainDir() + "/bin/tensorboard.sh";
    Process process = null;
    Integer port = 0;
    BigInteger pid = null;
    String tbBasePath = settings.getStagingDir() + Settings.TENSORBOARD_DIRS + File.separator;
    String projectUserUniquePath = project.getName() + "_" + hdfsUser.getName();
    String tbPath = tbBasePath + DigestUtils.sha256Hex(projectUserUniquePath);
    String certsPath = "\"\"";

    File tbDir = new File(tbPath);
    if(tbDir.exists()) {
      for(File file: tbDir.listFiles()) {
        if(file.getName().endsWith(".pid")) {
          String pidContents = com.google.common.io.Files.readFirstLine(file, Charset.defaultCharset());
          try {
            pid = BigInteger.valueOf(Long.parseLong(pidContents));
            if (pid != null && ping(pid) == 0) {
              killTensorBoard(pid);
            }
          } catch(NumberFormatException nfe) {
            LOGGER.log(Level.WARNING, "Expected number in pidfile " +
                    file.getAbsolutePath() + " got " + pidContents);
          }
        }
      }
      FileUtils.deleteDirectory(tbDir);
    }
    tbDir.mkdirs();


    DistributedFileSystemOps dfso = dfsService.getDfsOps();
    try {
      certsPath = tbBasePath + DigestUtils.sha256Hex(projectUserUniquePath + "_certs");
      File certsDir = new File(certsPath);
      certsDir.mkdirs();
      HopsUtils.materializeCertificatesForUserCustomDir(project.getName(), user.getUsername(), settings
          .getHdfsTmpCertDir(), dfso, certificateMaterializer, settings, certsPath);
    } catch (IOException ioe) {
      LOGGER.log(Level.SEVERE, "Failed in materializing certificates for " +
          hdfsUser + " in directory " + certsPath, ioe);
      HopsUtils.cleanupCertificatesForUserCustomDir(user.getUsername(), project.getName(),
          settings.getHdfsTmpCertDir(), certificateMaterializer, certsPath, settings);
    } finally {
      if (dfso != null) {
        dfsService.closeDfsClient(dfso);
      }
    }

    String anacondaEnvironmentPath = settings.getAnacondaProjectDir(project);
    int retries = 3;

    while(retries > 0) {

      if(retries == 0) {
        throw new IOException("Failed to start TensorBoard for project=" + project.getName() + ", user="
                + user.getUid());
      }

      // use pidfile to kill any running servers
      port = ThreadLocalRandom.current().nextInt(40000, 59999);

      String[] command = new String[]{"/usr/bin/sudo", prog, "start", hdfsUser.getName(), hdfsLogdir,
        tbPath, port.toString(), anacondaEnvironmentPath, settings.getHadoopVersion(), certsPath,
      settings.getJavaHome()};

      LOGGER.log(Level.INFO, Arrays.toString(command));
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
            LOGGER.log(Level.SEVERE,
                    "Woken while waiting for the TensorBoard to start: {0}",
                    ex.getMessage());
          }
        }

        int exitValue = process.exitValue();
        String pidPath = tbPath + File.separator + port + ".pid";
        File pidFile = new File(pidPath);
        // Read the pid for TensorBoard server
        if(pidFile.exists()) {
          String pidContents = com.google.common.io.Files.readFirstLine(pidFile, Charset.defaultCharset());
          pid = BigInteger.valueOf(Long.parseLong(pidContents));
        }
        if(exitValue == 0 && pid != null) {
          int maxWait = 10;
          String logFilePath = tbPath + File.separator + port + ".log";
          File logFile = new File(logFilePath);
          while(maxWait > 0) {
            String logFileContents = com.google.common.io.Files.readFirstLine(logFile, Charset.defaultCharset());
            // It is not possible to have a fixed wait time before showing the TB, we need to be sure it has started
            if(logFile.length() > 0 &&
                (logFileContents.contains("Loaded") |
                logFileContents.contains("Reloader") |
                logFileContents.contains("event"))
              | maxWait == 1) {
              Thread.currentThread().sleep(5000);
              TensorBoardDTO tensorBoardDTO = new TensorBoardDTO();
              String host = null;
              try {
                host = InetAddress.getLocalHost().getHostAddress();
              } catch (UnknownHostException ex) {
                Logger.getLogger(TensorBoardProcessMgr.class.getName()).log(Level.SEVERE, null, ex);
              }
              tensorBoardDTO.setEndpoint(host + ":" + port);
              tensorBoardDTO.setPid(pid);
              return tensorBoardDTO;
            } else {
              Thread.currentThread().sleep(1000);
              maxWait--;
            }
          }
          TensorBoardDTO tensorBoardDTO = new TensorBoardDTO();
          tensorBoardDTO.setPid(pid);
          String host = null;
          try {
            host = InetAddress.getLocalHost().getHostAddress();
          } catch (UnknownHostException ex) {
            Logger.getLogger(TensorBoardProcessMgr.class.getName()).log(Level.SEVERE, null, ex);
          }
          tensorBoardDTO.setEndpoint(host + ":" + port);
          return tensorBoardDTO;
        } else {
          LOGGER.log(Level.SEVERE,"Failed starting TensorBoard got exitcode " + exitValue + " retrying on new port");
          if(pid != null) {
            this.killTensorBoard(pid);
          }
          pid = null;
        }

      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Problem starting TensorBoard: {0}", ex);
        if (process != null) {
          process.destroyForcibly();
        }
      } finally {
        retries--;
      }
    }

    //Failed to start TensorBoard, make sure there is no process running for it! (This should not be needed)
    if(pid != null && this.ping(pid) == 0) {
      this.killTensorBoard(pid);
    }

    //Certificates cleanup in case they were materialized but no TB started successfully

    dfso = dfsService.getDfsOps();
    certsPath = tbBasePath + DigestUtils.sha256Hex(projectUserUniquePath + "_certs");
    File certsDir = new File(certsPath);
    certsDir.mkdirs();
    try {
      HopsUtils.cleanupCertificatesForUserCustomDir(user.getUsername(), project.getName(),
          settings.getHdfsTmpCertDir(), certificateMaterializer, certsPath, settings);
    } finally {
      if (dfso != null) {
        dfsService.closeDfsClient(dfso);
      }
    }

    return null;
  }

  /**
   * Kill the TensorBoard process
   * @param pid
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int killTensorBoard(BigInteger pid) {

    String prog = settings.getHopsworksDomainDir() + "/bin/tensorboard.sh";
    int exitValue;

    String[] command = {"/usr/bin/sudo", prog, "kill", pid.toString()};
    LOGGER.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();
      process.waitFor(20l, TimeUnit.SECONDS);
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      exitValue=2;
      LOGGER.log(Level.SEVERE,"Failed to kill TensorBoard" , ex);
    }
    return exitValue;
  }

  /**
   * Kill the TensorBoard process
   * @param tb
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int killTensorBoard(TensorBoard tb) throws ServiceException {

    String prog = settings.getHopsworksDomainDir() + "/bin/tensorboard.sh";
    int exitValue;

    String[] command = {"/usr/bin/sudo", prog, "kill", tb.getPid().toString()};
    LOGGER.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();
      process.waitFor(20l, TimeUnit.SECONDS);
      exitValue = process.exitValue();
      cleanupLocalTBDir(tb);
    } catch (IOException | InterruptedException ex) {
      exitValue=2;
      LOGGER.log(Level.SEVERE,"Failed to kill TensorBoard" , ex);
    }
    return exitValue;
  }

  /**
   * Cleanup the local TensorBoard directory
   * @param tb
   * @throws IOException
   */
  public void cleanupLocalTBDir(TensorBoard tb) throws ServiceException {

    int hdfsUserId = tb.getHdfsUserId();
    HdfsUsers hdfsUser = hdfsUsersFacade.findById(hdfsUserId);
    String tbBasePath = settings.getStagingDir() + Settings.TENSORBOARD_DIRS + File.separator;
    String projectUserUniquePath = tb.getProject().getName() + "_" + hdfsUser.getName();
    String tbPath = tbBasePath + DigestUtils.sha256Hex(projectUserUniquePath);

    //dematerialize certificates
    String certsPath = tbBasePath + DigestUtils.sha256Hex(projectUserUniquePath + "_certs");
    DistributedFileSystemOps dfso = dfsService.getDfsOps();
    try {
      HopsUtils.cleanupCertificatesForUserCustomDir(tb.getUsers().getUsername(), tb.getProject().getName(),
        settings.getHdfsTmpCertDir(), certificateMaterializer, certsPath, settings);
    } finally {
      if (dfso != null) {
        dfsService.closeDfsClient(dfso);
      }
    }

    //remove directory itself
    File tbDir = new File(tbPath);
    if(tbDir.exists()) {
      try {
        FileUtils.deleteDirectory(tbDir);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Could not delete TensorBoard directory: " + tbDir);
        throw new ServiceException(RESTCodes.ServiceErrorCode.TENSORBOARD_CLEANUP_ERROR, Level.SEVERE,
          "TensorBoard directory:"+tbDir, e.getMessage());
      }
    }
  }

  /**
   * Check to see if the process is running and is a TensorBoard started by tensorboard.sh
   * @param pid
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public int ping(BigInteger pid) {

    String prog = settings.getHopsworksDomainDir() + "/bin/tensorboard.sh";
    int exitValue = 1;

    String[] command = {"/usr/bin/sudo", prog, "ping", pid.toString()};
    LOGGER.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();
      process.waitFor(20l, TimeUnit.SECONDS);
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE, "Problem pinging: {0}", ex.
              toString());
    }
    return exitValue;
  }
}