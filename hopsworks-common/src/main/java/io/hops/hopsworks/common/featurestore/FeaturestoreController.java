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
 */
package io.hops.hopsworks.common.featurestore;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_store table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FeaturestoreController {
  
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private Settings settings;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private HiveController hiveController;
  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private TrainingDatasetFacade trainingDatasetFacade;
  @EJB
  private FeaturestoreConnectorFacade connectorFacade;
  @EJB
  private FeaturestoreStorageConnectorController featurestoreStorageConnectorController;
  

  /*
   * Retrieves a list of all featurestores for a particular project
   *
   * @param project the project to retrieve featurestores for
   * @return a list of DTOs for the featurestores
   */
  public List<FeaturestoreDTO> getFeaturestoresForProject(Project project) throws FeaturestoreException {
    List<Featurestore> featurestores = getProjectFeaturestores(project);
    try {
      return featurestores.stream().map(this::convertFeaturestoreToDTO).collect(Collectors.toList());
    } catch (RuntimeException ex) {
      if (ex.getCause() instanceof ServiceDiscoveryException) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_INITIALIZATION_ERROR,
            Level.SEVERE, "Could not create Hive connection string",
            ex.getMessage(), ex);
      }
      throw ex;
    }
  }
  
  /**
   * Return the feature store dataset for the specific project. not the shared ones.
   * @param project
   * @return
   */
  public Dataset getProjectFeaturestoreDataset(Project project) throws FeaturestoreException {
    return  project.getDatasetCollection().stream()
      .filter(ds -> ds.getDsType() == DatasetType.FEATURESTORE)
      .findFirst()
      .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND,
        Level.INFO, "Could not find feature store for project: " + project.getName()));
  }
  
  /**
   * Return the feature store for the specific project. not the shared ones.
   * @param project
   * @return
   */
  public Featurestore getProjectFeaturestore(Project project) throws FeaturestoreException {
    Collection<Dataset> dsInProject = project.getDatasetCollection();
    return  dsInProject.stream()
        .filter(ds -> ds.getDsType() == DatasetType.FEATURESTORE)
        .map(Dataset::getFeatureStore)
        .findFirst()
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND,
            Level.INFO, "Could not find feature store for project: " + project.getName()));
  }

  /**
   * Helper function that lists all featurestores in the project (including featurestores shared with the project)
   *
   * @param project the project to list featurestores for
   * @return a list of featurestore entities
   */
  private List<Featurestore> getProjectFeaturestores(Project project) {
    Collection<Dataset> dsInProject = project.getDatasetCollection();
    // Add all datasets shared with the project
    dsInProject.addAll(project.getDatasetSharedWithCollection().stream()
        // Filter out datasets which have not been accepted
        .filter(DatasetSharedWith::getAccepted)
        .map(DatasetSharedWith::getDataset).collect(Collectors.toList()));
    return  dsInProject.stream()
        .filter(ds -> ds.getDsType() == DatasetType.FEATURESTORE)
        .map(Dataset::getFeatureStore).collect(Collectors.toList());
  }

  /**
   * Retrieves a featurestore for a project with a specific name
   *
   * @param project the project to retrieve featurestores for
   * @param featurestoreName the name of the featurestore
   * @return a list of DTOs for the featurestores
   */
  public FeaturestoreDTO getFeaturestoreForProjectWithName(Project project, String featurestoreName)
      throws FeaturestoreException {
    try {
      return getProjectFeaturestores(project).stream()
          .map(this::convertFeaturestoreToDTO)
          .filter(fs -> fs.getFeaturestoreName().equals(featurestoreName))
          .findFirst()
          //Featurestore name corresponds to Hive databases so uniqueness is enforced by Hive
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND,
              Level.FINE, "featurestoreName: " + featurestoreName + " , project: " + project.getName()));
    } catch (RuntimeException ex) {
      if (ex.getCause() instanceof ServiceDiscoveryException) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_INITIALIZATION_ERROR,
            Level.SEVERE, "Could not create Hive connection string",
            ex.getMessage(), ex);
      }
      throw ex;
    }
  }

  /**
   * Gets a featurestore with a particular featurestoreId from the list of featurestores for this project
   *
   * @param project the project to look for the featurestore in
   * @param featurestoreId the featurestoreId of the featurestore
   * @return a DTO representation of the featurestore
   * @throws FeaturestoreException
   */
  public FeaturestoreDTO getFeaturestoreForProjectWithId(Project project, Integer featurestoreId)
      throws FeaturestoreException {
    try {
      return getProjectFeaturestores(project).stream()
          .filter(fs -> fs.getId().equals(featurestoreId))
          .map(this::convertFeaturestoreToDTO)
          .findAny()
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND,
              Level.FINE, "featurestoreId: " + featurestoreId + " , project: " + project.getName()));
    } catch (RuntimeException ex) {
      if (ex.getCause() instanceof ServiceDiscoveryException) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_INITIALIZATION_ERROR,
            Level.SEVERE, "Could not create Hive connection string",
            ex.getMessage(), ex);
      }
      throw ex;
    }
  }

  /**
   * Retrieves a featurestore with a particular Id from the database
   *
   * @param id the id of the featurestore
   * @return featurestore entity with the given id
   */
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
  public Featurestore createProjectFeatureStore(Project project, Users user, String featurestoreName,
      Dataset trainingDatasetsFolder) throws FeaturestoreException, ProjectException, UserException {

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
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR +
        getOfflineFeaturestoreDbName(project), project, project.getOwner(), ActivityFlag.SERVICE);
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR + project.getName(),
      project, project.getOwner(), ActivityFlag.SERVICE);
    featurestoreStorageConnectorController
        .createStorageConnector(user, project, featurestore, hopsfsTrainingDatasetConnector(trainingDatasetsFolder));
    featurestoreStorageConnectorController
        .createStorageConnector(user, project, featurestore, createOfflineJdbcConnector(featurestoreName));
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR + trainingDatasetsFolder.
        getName(), project, project.getOwner(), ActivityFlag.SERVICE);
    if (settings.isOnlineFeaturestore()) {
      onlineFeaturestoreController.setupOnlineFeaturestore(user, featurestore);
    }
    return featurestore;
  }

  public FeaturestoreStorageConnectorDTO hopsfsTrainingDatasetConnector(Dataset hopsfsDataset) {
    String name = hopsfsDataset.getName();
    String description = "HOPSFS backend for storing Training Datasets of the Hopsworks Feature Store";
    FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO = new FeaturestoreHopsfsConnectorDTO();
    featurestoreHopsfsConnectorDTO.setStorageConnectorType(FeaturestoreConnectorType.HOPSFS);
    featurestoreHopsfsConnectorDTO.setName(name);
    featurestoreHopsfsConnectorDTO.setDescription(description);
    featurestoreHopsfsConnectorDTO.setDatasetName(hopsfsDataset.getName());

    return featurestoreHopsfsConnectorDTO;
  }

  public FeaturestoreStorageConnectorDTO createOfflineJdbcConnector(String databaseName) throws FeaturestoreException {
    String hiveEndpoint;
    try {
      hiveEndpoint = hiveController.getHiveServerInternalEndpoint();
    } catch (ServiceDiscoveryException ex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND,
          Level.SEVERE, "Could not create Hive connection string", ex.getMessage(), ex);
    }

    String connectionString = "jdbc:hive2://" + hiveEndpoint + "/" + databaseName + ";" +
        "auth=noSasl;ssl=true;twoWay=true;";
    String arguments = "sslTrustStore,trustStorePassword,sslKeyStore,keyStorePassword";
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO = new FeaturestoreJdbcConnectorDTO();
    featurestoreJdbcConnectorDTO.setStorageConnectorType(FeaturestoreConnectorType.JDBC);
    featurestoreJdbcConnectorDTO.setName(databaseName);
    featurestoreJdbcConnectorDTO.setDescription("JDBC connector for the Offline Feature Store");
    featurestoreJdbcConnectorDTO.setConnectionString(connectionString);
    featurestoreJdbcConnectorDTO.setArguments(arguments);
    return featurestoreJdbcConnectorDTO;
  }

  /**
   * Converts a featurestore entity to a Featurestore DTO, supplements the featurestore entity
   * with Hive metadata and remove foreign keys that are less interesting for users.
   *
   * @param featurestore the featurestore entity
   * @return a DTO representation of the featurestore
   */
  private FeaturestoreDTO convertFeaturestoreToDTO(Featurestore featurestore) {
    String hiveDbDescription = featurestoreFacade.getHiveDatabaseDescription(featurestore.getHiveDbId());
    FeaturestoreDTO featurestoreDTO = new FeaturestoreDTO(featurestore);

    featurestoreDTO.setFeaturestoreDescription(hiveDbDescription);
    String name = featurestoreFacade.getHiveDbName(featurestore.getHiveDbId());
    // TODO(Fabio): remove this when we switch to the new UI.
    featurestoreDTO.setFeaturestoreName(name);
    featurestoreDTO.setOfflineFeaturestoreName(name);
    featurestoreDTO.setHdfsStorePath(featurestoreFacade.getHiveDbHdfsPath(featurestore.getHiveDbId()));
    featurestoreDTO.setInodeId(featurestoreFacade.getFeaturestoreInodeId(featurestore.getHiveDbId()));

    try {
      featurestoreDTO.setHiveEndpoint(hiveController.getHiveServerInternalEndpoint());
      if (settings.isOnlineFeaturestore() &&
          onlineFeaturestoreController.checkIfDatabaseExists(
              onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject()))) {
        featurestoreDTO.setMysqlServerEndpoint(onlineFeaturestoreController.getJdbcURL());
        featurestoreDTO.setOnlineFeaturestoreSize(onlineFeaturestoreController.getDbSize(featurestore));
        featurestoreDTO.setOnlineFeaturestoreName(featurestore.getProject().getName());
        featurestoreDTO.setOnlineEnabled(true);
      }
    } catch (ServiceDiscoveryException ex) {
      throw new RuntimeException(ex);
    }

    // add counters
    featurestoreDTO.setNumFeatureGroups(featuregroupFacade.countByFeaturestore(featurestore));
    featurestoreDTO.setNumTrainingDatasets(trainingDatasetFacade.countByFeaturestore(featurestore));
    featurestoreDTO.setNumStorageConnectors(connectorFacade.countByFeaturestore(featurestore));

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
}
