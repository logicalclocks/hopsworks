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

package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.integrations.LocalhostStereotype;
import io.hops.hopsworks.common.jupyter.TokenGenerator;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * *
 * This class wraps a bash script with sudo rights that can be executed by the node['hopsworks']['user'].
 * /srv/hops/domains/domain1/bin/jupyter.sh
 * The bash script has several commands with parameters that can be exceuted.
 * This class provides a Java interface for executing the commands.
 */
@Stateless
@LocalhostStereotype
public class JupyterProcessMgr implements JupyterManager {

  private static final Logger LOGGER = Logger.getLogger(JupyterProcessMgr.class.getName());
  private static final int TOKEN_LENGTH = 48;
  private static final String JUPYTER_HOST_TEMPLATE = "http://%s:%d";
  private static final String PING_PATH = "/hopsworks-api/jupyter/%d/api/status";

  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeFacade;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterConfigFilesGenerator jupyterConfigFilesGenerator;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private HttpClient httpClient;
  
  private String jupyterHost;
  
  @PostConstruct
  public void init() {
    jupyterHost = settings.getJupyterHost();
  }

  @PreDestroy
  public void preDestroy() {
  }
  
  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public JupyterDTO startJupyterServer(Project project, String secretConfig, String hdfsUser, Users user,
    JupyterSettings js, String allowOrigin) throws ServiceException {
    
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    
    Integer port = ThreadLocalRandom.current().nextInt(40000, 59999);
    String realName = user.getFname() + " " + user.getLname();
    JupyterPaths jp = jupyterConfigFilesGenerator.generateConfiguration(project, secretConfig, hdfsUser, realName,
        hdfsLeFacade.getRPCEndpoint(), js, port, allowOrigin);
    String secretDir = settings.getStagingDir() + Settings.PRIVATE_DIRS + js.getSecret();

    String token = TokenGenerator.generateToken(TOKEN_LENGTH);
    Long pid = 0l;
    
    // The Jupyter Notebook is running at: http://localhost:8888/?token=c8de56fa4deed24899803e93c227592aef6538f93025fe01
    int maxTries = 5;

    // kill any running servers for this user, clear cached entries
    while (maxTries > 0) {

      // use pidfile to kill any running servers
      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
          .addCommand("/usr/bin/sudo")
          .addCommand(prog)
          .addCommand("start")
          .addCommand(jp.getNotebookPath())
          .addCommand(settings.getHadoopSymbolicLinkDir() + "-" + settings.getHadoopVersion())
          .addCommand(settings.getJavaHome())
          .addCommand(settings.getAnacondaProjectDir(project))
          .addCommand(port.toString())
          .addCommand(hdfsUser + "-" + port + ".log")
          .addCommand(secretDir)
          .addCommand(jp.getCertificatesDir())
          .addCommand(hdfsUser)
          .addCommand(token)
          .addCommand(js.getMode().getValue())
          .redirectErrorStream(true)
          .setCurrentWorkingDirectory(new File(jp.getNotebookPath()))
          .setWaitTimeout(20L, TimeUnit.SECONDS)
          .build();


      String pidfile = jp.getRunDirPath() + "/jupyter.pid";
      try {
        ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
        if (processResult.getExitCode() != 0) {
          String errorMsg = "Could not start Jupyter server. Exit code: " + processResult.getExitCode()
              + " Error: " + processResult.getStdout();
          LOGGER.log(Level.SEVERE, errorMsg);
          throw new IOException(errorMsg);
        }
        // Read the pid for Jupyter Notebook
        String pidContents = com.google.common.io.Files.readFirstLine(
            new File(pidfile), Charset.defaultCharset());
        pid = Long.parseLong(pidContents);

        return new JupyterDTO(port, token, pid, secretConfig, jp.getCertificatesDir());
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Problem executing shell script to start Jupyter server", ex);
        maxTries--;
      }
    }

    String errorMsg = "Failed to start Jupyter";
    throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.SEVERE, errorMsg,
      errorMsg + " for project " + project);
  }

  @Override
  public void waitForStartup(Project project, Users user) throws TimeoutException {
    // Nothing to do as the start is blocking
  }

  public void stopOrphanedJupyterServer(Long pid, Integer port) throws ServiceException {
    stopJupyterServer(null, null, "Orphaned", "", pid, port);
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void stopJupyterServer(Project project, Users user, String hdfsUsername, String jupyterHomePath, Long pid,
      Integer port) throws ServiceException {
    if (jupyterHomePath == null || pid == null || port == null) {
      throw new IllegalArgumentException("Invalid arguments when stopping the Jupyter Server.");
    }
    // 1. Remove jupyter settings from the DB for this notebook first. If this fails, keep going to kill the notebook
    try {
      jupyterFacade.remove(hdfsUsername, port);
    } catch (Exception e) {
      LOGGER.severe("Problem when removing jupyter notebook entry from jupyter_project table: " + jupyterHomePath);
    }

    // 2. Then kill the jupyter notebook server. If this step isn't 
    String prog = settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    if (jupyterHomePath.isEmpty()) {
      jupyterHomePath = "''";
    }
    int exitValue = 0;
    Integer id = 1;
    
    ProcessDescriptor.Builder pdBuilder = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("kill")
        .addCommand(jupyterHomePath)
        .addCommand(pid.toString())
        .addCommand(port.toString())
        .setWaitTimeout(10L, TimeUnit.SECONDS);
    
    if (!LOGGER.isLoggable(Level.FINE)) {
      pdBuilder.ignoreOutErrStreams(true);
    }
    
    try {
      ProcessResult processResult = osProcessExecutor.execute(pdBuilder.build());
      LOGGER.log(Level.FINE, processResult.getStdout());
      exitValue = processResult.getExitCode();
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_STOP_ERROR, Level.SEVERE, "exitValue: " + exitValue,
          ex.getMessage(), ex);
    }
    if (exitValue != 0) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_STOP_ERROR, Level.SEVERE,
        "exitValue: " + exitValue);
    }

  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void projectCleanup(Project project) {
    projectCleanup(settings, LOGGER, osProcessExecutor, project);
  }
  
  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public boolean ping(JupyterProject jupyterProject) {
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST_TEMPLATE, jupyterHost, jupyterPort));
    String pingPath = String.format(PING_PATH, jupyterPort);
    try {
      URI authPath = new URIBuilder(pingPath).addParameter("token", jupyterProject.getToken()).build();
      HttpGet httpRequest = new HttpGet(authPath);
      return httpClient.execute(host, httpRequest, new ResponseHandler<Boolean>() {
        @Override
        public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
          int status = response.getStatusLine().getStatusCode();
          return status == HttpStatus.SC_OK;
        }
      });
    } catch (URISyntaxException ex) {
      LOGGER.log(Level.SEVERE, "Could not parse URI to ping Jupyter server", ex);
      return false;
    } catch (IOException ex) {
      return false;
    }
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public List<JupyterProject> getAllNotebooks() {
    List<JupyterProject> allNotebooks = jupyterFacade.getAllNotebookServers();

    executeJupyterCommand("list");

    File file = new File(Settings.JUPYTER_PIDS);

    List<Long> pidsRunning = new ArrayList<>();
    try {
      Scanner scanner = new Scanner(file);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        pidsRunning.add(Long.parseLong(line));
      }
    } catch (FileNotFoundException e) {
      LOGGER.warning("Invalid pids in file: " + Settings.JUPYTER_PIDS);
    }

    List<Long> pidsOrphaned = new ArrayList<>();
    pidsOrphaned.addAll(pidsRunning);

    for (Long pid : pidsRunning) {
      boolean foundPid = false;
      for (JupyterProject jp : allNotebooks) {
        if (pid == jp.getPid()) {
          foundPid = true;
        }
        if (foundPid) {
          pidsOrphaned.remove(pid);
        }
      }
    }

    for (Long pid : pidsOrphaned) {
      JupyterProject jp = new JupyterProject();
      jp.setPid(pid);
      jp.setPort(0);
      jp.setHdfsUserId(-1);
      allNotebooks.add(jp);
    }
    file.deleteOnExit();

    return allNotebooks;
  }

  private int executeJupyterCommand(String... args) {
    if (args == null || args.length == 0) {
      return -99;
    }
    int exitValue;
    Integer id = 1;
    String prog = this.settings.getHopsworksDomainDir() + "/bin/jupyter.sh";
    
    ProcessDescriptor.Builder pdBuilder = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog);
    for (String arg : args) {
      pdBuilder.addCommand(arg);
    }
    pdBuilder.setWaitTimeout(20L, TimeUnit.SECONDS);
    if (!LOGGER.isLoggable(Level.FINE)) {
      pdBuilder.ignoreOutErrStreams(true);
    }
    
    try {
      ProcessResult processResult = osProcessExecutor.execute(pdBuilder.build());
      LOGGER.log(Level.FINE, processResult.getStdout());
      exitValue = processResult.getExitCode();
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE,
          "Problem checking if Jupyter Notebook server is running: {0}", ex);
      exitValue = -2;
    }
    return exitValue;
  }
}
