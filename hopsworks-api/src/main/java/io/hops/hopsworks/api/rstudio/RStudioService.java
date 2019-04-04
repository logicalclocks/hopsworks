/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.api.rstudio;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.rstudio.RStudioConfigFilesGenerator;
import io.hops.hopsworks.common.dao.rstudio.RStudioDTO;
import io.hops.hopsworks.common.dao.rstudio.RStudioPaths;
import io.hops.hopsworks.common.dao.rstudio.RStudioSettings;
import io.hops.hopsworks.common.dao.rstudio.RStudioSettingsFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Ip;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.codec.digest.DigestUtils;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RStudioService {

  private final static Logger LOGGER = Logger.getLogger(RStudioService.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private Settings settings;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeFacade;
  @EJB
  private RStudioConfigFilesGenerator rstudioConfigFilesGenerator;
  @EJB
  private RStudioSettingsFacade rstudioSettingsFacade;

  private Integer projectId;
  // No @EJB annotation for Project, it's injected explicitly in ProjectService.
  private Project project;

  public RStudioService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  @POST
  @Path("/start")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response startServer(RStudioSettings rstudioSettings, @Context HttpServletRequest req,
      @Context SecurityContext sc) throws ProjectException, HopsSecurityException, ServiceException {

    Users hopsworksUser = jwtHelper.getUserPrincipal(sc);
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, hopsworksUser);
    String realName = hopsworksUser.getFname() + " " + hopsworksUser.getLname();

    HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);

    DistributedFileSystemOps dfso = dfsService.getDfsOps();

    String externalIp = Ip.getHost(req.getRequestURL().toString());

    String prog = settings.getHopsworksDomainDir() + "/bin/rstudio.sh";

    RStudioPaths rsPaths = null;
    String token = null;
    Long pid = 0l;

    int maxTries = 3;
    Process process = null;
    Integer port = 0;
    boolean started = false;

    RStudioSettings rs = rstudioSettingsFacade.findByProjectUser(projectId, hopsworksUser.getEmail());

    // kill any running servers for this user, clear cached entries
    while (!started && maxTries > 0) {
      // use pidfile to kill any running servers
      port = ThreadLocalRandom.current().nextInt(40000, 59999);
      String configSecret = DigestUtils.sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt()));
      RStudioDTO dto = null;

      rsPaths = rstudioConfigFilesGenerator.generateConfiguration(project, configSecret, hdfsUser, realName,
          hdfsLeFacade.getSingleEndpoint(), rs, port);

      String secretDir = settings.getStagingDir() + Settings.PRIVATE_DIRS + rs.getSecret();

      String logfile = rsPaths.getLogDirPath() + "/" + hdfsUser + "-" + port + ".log";

      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
          .addCommand("/usr/bin/sudo")
          .addCommand(prog)
          .addCommand("start")
          .addCommand(rsPaths.getNotebookPath())
          .addCommand(settings.getHadoopSymbolicLinkDir() + "-" + settings.getHadoopVersion())
          .addCommand(settings.getJavaHome())
          .addCommand(settings.getAnacondaProjectDir(project))
          .addCommand(port.toString())
          .addCommand(hdfsUser + "-" + port + ".log")
          .addCommand(secretDir)
          .addCommand(rsPaths.getCertificatesDir())
          .addCommand(hdfsUser)
          .redirectErrorStream(true)
          .setCurrentWorkingDirectory(new File(rsPaths.getNotebookPath()))
          .setWaitTimeout(20L, TimeUnit.SECONDS)
          .build();

      String pidfile = rsPaths.getRunDirPath() + "/rstudio.pid";
      try {
        ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
        if (processResult.getExitCode() != 0) {
          String errorMsg = "Could not start RStudio server. Exit code: " + processResult.getExitCode()
              + " Error: " + processResult.getStdout();
          LOGGER.log(Level.SEVERE, "Could not start RStudio server. Exit code: " + processResult.getExitCode()
              + " Error: " + processResult.getStdout());
          throw new IOException(errorMsg);
        }

        // Read the pid for RStudio Notebook
        String pidContents = com.google.common.io.Files.readFirstLine(
            new File(pidfile), Charset.defaultCharset());
        pid = Long.parseLong(pidContents);

        started = true;
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Problem starting a RStudio server: {0}", ex);
        if (process != null) {
          process.destroyForcibly();
        }
      }
      maxTries--;
    }

    if (!started) {
      return null;
    }

    RStudioDTO dto = new RStudioDTO(port, pid, rs.getSecret(), rsPaths.getCertificatesDir());

    try {
      HopsUtils.materializeCertificatesForUserCustomDir(project.getName(), user.getUsername(),
          settings.getHdfsTmpCertDir(), dfso, certificateMaterializer, settings, dto.getCertificatesDir());
      certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
    } catch (IOException ex) {
      Logger.getLogger(RStudioService.class.getName()).log(Level.SEVERE, null, ex);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(dto).build();
  }
}
