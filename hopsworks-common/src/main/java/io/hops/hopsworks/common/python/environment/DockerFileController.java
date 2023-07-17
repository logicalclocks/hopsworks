/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.python.environment;

import com.google.common.base.Strings;
import freemarker.template.TemplateException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.TemplateEngine;
import io.hops.hopsworks.common.util.templates.python.DockerCustomCommandsTemplate;
import io.hops.hopsworks.common.util.templates.python.DockerCustomCommandsTemplateBuilder;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.jupyter.config.GitBackend;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DockerFileController {
  private static final String DOCKER_HOST_NETWORK_OPT = "--network=host";
  private static final String DOCKER_NO_CACHE_OPT = "--no-cache";
  
  @EJB
  private Settings settings;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private SecretsController secretsController;
  @EJB
  private TemplateEngine templateEngine;

  public File createTmpDir(Project project) {
    File tmpDir = new File("/tmp/docker/" + project.getName());
    tmpDir.mkdirs();
    return tmpDir;
  }
  
  public File createNewImage(File cwd, String dockerFileName, String baseImage, CondaCommands cc)
    throws ServiceException {
    String anaconda_dir = settings.getAnacondaDir();
    File home = new File(System.getProperty("user.home"));
    File condarc = new File(home, ".condarc");
    File pip = new File(home, ".pip");
    try {
      FileUtils.copyFileToDirectory(condarc, cwd);
      FileUtils.copyDirectoryToDirectory(pip, cwd);
      File dockerFile = new File(cwd, dockerFileName);
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile))) {
        writer.write("FROM " + baseImage);
        writer.newLine();
        // If new image is created from an IMPORT (.yml or requirements.txt) file
        // copy it in the image, create the env and delete it
        if (!Strings.isNullOrEmpty(cc.getEnvironmentFile())) {
          writer.write("RUN rm -f /root/.condarc");
          writer.newLine();
          // Materialize IMPORT FILE
          String environmentFilePath = cc.getEnvironmentFile();
          String environmentFile = FilenameUtils.getName(environmentFilePath);
    
          writer.write("COPY .condarc .pip " + environmentFile + " /root/");
          writer.newLine();
          copyCondaArtifactToLocal(environmentFilePath, cwd + File.separator + environmentFile);
          String anaconda_project_dir = anaconda_dir + "/envs/" + settings.getCurrentCondaEnvironment();
          if (environmentFilePath.endsWith(".yml")) {
            if (cc.getInstallJupyter()) {
              writer.write("RUN conda env update -f /root/" + environmentFile + " -n "
                + settings.getCurrentCondaEnvironment());
            } else {
              writer.write("RUN conda env create -f /root/" + environmentFile + " -p "
                + anaconda_project_dir);
            }
          } else if (environmentFilePath.endsWith("/requirements.txt")) {
            if (cc.getInstallJupyter()) {
              writer.write("RUN pip install -r /root/" + environmentFile);
            } else {
              writer.write("RUN conda create -y -p " + anaconda_project_dir
                + " python=" + settings.getDockerBaseImagePythonVersion()
                + " && pip install -r /root/" + environmentFile);
            }
          }
          writer.write(" && " + getCleanupCommand(anaconda_dir) + " && " + anaconda_dir + "/bin/conda list -n "
            + settings.getCurrentCondaEnvironment());
        }
      }
      return dockerFile;
    } catch(IOException e) {
      String errorMsg = "Failed to write docker file";
      throw new ServiceException(RESTCodes.ServiceErrorCode.LOCAL_FILESYSTEM_ERROR, Level.INFO,
        errorMsg, errorMsg, e);
    }
  }
  
  public static class BuildImageDetails {
    public final File dockerFile;
    public final ArrayList<String> dockerBuildOpts;
    public final String gitApiKeyName;
    public final String gitApiToken;
  
    public BuildImageDetails(File dockerFile, ArrayList<String> dockerBuildOpts, String gitApiKeyName,
                                String gitApiToken) {
      this.dockerFile = dockerFile;
      this.dockerBuildOpts = dockerBuildOpts;
      this.gitApiKeyName = gitApiKeyName;
      this.gitApiToken = gitApiToken;
    }
  }
  
  public BuildImageDetails installLibrary(File baseDir,
                                             String dockerFileName,
                                             String baseImage,
                                             CondaCommands cc)
    throws UserException, ServiceException {
    
    String anaconda_dir = settings.getAnacondaDir();
    String anaconda_project_dir = anaconda_dir + "/envs/" + settings.getCurrentCondaEnvironment();
    ArrayList<String> dockerBuildOpts = new ArrayList<>();
    dockerBuildOpts.add(DOCKER_HOST_NETWORK_OPT);
  
    File home = new File(System.getProperty("user.home"));
    File condarc = new File(home, ".condarc");
    File pip = new File(home, ".pip");
    try {
      FileUtils.copyFileToDirectory(condarc, baseDir);
      FileUtils.copyDirectoryToDirectory(pip, baseDir);
      File dockerFile = new File(baseDir, dockerFileName);
      String apiToken = null;
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile))) {
        writer.write("FROM " + baseImage);
        writer.newLine();
        writer.write(
          "RUN --mount=type=bind,source=.condarc,target=/root/.condarc"
            + " --mount=type=bind,source=.pip,target=/root/.pip ");
        switch (cc.getInstallType()) {
          case CONDA:
            String condaLib;
            if (cc.getVersion().equals(Settings.UNKNOWN_LIBRARY_VERSION)) {
              condaLib = cc.getLib();
              dockerBuildOpts.add(DOCKER_NO_CACHE_OPT);
            } else {
              condaLib = cc.getLib() + "=" + cc.getVersion();
            }
            writer.write(anaconda_dir + "/bin/conda install -y -n " + settings.getCurrentCondaEnvironment()
              + " -c " + cc.getChannelUrl() + " " + condaLib);
            break;
          case PIP:
            String pipLib;
            if (cc.getVersion().equals(Settings.UNKNOWN_LIBRARY_VERSION)) {
              pipLib = cc.getLib();
              dockerBuildOpts.add(DOCKER_NO_CACHE_OPT);
            } else {
              pipLib = cc.getLib() + "==" + cc.getVersion();
            }
            writer.write(anaconda_project_dir + "/bin/pip install --upgrade " + pipLib);
            break;
          case EGG:
            String eggName = cc.getLib();
            String localEggPath = baseDir + File.separator + eggName;
            copyCondaArtifactToLocal(cc.getArg(), localEggPath);
            writer.write("--mount=type=bind,source=" + eggName + ",target=/root/" + eggName + " ");
            writer.write(anaconda_project_dir + "/bin/easy_install --upgrade /root/" + eggName);
            break;
          case WHEEL:
            String wheelName = cc.getLib();
            String localWheelPath = baseDir + File.separator + wheelName;
            copyCondaArtifactToLocal(cc.getArg(), localWheelPath);
            writer.write("--mount=type=bind,source=" + wheelName + ",target=/root/" + wheelName + " ");
            writer.write(anaconda_project_dir + "/bin/pip install --upgrade /root/" + wheelName);
            break;
          case REQUIREMENTS_TXT:
            String requirementsName = cc.getLib();
            String localRequirementsName = baseDir + File.separator + requirementsName;
            copyCondaArtifactToLocal(cc.getArg(), localRequirementsName);
            writer.write("--mount=type=bind,source=" + requirementsName + ",target=/root/" + requirementsName + " ");
            writer.write(anaconda_project_dir + "/bin/pip install -r /root/" + requirementsName);
            break;
          case ENVIRONMENT_YAML:
            String environmentsName = cc.getLib();
            String localEnvironmentsName = baseDir + File.separator + environmentsName;
            copyCondaArtifactToLocal(cc.getArg(), localEnvironmentsName);
            writer.write("--mount=type=bind,source=" + environmentsName + ",target=/root/" + environmentsName + " ");
            writer.write(anaconda_dir + "/bin/conda env update -f /root/" + environmentsName + " -n "
              + settings.getCurrentCondaEnvironment());
            break;
          case GIT:
            if (cc.getGitBackend() != null && cc.getGitApiKeyName() != null) {
              apiToken = secretsController.get(cc.getUserId(), cc.getGitApiKeyName()).getPlaintext();
              URL repoUrl = new URL(cc.getArg());
              if (cc.getGitBackend().equals(GitBackend.GITHUB)) {
                writer.write(anaconda_project_dir + "/bin/pip install --upgrade 'git+https://"
                  + apiToken + ":x-oauth-basic@" + repoUrl.getHost() + repoUrl.getPath() + "'");
              } else if (cc.getGitBackend().equals(GitBackend.GITLAB)) {
                writer.write(anaconda_project_dir + "/bin/pip install --upgrade 'git+https://oauth2:"
                  + apiToken + "@" + repoUrl.getHost() + repoUrl.getPath() + "'");
              }
            } else {
              writer.write(anaconda_project_dir + "/bin/pip install --upgrade 'git+" + cc.getArg() + "'");
            }
            dockerBuildOpts.add(DOCKER_NO_CACHE_OPT);
            break;
          case CUSTOM_COMMANDS:
            copyCustomCommandsArtifactsToLocal(baseDir.getPath(), cc);
            customCommandsDockerfile(writer, cc, baseDir);
            break;
          case ENVIRONMENT:
          default:
            throw new UnsupportedOperationException("install type unknown: " + cc.getInstallType());
        }
        //Installing faulty libraries like broken .egg files can cause the list operation to fail
        //As we find library names and versions using that command we need to make sure it does not break
        writer.write(" && " + getCleanupCommand(anaconda_dir) + " && " + anaconda_dir + "/bin/conda list -n "
          + settings.getCurrentCondaEnvironment());
      }
      return new BuildImageDetails(dockerFile, dockerBuildOpts, cc.getGitApiKeyName(), apiToken);
    } catch(IOException e) {
      String errorMsg = "Failed to write docker file";
      throw new ServiceException(RESTCodes.ServiceErrorCode.LOCAL_FILESYSTEM_ERROR, Level.INFO,
        errorMsg, errorMsg, e);
    }
  }
  
  public File uninstallLibrary(File baseDir, String baseImage, CondaCommands cc) throws ServiceException {
    String anaconda_dir = settings.getAnacondaDir();
    String anaconda_project_dir = anaconda_dir + "/envs/" + settings.getCurrentCondaEnvironment();
    File dockerFile = new File(baseDir, "dockerFile_" + cc.getProjectId().getName());
    File home = new File(System.getProperty("user.home"));
    try {
      FileUtils.copyFileToDirectory(new File(home, ".condarc"), baseDir);
      FileUtils.copyDirectoryToDirectory(new File(home, ".pip"), baseDir);
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(dockerFile))) {
        writer.write("FROM " + baseImage + "\n");
        writer.newLine();
        writer.write(
          "RUN --mount=type=bind,source=.condarc,target=/root/.condarc"
            + " --mount=type=bind,source=.pip,target=/root/.pip ");
        switch (cc.getInstallType()) {
          case CONDA:
            writer.write(anaconda_dir + "/bin/conda remove -y -n " +
              settings.getCurrentCondaEnvironment() + " " + cc.getLib() + " || true\n");
            break;
          case PIP:
            writer.write(anaconda_project_dir + "/bin/pip uninstall -y " + cc.getLib() + " || true\n");
            break;
          case ENVIRONMENT:
          default:
            throw new UnsupportedOperationException("install type unknown: " + cc.getInstallType());
        }
      }
      return dockerFile;
    } catch(IOException e) {
      String errorMsg = "Failed to write docker file";
      throw new ServiceException(RESTCodes.ServiceErrorCode.LOCAL_FILESYSTEM_ERROR, Level.INFO,
        errorMsg, errorMsg, e);
    }
  }
  
  private void copyCondaArtifactToLocal(String source, String destPath) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      dfso.copyToLocal(source, destPath);
    }
    finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

  public void customCommandsDockerfile(BufferedWriter writer, CondaCommands cc, File baseDir) throws IOException {
    // 1. Configure the Docker commands from the template
    File bashScript = new File(cc.getCustomCommandsFile());
    DockerCustomCommandsTemplate template = DockerCustomCommandsTemplateBuilder.newBuilder()
        .setCommandScript(bashScript.getName())
        .build();
    File dockerCommandsFile = new File(baseDir, Settings.DOCKER_CUSTOM_COMMANDS_GENERATED_FILE_NAME);
    if (!dockerCommandsFile.exists()) {
      try (Writer out = new FileWriter(dockerCommandsFile, false)) {
        Map<String, Object> dataModel = new HashMap<>(1);
        dataModel.put("args", template);
        try {
          templateEngine.template(Settings.DOCKER_CUSTOM_COMMANDS_TEMPLATE_NAME, dataModel, out);
        } catch (TemplateException ex) {
          throw new IOException(ex);
        }
      }
    }
    String dockerCommands = new String(Files.readAllBytes(Paths.get(dockerCommandsFile.toString())));
    writer.write("\n\n" + dockerCommands + "\n\n");
    // dummy command so we concatenate with other conda commands we run at the end
    writer.write("RUN echo '' ");
    try {
      Files.delete(Paths.get(dockerCommandsFile.toString()));
    } catch (IOException e) {
      // we can fail silently
    }
  }

  private void copyCustomCommandsArtifactsToLocal(String destPath, CondaCommands cc) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      ArrayList<String> artifacts = new ArrayList<>();
      artifacts.add(cc.getCustomCommandsFile());
      if (!Strings.isNullOrEmpty(cc.getArg())) {
        artifacts.addAll(Arrays.asList(cc.getArg().split(",")));
      }
      for (String artifact: artifacts) {
        dfso.copyToLocal(artifact, destPath);
      }
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }
  
  private String getCleanupCommand(String anaconda_dir) {
    return anaconda_dir + "/bin/conda clean -afy && rm -rf ~/.cache && rm -rf /usr/local/share/.cache";
  }
}
