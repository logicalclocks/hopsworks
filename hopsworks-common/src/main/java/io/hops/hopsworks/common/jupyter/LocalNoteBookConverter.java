/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.jupyter;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.integrations.LocalhostStereotype;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.system.job.SystemJobStatus;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.codec.digest.DigestUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@LocalhostStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LocalNoteBookConverter implements NoteBookConverter {
  private static final Logger LOGGER = Logger.getLogger(LocalNoteBookConverter.class.getName());
  @EJB
  private Settings settings;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private OSProcessExecutor osProcessExecutor;

  @Override
  public SystemJobStatus convertIPythonNotebook(Project project, Users user, String notebookPath, String pyPath,
    NotebookConversion notebookConversion)  throws ServiceException {

    File baseDir = new File(settings.getStagingDir() + "/" + Settings.CONVERSION_DIR);
    if(!baseDir.exists()){
      baseDir.mkdir();
    }
    File conversionDir = new File(baseDir, DigestUtils.
      sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt())));
    conversionDir.mkdir();

    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    try{
      String prog = settings.getSudoersDir() + "/convert-ipython-notebook.sh";
      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand(notebookPath)
        .addCommand(hdfsUser)
        .addCommand(settings.getAnacondaProjectDir())
        .addCommand(pyPath)
        .addCommand(conversionDir.getAbsolutePath())
        .addCommand(notebookConversion.name())
        .addCommand(projectUtils.getFullDockerImageName(project, true))
        .setWaitTimeout(120L, TimeUnit.SECONDS) //on a TLS VM the timeout needs to be greater than 20s
        .redirectErrorStream(true)
        .build();

      LOGGER.log(Level.FINE, processDescriptor.toString());
      certificateMaterializer.
        materializeCertificatesLocalCustomDir(user.getUsername(), project.getName(), conversionDir.getAbsolutePath());
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (!processResult.processExited() || processResult.getExitCode() != 0) {
        LOGGER.log(Level.WARNING, "error code: " + processResult.getExitCode(), "Failed to convert "
          + notebookPath + "\nstderr: " + processResult.getStderr() + "\nstdout: " + processResult.getStdout());
        throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, Level.SEVERE,
          "Failed to convert ipython notebook to " + notebookConversion + " file", "Failed to convert " + notebookPath
          + "\nstderr: " + processResult.getStderr()
          + "\nstdout: " + processResult.getStdout());
      }
      String stdOut = processResult.getStdout();
      SystemJobStatus status = new SystemJobStatus(0);
      status.setLog(stdOut);
      if(notebookConversion.equals(NotebookConversion.HTML)) {
        status.setLog(getHtmlFromLog(stdOut));
      }
      return status;
    } catch (IOException | ServiceDiscoveryException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.IPYTHON_CONVERT_ERROR, Level.SEVERE, null, ex.getMessage(),
        ex);
    } finally {
      certificateMaterializer.removeCertificatesLocalCustomDir(user.getUsername(), project.getName(), conversionDir.
        getAbsolutePath());
    }
  }
}
