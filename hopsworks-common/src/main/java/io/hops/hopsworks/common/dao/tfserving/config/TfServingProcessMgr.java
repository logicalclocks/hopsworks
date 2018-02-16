/*
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
 *
 */

package io.hops.hopsworks.common.dao.tfserving.config;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.tfserving.TfServing;
import io.hops.hopsworks.common.dao.tfserving.TfServingFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;

import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;


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
public class TfServingProcessMgr {

  private static final Logger logger = Logger.getLogger(TfServingProcessMgr.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private TfServingFacade jupyterFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;

  private String hadoopClasspath = null;

  @PostConstruct
  public void init() {
  }

  @PreDestroy
  public void preDestroy() {

  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public TfServingDTO startTfServingAsTfServingUser(String hdfsUser,
                                             TfServing tfServing)
          throws AppException, IOException, InterruptedException {

    String prog = settings.getHopsworksDomainDir() + "/bin/tfserving.sh";

    HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);
    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find hdfs user. Not starting serving.");
    }


    Process process = null;
    Integer port = 0;

    BigInteger pid = null;

    // use pidfile to kill any running servers
    port = ThreadLocalRandom.current().nextInt(40000, 59999);

    String secretDir = settings.getStagingDir() + Settings.SERVING_DIRS + tfServing.getSecret();

    String[] command = new String[]{"/usr/bin/sudo", prog, "start", tfServing.getModelName(),
              tfServing.getVersion() + "", tfServing.getHdfsModelPath(), port.toString(),
        secretDir, Boolean.toString(tfServing.getEnableBatching())};

    logger.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);

    try {
        // Send both stdout and stderr to the same stream
      pb.redirectErrorStream(true);

      process = pb.start();

      synchronized (pb) {
        try {
            // Wait until the launcher bash script has finished
          process.waitFor();
        } catch (InterruptedException ex) {
          logger.log(Level.SEVERE,
                    "Woken while waiting for the serving server to start: {0}",
                    ex.getMessage());
        }
      }

      String pidfile = secretDir + "/tfserving/tfserving-" + port.toString() + ".pid";

      // Read the pid for TensorFlow Serving server
      String pidContents = com.google.common.io.Files.readFirstLine(
              new File(
                      pidfile), Charset.defaultCharset());
      pid = BigInteger.valueOf(Long.parseLong(pidContents));

    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Problem starting a serving server: {0}", ex.
              toString());
      if (process != null) {
        process.destroyForcibly();
      }
    }

    return new TfServingDTO(pid, port, process.exitValue());
  }

  public int killServingAsServingUser(TfServing tfServing) {

    String prog = settings.getHopsworksDomainDir() + "/bin/tfserving.sh";
    int exitValue;

    String secretDir = settings.getStagingDir() + Settings.SERVING_DIRS + tfServing.getSecret();

    String[] command = {"/usr/bin/sudo", prog, "kill", tfServing.getModelName(), tfServing.getPid().toString(),
    tfServing.getPort().toString(), secretDir};
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
      logger.log(Level.SEVERE, "Problem starting a backup: {0}", ex.
              toString());
      exitValue = -2;
    }
    return exitValue;
  }

  public void removeProject(Project project) {
    Collection<TfServing> instances = project.getTfServingCollection();

    if (instances != null) {
      //Maybe do this in a separate thread in best-effort style
      for (TfServing tfServing : instances) {
        killServingAsServingUser(tfServing);
      }
    }
  }

  public String getLogs(TfServing tfServing) {

    StringBuilder sb = new StringBuilder();

    String prog = settings.getHopsworksDomainDir() + "/bin/tfserving.sh";

    String secretDir = settings.getStagingDir() + Settings.SERVING_DIRS + tfServing.getSecret();
    String logFile = secretDir + "/tfserving/logs/" + tfServing.getModelName() + "-" + tfServing.getPort() + ".log";

    String[] command = {"/usr/bin/sudo", prog, "logs", tfServing.getHostIp(), logFile};
    logger.log(Level.SEVERE, "CMD: " + Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);

    try {
      Process process = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line + System.lineSeparator());
      }

    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Problem getting logs: {0}", ex.
              toString());
    }
    return sb.toString();
  }
}