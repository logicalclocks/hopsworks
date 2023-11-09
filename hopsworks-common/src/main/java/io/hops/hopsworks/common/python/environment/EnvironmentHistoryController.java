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

import aQute.bnd.version.maven.ComparableVersion;
import com.google.common.collect.Sets;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.python.EnvironmentHistoryFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.history.EnvironmentDelta;
import io.hops.hopsworks.persistence.entity.python.history.LibrarySpec;
import io.hops.hopsworks.persistence.entity.python.history.LibraryUpdate;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.json.JSONArray;
import org.yaml.snakeyaml.Yaml;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
public class EnvironmentHistoryController {
  @EJB
  private DistributedFsService dfs;
  @EJB
  private  HdfsUsersController hdfsUsersController;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private EnvironmentHistoryFacade environmentHistoryFacade;

  private final String BASE_IMAGE = "BASE";

  private final String UPGRADED_KEY = "upgraded";
  private final String DOWNGRADED_KEY = "downgraded";

  public void computeDelta(Project project, Users user)
      throws ServiceDiscoveryException, ServiceException {
    String dockerImage = projectUtils.getFullDockerImageName(project, false);
    String tag = dockerImage.substring(dockerImage.lastIndexOf(":") + 1);
    Optional<EnvironmentDelta> previousEnvironmentOptional = environmentHistoryFacade.getPreviousBuild(project);
    EnvironmentDelta diff = new EnvironmentDelta(project, user, tag, new Date());
    if (!previousEnvironmentOptional.isPresent()) {
      // We are creating from the base
      diff.setPreviousDockerImage(BASE_IMAGE);
      environmentHistoryFacade.create(diff);
      return;
    }
    EnvironmentDelta previousEnvironment = previousEnvironmentOptional.get();

    Set<LibrarySpec> currentEnvInstalledLibraries = getDependencies(project, user, tag);
    Set<LibrarySpec> prevEnvInstalledLibraries = getDependencies(project, user, previousEnvironment.getDockerImage());

    diff.setInstalled(new JSONArray(getInstalled(currentEnvInstalledLibraries, prevEnvInstalledLibraries)));
    diff.setUninstalled(new JSONArray(getUninstalled(currentEnvInstalledLibraries, prevEnvInstalledLibraries)));

    Map<String, List<LibraryUpdate>> updates = getLibraryUpdates(currentEnvInstalledLibraries,
        prevEnvInstalledLibraries);
    diff.setUpgraded(new JSONArray(updates.get(UPGRADED_KEY)));
    diff.setDowngraded(new JSONArray(updates.get(DOWNGRADED_KEY)));
    diff.setPreviousDockerImage(previousEnvironment.getDockerImage());
    environmentHistoryFacade.create(diff);
  }

  public Set<LibrarySpec> getDependencies(Project project, Users user, String tag) throws ServiceException {
    ArrayList allDependencies = readEnvironment(project, user, tag);
    Set<LibrarySpec> allLibraries = new HashSet<>();
    allLibraries.addAll(getPipInstalled(allDependencies));
    allLibraries.addAll(getCondaInstalled(allDependencies));
    return allLibraries;
  }

  private Set<LibrarySpec> getPipInstalled(ArrayList allDeps) {
    Set<LibrarySpec> pipInstalled = new HashSet<>();
    //usually the pip installed are at the end
    for (int i = allDeps.size() - 1; i >= 0; i--) {
      if (allDeps.get(i) instanceof LinkedHashMap) {
        LinkedHashMap lhm = (LinkedHashMap) allDeps.get(i);
        if (lhm.containsKey("pip")) {
          pipInstalled = parseLibrariesToLibrarySpec((ArrayList<String>) lhm.get("pip"), CondaInstallType.PIP);
        }
        break;
      }
    }
    return pipInstalled;
  }

  private Set<LibrarySpec> getCondaInstalled(ArrayList allDeps) {
    Set<LibrarySpec> condaInstalled = new HashSet<>();
    allDeps.stream().forEach(dep -> {
      if (dep instanceof String) {
        condaInstalled.add(parseLibraryToLibrarySpec((String) dep, CondaInstallType.CONDA));
      }
    });
    return condaInstalled;
  }

  private ArrayList readEnvironment(Project project, Users user, String imageTag) throws ServiceException {
    Yaml yaml = new Yaml();
    String environmentYaml = readEnvironmentYamlFile(project, user, imageTag);
    LinkedHashMap data = yaml.load(environmentYaml);
    return (ArrayList) data.get("dependencies");
  }

  private List<LibrarySpec> getUninstalled(Set<LibrarySpec> current, Set<LibrarySpec> previous) {
    List<LibrarySpec> uninstalled = previous.stream()
        .filter(lib -> !current.contains(lib)).collect(Collectors.toList());
    return uninstalled;
  }

  private List<LibrarySpec> getInstalled(Set<LibrarySpec> current, Set<LibrarySpec> previous) {
    List<LibrarySpec> installed = current.stream().filter(lib -> !previous.contains(lib)).collect(Collectors.toList());
    return installed;
  }

  private Map<String, List<LibraryUpdate>> getLibraryUpdates(Set<LibrarySpec> current, Set<LibrarySpec> old) {
    List<LibraryUpdate> upgraded = new ArrayList<>();
    List<LibraryUpdate> downgraded = new ArrayList<>();
    Map<String, String> currentMap =
        current.stream().collect(Collectors.toMap(LibrarySpec::getLibrary, lib -> lib.getVersion()));
    Map<String, String> oldMap =
        old.stream().collect(Collectors.toMap(LibrarySpec::getLibrary, lib -> lib.getVersion()));
    Set<LibrarySpec> common = Sets.intersection(current, old);
    common.stream().forEach(lib -> {
      String currentVersion = currentMap.get(lib.getLibrary());
      String oldVersion = oldMap.get(lib.getLibrary());
      int compare = new ComparableVersion(currentVersion).compareTo(new ComparableVersion(oldVersion));
      if (compare > 0) {
        upgraded.add(new LibraryUpdate(lib.getLibrary(), oldVersion, currentVersion));
      } else if (compare < 0) {
        downgraded.add(new LibraryUpdate(lib.getLibrary(), oldVersion, currentVersion));
      }
    });

    return new HashMap<String, List<LibraryUpdate>>() {
      {
        put(UPGRADED_KEY, upgraded);
        put(DOWNGRADED_KEY, downgraded);
      }
    };
  }

  private Set<LibrarySpec> parseLibrariesToLibrarySpec(ArrayList<String> rawLibraries,
                                                       CondaInstallType condaInstallType) {
    Set<LibrarySpec> libraries = new HashSet<>();
    rawLibraries.stream().forEach(lib -> {
      libraries.add(parseLibraryToLibrarySpec(lib, condaInstallType));
    });
    return libraries;
  }

  private LibrarySpec parseLibraryToLibrarySpec(String library, CondaInstallType condaInstallType) {
    String[] libParts = library.split("==");
    if (condaInstallType == CondaInstallType.CONDA) {
      libParts = library.split("=");
    }
    return new LibrarySpec(libParts[0], libParts[1], condaInstallType.name());
  }

  private String readEnvironmentYamlFile(Project project, Users user, String imageTag) throws ServiceException {
    DistributedFileSystemOps dfso = null;
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    try {
      dfso = dfs.getDfsOps(hdfsUser);
      return dfso.cat(Utils.getProjectPath(project.getName())
          + Settings.PROJECT_PYTHON_ENVIRONMENT_FILE_DIR + imageTag
          + Settings.ENVIRONMENT_FILE_DELIMETER + Settings.ENVIRONMENT_FILE);
    } catch (IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ENVIRONMENT_YAML_READ_ERROR,
          Level.SEVERE, "Failed to compute history", e.getMessage(), e);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }

  public Optional<Path> getCustomCommandsFileForEnvironment(Users user, Project project, String dockerImage)
      throws ServiceException {
    DistributedFileSystemOps dfso = null;
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    try {
      dfso = dfs.getDfsOps(hdfsUser);
      Path customCommandsFilePath = new Path(Utils.getProjectPath(project.getName())
          + Settings.PROJECT_PYTHON_ENVIRONMENT_FILE_DIR
          + dockerImage
          + Settings.DOCKER_CUSTOM_COMMANDS_POST_BUILD_ARTIFACT_DIR_SUFFIX,
          Settings.DOCKER_CUSTOM_COMMANDS_FILE_NAME);
      if (dfso.exists(customCommandsFilePath)) {
        return Optional.of(customCommandsFilePath);
      }
      return Optional.empty();
    } catch (IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ENVIRONMENT_HISTORY_CUSTOM_COMMANDS_FILE_READ_ERROR,
          Level.SEVERE, "Failed to get the custom commands file path", e.getMessage(), e);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }
}
