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
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.Stateless;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
  private HdfsLeDescriptorsFacade hdfsLeFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private TensorBoardFacade tensorBoardFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
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

    String tensorBoardDir = settings.getStagingDir() + Settings.TENSORBOARD_DIRS;

    String localDir = DigestUtils.sha256Hex(project.getName() + "_" + hdfsUser.getName());
    tensorBoardDir = tensorBoardDir + File.separator + localDir;
    String certificatesPath = tensorBoardDir + File.separator + "certificates";
    File certsDir = new File(certificatesPath);


    File tbDir = new File(tensorBoardDir);
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
    certsDir.mkdirs();

    String anacondaEnvironmentPath = settings.getAnacondaProjectDir(project.getName());
    //disable for now

    int retries = 3;

    while(retries > 0) {

      if(retries == 0) {
        throw new IOException("Failed to start TensorBoard for project=" + project.getName() + ", user="
                + user.getUid());
      }

      // use pidfile to kill any running servers
      port = ThreadLocalRandom.current().nextInt(40000, 59999);

      String[] command = new String[]{"/usr/bin/sudo", prog, "start", hdfsUser.getName(), hdfsLogdir,
        tensorBoardDir, port.toString(), anacondaEnvironmentPath, settings.getHadoopVersion(), certificatesPath};

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
        String pidPath = tensorBoardDir + File.separator + port + ".pid";
        File pidFile = new File(pidPath);
        // Read the pid for TensorBoard server
        if(pidFile.exists()) {
          String pidContents = com.google.common.io.Files.readFirstLine(pidFile, Charset.defaultCharset());
          pid = BigInteger.valueOf(Long.parseLong(pidContents));
        }
        if(exitValue == 0 && pid != null) {
          DistributedFileSystemOps dfso = dfsService.getDfsOps();
          try {
            if(settings.getHopsRpcTls()) {
              HopsUtils.materializeCertificatesForUserCustomDir(project.getName(), user.getUsername(), settings
                      .getHdfsTmpCertDir(), dfso, certificateMaterializer, settings, certificatesPath);
            }
          } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Failed in materializing certificates for " +
                hdfsUser + " in directory " + certsDir, ioe);
            HopsUtils.cleanupCertificatesForUserCustomDir(user.getUsername(), project.getName(),
                settings.getHdfsTmpCertDir(), certificateMaterializer, certificatesPath, settings);

          } finally {
            if (dfso != null) {
              dfsService.closeDfsClient(dfso);
            }
          }

          int maxWait = 10;
          String logFilePath = tensorBoardDir + File.separator + port + ".log";
          File logFile = new File(logFilePath);
          while(maxWait > 0) {
            if(logFile.length() > 0) {
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
          tensorBoardDTO.setEndpoint(host + ": " + port);
          return tensorBoardDTO;
        } else {
          LOGGER.log(Level.SEVERE,"Failed starting TensorBoard got exitcode " + exitValue + " retrying on new port");
          if(pid != null) {
            killTensorBoard(pid);
          }
          pid = null;
        }

      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Problem starting TensorBoard: {0}", ex.
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
  public int killTensorBoard(TensorBoard tb) {

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
  public void cleanupLocalTBDir(TensorBoard tb) throws IOException {

    int hdfsUserId = tb.getHdfsUserId();
    HdfsUsers hdfsUser = hdfsUsersFacade.findById(hdfsUserId);
    String tbPath = settings.getStagingDir() + Settings.TENSORBOARD_DIRS + File.separator +
        DigestUtils.sha256Hex(tb.getProject().getName() + "_" + hdfsUser.getName());
    String certsDir = tbPath + File.separator + "certificates";

    //dematerialize certificates
    if(settings.getHopsRpcTls()) {
      DistributedFileSystemOps dfso = dfsService.getDfsOps();
      try {
        HopsUtils.cleanupCertificatesForUserCustomDir(tb.getUsers().getUsername(), tb.getProject().getName(),
            settings.getHdfsTmpCertDir(), certificateMaterializer, certsDir, settings);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Could not cleanup certificates for " + hdfsUser + " in directory " + certsDir, e);
      } finally {
        if (dfso != null) {
          dfsService.closeDfsClient(dfso);
        }
      }
    }

    //remove directory itself
    File tbDir = new File(tbPath);
    if(tbDir.exists()) {
      FileUtils.deleteDirectory(tbDir);
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

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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