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
package io.hops.hopsworks.common.dao.featurestore;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetType;
import io.hops.hopsworks.common.dao.featurestore.app.FeaturestoreUtilJobDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnectorController;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.dao.user.security.secrets.Secret;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretId;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.VisibilityType;
import io.hops.hopsworks.common.featorestore.FeaturestoreConstants;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_store table and required business logic
 */
@Stateless
public class FeaturestoreController {

  private static final Logger LOGGER = Logger.getLogger(FeaturestoreController.class.getName());

  private static final String FEATURESTORE_UTIL_ARGS_PATH = Path.SEPARATOR + Settings.DIR_ROOT + Path.SEPARATOR
      + "%s" + Path.SEPARATOR + FeaturestoreConstants.FEATURESTORE_UTIL_4J_ARGS_DATASET + Path.SEPARATOR + "%s";
  private static final String HDFS_FILE_PATH = "hdfs://%s";
  private static JAXBContext featurestoreUtilJobArgsJaxbContext = null;
  private static Marshaller featurestoreUtilJobArgsMarshaller = null;
  @EJB
  private SecretsFacade secretsFacade;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private FeaturestoreJdbcConnectorController featurestoreJdbcConnectorController;
  @EJB
  private FeaturestoreHopsfsConnectorController featurestoreHopsfsConnectorController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService distributedFsService;
  @EJB
  private Settings settings;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private SecurityUtils securityUtils;
  @EJB
  private SecretsController secretsController;
  @EJB
  private ProjectTeamFacade projectTeamBean;

  /**
   * Retrieves a list of all featurestores for a particular project
   *
   * @param project
   * the project to retrieve featurestores for
   * @return a list of DTOs for the featurestores
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<FeaturestoreDTO> getFeaturestoresForProject(Project project) {
    List<Featurestore> featurestores = getProjectFeaturestores(project);
    return featurestores.stream().map(fs -> convertFeaturestoretoDTO(fs)).collect(Collectors.toList());
  }

  /**
   * Helper function that lists all featurestores in the project (including featurestores shared with the project)
   *
   * @param project
   * the project to list featurestores for
   * @return a list of featurestore entities
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private List<Featurestore> getProjectFeaturestores(Project project) {
    Collection<Dataset> dsInProject = project.getDatasetCollection();
    Collection<Dataset> featurestoresDsInproject = dsInProject.stream().filter(ds -> ds.getType()
        == DatasetType.FEATURESTORE).collect(Collectors.toList());
    return featurestoresDsInproject.stream().map(ds -> ds.getFeaturestore()).collect(Collectors.toList());
  }

  /**
   * @param args
   * add|rm|update, databasename, db_user, [db_passwords_file|
   * @return exist status of the local process that calls the DB
   */
  private int onlineFeatureStoreDB(String... args) {
    int exitValue;
    String prog = this.settings.getHopsworksDomainDir() + "/bin/featurestore-online-db.sh";

    ProcessDescriptor.Builder pdBuilder = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog);

    for (String arg : args) {
      if (arg != null) {
        pdBuilder.addCommand(arg);
      }
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
    LOGGER.log(Level.INFO, "{0} - exit value: {1}", new Object[]{pdBuilder.toString(), exitValue});

    return exitValue;
  }

  /**
   * @param project
   * project of the new featurestore
   * @param user
   * user creating the new featurestore
   * @return 0 if success, non-zero for error
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public int addUserOnlineFeatureStoreDB(Project project, Users user) {

    if (!settings.isOnlineFeaturestore())
      return 1;
    
    String role = getRole(project, user);
    String onlineFsPassword = securityUtils.generateSecureRandomString();
    String dbuser = onlineDbUsername(project, user);
    SecretId id = new SecretId(user.getUid(), dbuser);
    Secret storedSecret = secretsFacade.findById(id);
    if (storedSecret == null) {
      try {
        secretsController.add(user, dbuser, onlineFsPassword, VisibilityType.PRIVATE, project.getId());

      } catch (UserException e) {
        LOGGER.log(Level.SEVERE, "Problem adding online featurestore password to hopsworks secretsmgr");
        return -33;
      }
    }
    Set<PosixFilePermission> perms = new HashSet<>();
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    java.nio.file.Path path = null;
    try {
      path = java.nio.file.Paths.get("/tmp/" + dbuser);
      if (!path.toFile().exists()) {
        if (!path.toFile().createNewFile()) {
          LOGGER.log(Level.SEVERE, "Problem creating file: " + path);
          return -22;
        }
      }
      PrintWriter out = new PrintWriter(path.toFile());
      out.println(onlineFsPassword);
      out.flush();
      out.close();

      Files.setPosixFilePermissions(path, perms);

    } catch (IOException e) {
      e.printStackTrace();
      return -11;
    }

    String[] args = new String[5];
    args[0] = "add";
    args[1] = project.getName();
    args[2] = onlineDbUsername(project, user);
    args[3] = role;
    args[4] = path.toString();
    return onlineFeatureStoreDB(args);
  }

  private String getRole(Project project, Users user) {
    String role = projectTeamBean.findCurrentRole(project, user);

    // The role name has a space in it, and i don't want a space as i will pass this role as an argument
    // to the bash script to create the online DB user. Convert role to string without spaces.
    if (role.compareToIgnoreCase(AllowedRoles.DATA_OWNER) == 0) {
      role = "DATA_OWNER";
    } else if (role.compareToIgnoreCase(AllowedRoles.DATA_SCIENTIST) == 0) {
      role = "DATA_SCIENTIST";
    }
    return role;
  }

  public String onlineDbUsername(Project project, Users user) {
    return onlineDbUsername(project.getName(), user.getUsername());
  }

  /**
   * The mysql username can be at most 32 characters in length. 
   * Clip the username to 32 chars if it is longer than 32.
   * 
   * @param project projectname
   * @param user username
   * @return 
   */
  private String onlineDbUsername(String project, String user) {
    String username = project + "_" + user;
    if (username.length() > 32) {
      username = username.substring(0, 31);
    }
    return username;
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public int updateUserOnlineFeatureStoreDB(Project project, Users user) {
    if (!settings.isOnlineFeaturestore()) {
      return 1;
    }    
    String role = getRole(project, user);
    String[] args = new String[5];
    args[0] = "update";
    args[1] = project.getName();
    args[2] = onlineDbUsername(project, user);
    args[3] = role;
    return onlineFeatureStoreDB(args);
  }

  private int rmOnlineFeatureStore(String project, String user) {
    String[] args = new String[5];
    args[0] = "rm";
    args[1] = project;
    if (user.compareTo("all") == 0) {
      args[2] = user;
      
    } else {
      args[2] = onlineDbUsername(project, user);
    }
    return onlineFeatureStoreDB(args);
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public int rmUserOnlineFeatureStore(String project, Users user) {
    if (!settings.isOnlineFeaturestore()) {
      return 1;
    }
    SecretId id = new SecretId(user.getUid(), onlineDbUsername(project, user.getUsername()));
    secretsFacade.deleteSecret(id);
    return rmOnlineFeatureStore(project, user.getUsername());
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public int dropOnlineFeatureStore(String project) {
    if (!settings.isOnlineFeaturestore()) {
      return 1;
    }    
    List<Secret> secrets = secretsFacade.findAll();
    for (Secret s : secrets) {
      if (s.getId().getName().startsWith(project + "_")) {
        secretsFacade.deleteSecret(s.getId());
      }
    }
    
    return rmOnlineFeatureStore(project, "all");
  }

  /**
   * Retrieves a featurestore for a project with a specific name
   *
   * @param project
   * the project to retrieve featurestores for
   * @param featurestoreName
   * the name of the featurestore
   * @return a list of DTOs for the featurestores
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturestoreDTO getFeaturestoreForProjectWithName(Project project, String featurestoreName)
      throws FeaturestoreException {
    List<Featurestore> featurestores = getProjectFeaturestores(project);
    List<FeaturestoreDTO> featurestoreDTOs = featurestores.stream().map(fs -> convertFeaturestoretoDTO(fs)).collect(
        Collectors.toList());
    List<FeaturestoreDTO> featurestoresDTOWithName = featurestoreDTOs.stream().filter(fs -> fs.getFeaturestoreName().
        equals(featurestoreName))
        .collect(Collectors.toList());
    if (featurestoresDTOWithName.size() != 1) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND,
          Level.FINE, "featurestoreName: " + featurestoreName + " , project: " + project.getName());
    }
    //Featurestore name corresponds to Hive databases so uniqueness is enforced by Hive
    return featurestoresDTOWithName.get(0);
  }

  /**
   * Gets a featurestore with a particular featurestoreId from the list of featurestores for this project
   *
   * @param project
   * the project to look for the featurestore in
   * @param featurestoreId
   * the featurestoreId of the featurestore
   * @return a DTO representation of the featurestore
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturestoreDTO getFeaturestoreForProjectWithId(Project project, Integer featurestoreId)
      throws FeaturestoreException {
    List<Featurestore> featurestores = getProjectFeaturestores(project);
    List<Featurestore> featurestoresWithId = featurestores.stream().filter(fs -> fs.getId().equals(featurestoreId)).
        collect(Collectors.toList());
    if (featurestoresWithId.size() != 1) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND,
          Level.FINE, "featurestoreId: " + featurestoreId + " , project: " + project.getName());
    }
    return convertFeaturestoretoDTO(featurestoresWithId.get(0));
  }

  /**
   * Retrieves a featurestore with a particular Id from the database
   *
   * @param id
   * the id of the featurestore
   * @return featurestore entity with the given id
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Featurestore getFeaturestoreWithId(Integer id) throws FeaturestoreException {
    Featurestore featurestore = featurestoreFacade.findById(id);
    if (featurestore == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND,
          Level.FINE, "featurestoreId: " + id);
    }
    return featurestore;
  }

  /**
   * Creates a new featurestore in the database
   *
   * @param project
   * project of the new featurestore
   * @param user
   * user creating the new featurestore
   * @param featurestoreName
   * the name of the new featurestore
   * @param trainingDatasetsFolder
   * the Hopsworks dataset where training datasets are stored by default
   * @return the created featurestore
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Featurestore createProjectFeatureStore(Project project, Users user, String featurestoreName,
      Dataset trainingDatasetsFolder) throws FeaturestoreException {

    //Get HiveDbId for the newly created Hive featurestore DB
    Long hiveDbId = featurestoreFacade.getHiveDatabaseId(featurestoreName);
    //Store featurestore metadata in Hopsworks
    Featurestore featurestore = new Featurestore();
    featurestore.setProject(project);
    featurestore.setHiveDbId(hiveDbId);
    featurestore.setCreated(new Date());
    featurestoreFacade.persist(featurestore);
    activityFacade.persistActivity(ActivityFacade.CREATED_FEATURESTORE + featurestoreName, project, project.getOwner(),
        ActivityFlag.SERVICE);
    featurestoreJdbcConnectorController.createDefaultJdbcConnectorForFeaturestore(featurestore,
        getFeaturestoreDbName(project), "JDBC connection to Hopsworks Project Feature Store Hive Database");
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR + getFeaturestoreDbName(project),
        project, project.getOwner(), ActivityFlag.SERVICE);
    featurestoreJdbcConnectorController.createDefaultJdbcConnectorForFeaturestore(featurestore, project.getName(),
        "JDBC connection to Hopsworks Project Hive Warehouse");
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR + project.getName(), project,
        project.getOwner(), ActivityFlag.SERVICE);
    featurestoreHopsfsConnectorController.createHopsFsBackendForFeaturestoreConnector(featurestore,
        trainingDatasetsFolder);
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR + trainingDatasetsFolder.
        getName(), project, project.getOwner(), ActivityFlag.SERVICE);

    addUserOnlineFeatureStoreDB(project, user);

    return featurestore;
  }

  /**
   * Converts a featurestore entity to a Featurestore DTO, supplements the featurestore entity
   * with Hive metadata and remove foreign keys that are less interesting for users.
   *
   * @param featurestore
   * the featurestore entity
   * @return a DTO representation of the featurestore
   */
  public FeaturestoreDTO convertFeaturestoretoDTO(Featurestore featurestore) {
    String hiveDbDescription = featurestoreFacade.getHiveDatabaseDescription(featurestore.getHiveDbId());
    FeaturestoreDTO featurestoreDTO = new FeaturestoreDTO(featurestore);
    featurestoreDTO.setFeaturestoreDescription(hiveDbDescription);
    String hiveDbName = featurestoreFacade.getHiveDbName(featurestore.getHiveDbId());
    featurestoreDTO.setFeaturestoreName(hiveDbName);
    String hdfsPath = featurestoreFacade.getHiveDbHdfsPath(featurestore.getHiveDbId());
    featurestoreDTO.setHdfsStorePath(hdfsPath);
    Long inodeId = featurestoreFacade.getFeaturestoreInodeId(featurestore.getHiveDbId());
    featurestoreDTO.setInodeId(inodeId);
    return featurestoreDTO;
  }

  /**
   * Gets the featurestore Hive DB name of a project
   *
   * @param project
   * @return the hive database name of the featurestore in the project
   */
  public String getFeaturestoreDbName(Project project) {
    return project.getName() + "_featurestore";
  }

  /**
   * Writes JSON input for featurestore Util Job to HDFS as a JSON file
   *
   * @param user
   * user making the request
   * @param project
   * project of the user
   * @param featurestoreUtilJobDTO
   * the JSON DTO
   * @return HDFS path where the JSON file was written
   * @throws FeaturestoreException
   * @throws JAXBException
   */
  public String writeUtilArgsToHdfs(Users user, Project project, FeaturestoreUtilJobDTO featurestoreUtilJobDTO)
      throws FeaturestoreException, JAXBException {
    if (featurestoreUtilJobArgsMarshaller == null) {
      try {
        featurestoreUtilJobArgsJaxbContext = JAXBContextFactory.createContext(new Class[]{FeaturestoreUtilJobDTO.class},
            null);
        featurestoreUtilJobArgsMarshaller = featurestoreUtilJobArgsJaxbContext.createMarshaller();
        featurestoreUtilJobArgsMarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
        featurestoreUtilJobArgsMarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      } catch (JAXBException e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_INITIALIZATION_ERROR,
            Level.SEVERE, "Error initialization feature store controller");
      }
    }
    StringWriter sw = new StringWriter();
    featurestoreUtilJobArgsMarshaller.marshal(featurestoreUtilJobDTO, sw);
    Path hdfsPath = new Path(String.format(FEATURESTORE_UTIL_ARGS_PATH, project.getName(),
        featurestoreUtilJobDTO.getFileName()));
    writeToHDFS(project, user, hdfsPath, sw.toString());
    return String.format(HDFS_FILE_PATH, hdfsPath.toString());
  }

  /**
   * Writes a string to a new file in HDFS
   *
   * @param project
   * project of the user
   * @param user
   * user making the request
   * @param path2file
   * the hdfs path
   * @param content
   * the content to write
   * @throws FeaturestoreException
   */
  private void writeToHDFS(Project project, Users user, Path path2file, String content) throws FeaturestoreException {
    DistributedFileSystemOps udfso = null;
    try {
      String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
      udfso = distributedFsService.getDfsOps(hdfsUsername);
      try (FSDataOutputStream outStream = udfso.create(path2file)) {
        outStream.writeBytes(content);
        outStream.hflush();
      }
    } catch (IOException ex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_UTIL_ARGS_FAILURE,
          Level.WARNING, "Failed to write featurestore util args to HDFS", ex.getMessage(), ex);
    } finally {
      if (udfso != null) {
        distributedFsService.closeDfsClient(udfso);
      }
    }
  }
}
