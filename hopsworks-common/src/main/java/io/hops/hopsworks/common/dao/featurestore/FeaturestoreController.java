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

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetType;
import io.hops.hopsworks.common.dao.featurestore.app.FeaturestoreUtilJobDTO;
import io.hops.hopsworks.common.dao.featurestore.online_featurestore.OnlineFeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnectorController;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.dao.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.featorestore.FeaturestoreConstants;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
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
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_store table and required business logic
 */
@Stateless
public class FeaturestoreController {
  
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private FeaturestoreJdbcConnectorController featurestoreJdbcConnectorController;
  @EJB
  private FeaturestoreHopsfsConnectorController featurestoreHopsfsConnectorController;
  @EJB
  private Settings settings;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  
  private static JAXBContext featurestoreUtilJobArgsJaxbContext = null;
  private static Marshaller featurestoreUtilJobArgsMarshaller = null;
  private static final String FEATURESTORE_UTIL_ARGS_PATH = Path.SEPARATOR + Settings.DIR_ROOT + Path.SEPARATOR
      + "%s" + Path.SEPARATOR + FeaturestoreConstants.FEATURESTORE_UTIL_4J_ARGS_DATASET + Path.SEPARATOR + "%s";
  private static final String HDFS_FILE_PATH = "hdfs://%s";

  /**
   * Retrieves a list of all featurestores for a particular project
   *
   * @param project the project to retrieve featurestores for
   * @return a list of DTOs for the featurestores
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<FeaturestoreDTO> getFeaturestoresForProject(Project project) {
    List<Featurestore> featurestores = getProjectFeaturestores(project);
    return featurestores.stream().map(fs -> convertFeaturestoreToDTO(fs)).collect(Collectors.toList());
  }

  /**
   * Helper function that lists all featurestores in the project (including featurestores shared with the project)
   *
   * @param project the project to list featurestores for
   * @return a list of featurestore entities
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private List<Featurestore> getProjectFeaturestores(Project project) {
    Collection<Dataset> dsInProject = project.getDatasetCollection();
    Collection<Dataset> featurestoresDsInproject = dsInProject.stream().filter(ds -> ds.getDsType()
      == DatasetType.FEATURESTORE).collect(Collectors.toList());
    return featurestoresDsInproject.stream().map(ds -> ds.getFeatureStore()).collect(Collectors.toList());
  }

  /**
   * Retrieves a featurestore for a project with a specific name
   *
   * @param project the project to retrieve featurestores for
   * @param featurestoreName the name of the featurestore
   * @return a list of DTOs for the featurestores
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturestoreDTO getFeaturestoreForProjectWithName(Project project, String featurestoreName)
      throws FeaturestoreException {
    List<Featurestore> featurestores = getProjectFeaturestores(project);
    List<FeaturestoreDTO> featurestoreDTOs = featurestores.stream().map(fs -> convertFeaturestoreToDTO(fs)).collect(
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
   * @param project the project to look for the featurestore in
   * @param featurestoreId the featurestoreId of the featurestore
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
    return convertFeaturestoreToDTO(featurestoresWithId.get(0));
  }

  /**
   * Retrieves a featurestore with a particular Id from the database
   *
   * @param id the id of the featurestore
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
   * @param project                 project of the new featurestore
   * @param featurestoreName        the name of the new featurestore
   * @param trainingDatasetsFolder  the Hopsworks dataset where training datasets are stored by default
   * @return the created featurestore
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Featurestore createProjectFeatureStore(Project project, Users user, String featurestoreName,
      Dataset trainingDatasetsFolder) throws FeaturestoreException, IOException {

    //Get HiveDbId for the newly created Hive featurestore DB
    Long hiveDbId = featurestoreFacade.getHiveDatabaseId(featurestoreName);
    //Store featurestore metadata in Hopsworks
    Featurestore featurestore = new Featurestore();
    featurestore.setProject(project);
    featurestore.setHiveDbId(hiveDbId);
    featurestore.setCreated(new Date());
    featurestoreFacade.persist(featurestore);
    activityFacade.persistActivity(ActivityFacade.CREATED_FEATURESTORE + featurestoreName, project,
      project.getOwner(), ActivityFlag.SERVICE);
    featurestoreJdbcConnectorController.createDefaultJdbcConnectorForOfflineFeaturestore(featurestore,
        getOfflineFeaturestoreDbName(project), "JDBC connection to Hopsworks Project Feature Store Hive Database");
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR +
        getOfflineFeaturestoreDbName(project), project, project.getOwner(), ActivityFlag.SERVICE);
    featurestoreJdbcConnectorController.createDefaultJdbcConnectorForOfflineFeaturestore(featurestore,
      project.getName(), "JDBC connection to Hopsworks Project Hive Warehouse");
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR + project.getName(),
      project, project.getOwner(), ActivityFlag.SERVICE);
    featurestoreHopsfsConnectorController.createHopsFsBackendForFeaturestoreConnector(featurestore,
        trainingDatasetsFolder);
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR + trainingDatasetsFolder.
        getName(), project, project.getOwner(), ActivityFlag.SERVICE);
    if (settings.isOnlineFeaturestore()) {
      onlineFeaturestoreController.setupOnlineFeaturestore(project, user, featurestore);
    }
    return featurestore;
  }

  /**
   * Converts a featurestore entity to a Featurestore DTO, supplements the featurestore entity
   * with Hive metadata and remove foreign keys that are less interesting for users.
   *
   * @param featurestore the featurestore entity
   * @return a DTO representation of the featurestore
   */
  public FeaturestoreDTO convertFeaturestoreToDTO(Featurestore featurestore) {
    String hiveDbDescription = featurestoreFacade.getHiveDatabaseDescription(featurestore.getHiveDbId());
    FeaturestoreDTO featurestoreDTO = new FeaturestoreDTO(featurestore);
    featurestoreDTO.setFeaturestoreDescription(hiveDbDescription);
    String hiveDbName = featurestoreFacade.getHiveDbName(featurestore.getHiveDbId());
    featurestoreDTO.setFeaturestoreName(hiveDbName);
    String hdfsPath = featurestoreFacade.getHiveDbHdfsPath(featurestore.getHiveDbId());
    featurestoreDTO.setHdfsStorePath(hdfsPath);
    Long inodeId = featurestoreFacade.getFeaturestoreInodeId(featurestore.getHiveDbId());
    featurestoreDTO.setInodeId(inodeId);
    String hiveEndpoint = settings.getHiveServerHostName(false);
    featurestoreDTO.setHiveEndpoint(hiveEndpoint);
    featurestoreDTO.setOfflineFeaturestoreName(hiveDbName);
    if(settings.isOnlineFeaturestore() &&
      onlineFeaturestoreController.checkIfDatabaseExists(
          onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject()))) {
      featurestoreDTO.setMysqlServerEndpoint(settings.getFeaturestoreJdbcUrl());
      featurestoreDTO.setOnlineFeaturestoreSize(onlineFeaturestoreController.getDbSize(
          onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject())));
      featurestoreDTO.setOnlineFeaturestoreType(FeaturestoreConstants.ONLINE_FEATURE_STORE_TYPE);
      featurestoreDTO.setOnlineFeaturestoreName(featurestore.getProject().getName());
      featurestoreDTO.setOnlineEnabled(true);
    }
    return featurestoreDTO;
  }

  /**
   * Gets the featurestore Hive DB name of a project
   *
   * @param project the project to get the hive-db name of the feature store for
   * @return the hive database name of the featurestore in the project
   */
  public String getOfflineFeaturestoreDbName(Project project) {
    return project.getName().toLowerCase() + FeaturestoreConstants.FEATURESTORE_HIVE_DB_SUFFIX;
  }

  /**
   * Writes JSON input for featurestore Util Job to HDFS as a JSON file
   *
   * @param user user making the request
   * @param project project of the user
   * @param featurestoreUtilJobDTO the JSON DTO
   * @return HDFS path where the JSON file was written
   * @throws FeaturestoreException
   * @throws JAXBException
   */
  public String writeUtilArgsToHdfs(Users user, Project project, FeaturestoreUtilJobDTO featurestoreUtilJobDTO)
      throws FeaturestoreException, JAXBException {
    if (featurestoreUtilJobArgsMarshaller == null) {
      try {
        featurestoreUtilJobArgsJaxbContext =
            JAXBContextFactory.createContext(new Class[]{FeaturestoreUtilJobDTO.class}, null);
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

    try {
      featurestoreUtils.writeToHDFS(project, user, hdfsPath, sw.toString());
    } catch (IOException ex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_UTIL_ARGS_FAILURE,
          Level.WARNING, "Failed to write featurestore util args to HDFS", ex.getMessage(), ex);
    }

    return String.format(HDFS_FILE_PATH, hdfsPath.toString());
  }
}
