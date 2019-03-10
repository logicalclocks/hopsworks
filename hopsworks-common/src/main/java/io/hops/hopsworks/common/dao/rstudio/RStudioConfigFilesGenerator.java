/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.dao.rstudio;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.util.ConfigFileGenerator;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.io.FileUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to generate the Configuration Files for a RStudio Server
 */
@Stateless
public class RStudioConfigFilesGenerator {

  private static final Logger LOGGER = Logger.getLogger(RStudioConfigFilesGenerator.class.
      getName());
//  public static final String RSTUDIO_SERVER_CONFIG = "rstudio.conf";
  public static final String RSTUDIO_SESSION_CONFIG = "rsession.conf";
  public static final String RSTUDIO_SESSION_SCRIPT = "rsession.sh";
  @EJB
  private Settings settings;

  public RStudioPaths generateConfiguration(Project project, String secretConfig, String hdfsUser, String realName,
      String nameNodeEndpoint, RStudioSettings rs, Integer port)
      throws ServiceException {
    boolean newDir = false;

    RStudioPaths jP = new RStudioPaths(settings.getJupyterDir(), project.getName(), hdfsUser, secretConfig);

    try {
      newDir = createRStudioDirs(jP);
      createConfigFiles(jP.getConfDirPath(), jP.getKernelsDir(), hdfsUser, realName, port, rs);
    } catch (Exception e) {
      if (newDir) { // if the folder was newly created delete it
        removeProjectUserDirRecursive(jP);
      }
      LOGGER.log(Level.SEVERE,
          "Error in initializing RStudioConfig for project: {0}. {1}",
          new Object[]{project.getName(), e});

      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_ADD_FAILURE, Level.SEVERE, null, e.getMessage(), e);
    }

    return jP;
  }

  //returns true if the project dir was created
  private boolean createRStudioDirs(RStudioPaths jp) throws IOException {
    File projectDir = new File(jp.getProjectUserPath());
    projectDir.mkdirs();
    File baseDir = new File(jp.getNotebookPath());
    baseDir.mkdirs();
    // Set owner persmissions
    Set<PosixFilePermission> xOnly = new HashSet<>();
    xOnly.add(PosixFilePermission.OWNER_WRITE);
    xOnly.add(PosixFilePermission.OWNER_READ);
    xOnly.add(PosixFilePermission.OWNER_EXECUTE);
    xOnly.add(PosixFilePermission.GROUP_WRITE);
    xOnly.add(PosixFilePermission.GROUP_EXECUTE);

    Set<PosixFilePermission> perms = new HashSet<>();
    //add owners permission
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    //add group permissions
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.GROUP_WRITE);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    //add others permissions
    perms.add(PosixFilePermission.OTHERS_READ);
    perms.add(PosixFilePermission.OTHERS_EXECUTE);

    Files.setPosixFilePermissions(Paths.get(jp.getNotebookPath()), perms);
    Files.setPosixFilePermissions(Paths.get(jp.getProjectUserPath()), xOnly);

    new File(jp.getConfDirPath() + "/custom").mkdirs();
    new File(jp.getRunDirPath()).mkdirs();
    new File(jp.getLogDirPath()).mkdirs();
    new File(jp.getCertificatesDir()).mkdirs();
    new File(jp.getKernelsDir()).mkdirs();
    return true;
  }

  // returns true if one of the conf files were created anew 
  private boolean createConfigFiles(String confDirPath, String certsDir, String hdfsUser, String realName,
      Integer port, RStudioSettings rs)
      throws IOException, ServiceException {
//    File rstudio_conf = new File(confDirPath + RSTUDIO_SERVER_CONFIG);
    File rsession_conf = new File(confDirPath + RSTUDIO_SESSION_CONFIG);
    File rsessionSh = new File(confDirPath + RSTUDIO_SESSION_SCRIPT);
    boolean createdRStudio = false;
    boolean rsessionConf = false;
    boolean createdRsessionScript = false;

    if (!rsession_conf.exists()) {

      StringBuilder rstudioSessionConf = ConfigFileGenerator.instantiateFromTemplate(
          ConfigFileGenerator.RSTUDIO_SESSION_CONF,
          "rstudio_session_timeout", new String("10000"),
          "scratch_dir", rs.getBaseDir(),
          "hdfs_user", hdfsUser,
          "port", port.toString(),
          "hadoop_home", this.settings.getHadoopSymbolicLinkDir(),
          "hdfs_home", this.settings.getHadoopSymbolicLinkDir(),
          "secret_dir", this.settings.getStagingDir() + Settings.PRIVATE_DIRS + rs.getSecret()
      );
      createdRStudio = ConfigFileGenerator.createConfigFile(rsession_conf, rstudioSessionConf.toString());
    }
    if (!rsessionSh.exists()) {

      StringBuilder ressionShellScript = ConfigFileGenerator.
          instantiateFromTemplate(
              ConfigFileGenerator.RSTUDIO_SESSION_SCRIPT,
              "scratch_dir", this.settings.getStagingDir() + Settings.PRIVATE_DIRS + rs.getSecret(),
              "hadoop_home", this.settings.getHadoopSymbolicLinkDir(),
              "java_home", this.settings.getJavaHome(),
              "hadoop_user_name", hdfsUser,
              "certs_dir", certsDir
          );
      createdRsessionScript = ConfigFileGenerator.createConfigFile(rsessionSh, ressionShellScript.toString());
    }

    // Add this local file to 'spark: file' to copy it to hdfs and localize it.
    return createdRStudio || rsessionConf || createdRsessionScript;
  }

  private void removeProjectUserDirRecursive(RStudioPaths jp) {
    try {
      FileUtils.deleteDirectory(new File(jp.getProjectUserPath()));
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Could not delete RStudio directory: " + jp.getProjectUserPath(), e);
    }
  }
}
