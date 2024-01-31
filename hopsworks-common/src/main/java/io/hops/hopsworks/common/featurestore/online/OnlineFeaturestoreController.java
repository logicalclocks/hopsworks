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
package io.hops.hopsworks.common.featurestore.online;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.SecretId;
import io.hops.hopsworks.persistence.entity.user.security.secrets.VisibilityType;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang3.RandomStringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreFacade.MYSQL_DRIVER;

/**
 * Class controlling the interaction with the online featurestore databases in Hopsworks and the associated
 * business logic.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OnlineFeaturestoreController {

  private static final Logger LOGGER = Logger.getLogger(OnlineFeaturestoreController.class.getName());

  public static final String ONLINEFS_USERNAME = "onlinefs";
  
  @EJB
  private SecretsFacade secretsFacade;
  @EJB
  private Settings settings;
  @EJB
  private SecretsController secretsController;
  @EJB
  private OnlineFeaturestoreFacade onlineFeaturestoreFacade;
  @EJB
  private FeaturestoreConnectorFacade featurestoreConnectorFacade;
  @EJB
  private StorageConnectorUtil storageConnectorUtil;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private ProjectController projectController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

  /**
   * Sets up the online feature store database for a new project and creating a database-user for the project-owner
   *
   * @param project project
   * @param featurestore the featurestore metadata entity
   * @throws FeaturestoreException
   */
  public void setupOnlineFeaturestore(Project project, Featurestore featurestore, Users user)
    throws FeaturestoreException {

    try {
      if (projectController.findProjectById(project.getId()).getOnlineFeatureStoreAvailable()) {
        return;
      }
    } catch (ProjectException e) {
      // Should not happen but skip setup if project is not found.
      if (e.getErrorCode() == RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND) {
        return;
      }
    }

    if (!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
              Level.FINE, "Online feature store service is not enabled for this Hopsworks instance");
    }

    try (Connection connection = onlineFeaturestoreFacade.establishAdminConnection()) {
      String db = getOnlineFeaturestoreDbName(featurestore.getProject());
      // Create kafka offset table
      onlineFeaturestoreFacade.createOnlineFeaturestoreDatabaseIfNotExist(db, connection);
      // Create kafka offset table
      onlineFeaturestoreFacade.createOnlineFeaturestoreKafkaOffsetTable(db, connection);

      // Create project owner database user
      createDatabaseUser(user, featurestore, ProjectRoleTypes.DATA_OWNER.getRole(), connection);
      // Create online feature store users for existing team members
      for (ProjectTeam projectTeam : projectTeamFacade.findMembersByProject(project)) {
        if (!projectTeam.getUser().equals(user)) {
          createDatabaseUser(projectTeam.getUser(), featurestore, projectTeam.getTeamRole(), connection);
        }
      }
    } catch(SQLException e) {
      throw new FeaturestoreException(
              RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE,
              Level.SEVERE, e.getMessage(), e.getMessage(), e);
    }

    projectController.setOnlineFeatureStoreAvailable(project);
  }

  private void shareOnlineFeatureStore(Project project, Users user, String role, Connection connection)
          throws FeaturestoreException {
    List<DatasetSharedWith> sharedFeatureStores = project.getDatasetSharedWithCollection().stream()
            .filter(ds -> ds.getAccepted() && ds.getDataset().getDsType() == DatasetType.FEATURESTORE)
            .collect(Collectors.toList());
    for (DatasetSharedWith shared : sharedFeatureStores) {
      String featureStoreDb = getOnlineFeaturestoreDbName(shared.getDataset().getProject());
      shareOnlineFeatureStoreUser(project, user, role, featureStoreDb, shared.getPermission(), connection);
    }
  }
  
  /**
   * Creates a new database user
   *
   * @param user the Hopsworks user
   * @param featurestore the feature store
   * @throws FeaturestoreException
   */
  public void createDatabaseUser(Users user, Featurestore featurestore, String projectRole, Connection connection)
      throws FeaturestoreException {
    String db = getOnlineFeaturestoreDbName(featurestore.getProject());
    if (!checkIfDatabaseExists(db)) {
      // There is no online feature store for this feature store
      return;
    }

    String dbUser = onlineDbUsername(featurestore.getProject(), user);
    //Generate random pw
    String onlineFsPw = getOrCreateUserSecret(dbUser, user, featurestore.getProject());
    //database is the same as the project name
    onlineFeaturestoreFacade.createOnlineFeaturestoreUser(dbUser, onlineFsPw, connection);

    updateUserOnlineFeatureStoreDB(user, featurestore, projectRole, connection);

    shareOnlineFeatureStore(featurestore.getProject(), user, projectRole, connection);
  }
  
  /**
   * Stores the online-featurestore password as a Hopsworks secret for the user
   *
   * @param dbuser the database-user
   * @param user the user to store the secret for
   * @param project the project of the online feature store
   * @return the password
   * @throws FeaturestoreException
   */
  private String getOrCreateUserSecret(String dbuser, Users user, Project project) throws FeaturestoreException {
    String password = "";
    try {
      password = secretsController.get(user, dbuser).getPlaintext();
    } catch (UserException e) {
      if (e.getErrorCode() != RESTCodes.UserErrorCode.SECRET_EMPTY) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
                Level.SEVERE, "Error retrieving existing online feature store password from secret manager");
      }
    }

    if (!Strings.isNullOrEmpty(password)) {
      // Password was found, return it
      return password;
    }

    try {
      password = RandomStringUtils.randomAlphabetic(FeaturestoreConstants.ONLINE_FEATURESTORE_PW_LENGTH);
      secretsController.add(user, dbuser, password, VisibilityType.PRIVATE, project.getId());
    } catch (UserException e) {
      // Secret may be created concurrently when multiple online feature groups are created concurrently.
      // If secret already exists due to race condition, get it again.
      if (e.getErrorCode() == RESTCodes.UserErrorCode.SECRET_EXISTS) {
        return getOrCreateUserSecret(dbuser, user, project);
      } else {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
                Level.SEVERE, "Error adding online feature store password to secret manager");
      }
    }

    return password;
  }
  
  /**
   * Gets the name of the online database given a project and a user
   *
   * @param project the project
   * @param user the user
   * @return a string representing the online database name
   */
  public String onlineDbUsername(Project project, Users user) {
    return onlineDbUsername(project.getName(), user.getUsername());
  }
  
  /**
   * Gets the featurestore MySQL DB name of a project
   *
   * @param project the project to get the MySQL-db name of the feature store for
   * @return the MySQL database name of the featurestore in the project
   */
  public String getOnlineFeaturestoreDbName(Project project) {
    return project.getName().toLowerCase();
  }

  /**
   * The mysql username can be at most 32 characters in length. 
   * Clip the username to 32 chars if it is longer than 32.
   * 
   * @param project projectname
   * @param user username
   * @return the mysql username for the online featuer store
   */
  private String onlineDbUsername(String project, String user) {
    String username = project + "_" + user;
    if (username.length() > FeaturestoreConstants.ONLINE_FEATURESTORE_USERNAME_MAX_LENGTH) {
      username = username.substring(0, FeaturestoreConstants.ONLINE_FEATURESTORE_USERNAME_MAX_LENGTH-1);
    }
    return username;
  }
  
  /**
   * Updates the user-privileges on the online feature store database
   *
   * @param user the user to be added
   * @param featurestore the feature store
   * @throws FeaturestoreException
   */
  public void updateUserOnlineFeatureStoreDB(Users user, Featurestore featurestore, String projectRole,
                                             Connection connection)
    throws FeaturestoreException {
    String db = getOnlineFeaturestoreDbName(featurestore.getProject());
    if (!settings.isOnlineFeaturestore() || !checkIfDatabaseExists(db)) {
      //Nothing to update
      return;
    }

    String dbuser = onlineDbUsername(featurestore.getProject(), user);

    if (projectRole.equals(ProjectRoleTypes.DATA_OWNER.getRole())) {
      onlineFeaturestoreFacade.grantDataOwnerPrivileges(db, dbuser, connection);
    } else {
      onlineFeaturestoreFacade.grantDataScientistPrivileges(db, dbuser, connection);
    }

    try {
      createJdbcConnectorForOnlineFeaturestore(dbuser, featurestore, db);
    } catch(Exception e) {
      //If the connector have already been created, skip this step
    }
  }

  /**
   * Utility function for create a JDBC connection to the online featurestore for a particular user.
   *
   * @param onlineDbUsername the db-username of the connection
   * @param featurestore the featurestore metadata
   * @param dbName name of the MySQL database
   * @return DTO of the newly created connector
   * @throws FeaturestoreException
   */
  public void createJdbcConnectorForOnlineFeaturestore(String onlineDbUsername,
             Featurestore featurestore, String dbName) throws FeaturestoreException {
    String connectorName = onlineDbUsername + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX;
    if (featurestoreConnectorFacade.findByFeaturestoreName(featurestore, connectorName).isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
          "a storage connector with that name already exists");
    }

    FeaturestoreConnector featurestoreConnector = new FeaturestoreConnector();
    featurestoreConnector.setName(connectorName);
    featurestoreConnector.setDescription("JDBC connection to Hopsworks Project Online " +
        "Feature Store NDB Database for user: " + onlineDbUsername);
    featurestoreConnector.setFeaturestore(featurestore);
    featurestoreConnector.setConnectorType(FeaturestoreConnectorType.JDBC);

    FeaturestoreJdbcConnector featurestoreJdbcConnector = new FeaturestoreJdbcConnector();
    featurestoreJdbcConnector.setConnectionString(settings.getFeaturestoreJdbcUrl() + dbName +
        "?useSSL=false&allowPublicKeyRetrieval=true");
    List<OptionDTO> arguments = new ArrayList<>();
    arguments.add(new OptionDTO(FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_PASSWORD_ARG,
        FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE));
    arguments.add(new OptionDTO(FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_USER_ARG, onlineDbUsername));
    arguments.add(new OptionDTO(FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_DRIVER_ARG, MYSQL_DRIVER));
    arguments.add(new OptionDTO("isolationLevel", "NONE"));
    arguments.add(new OptionDTO("batchsize", "500"));

    featurestoreJdbcConnector.setArguments(storageConnectorUtil.fromOptions(arguments));
    featurestoreConnector.setJdbcConnector(featurestoreJdbcConnector);

    featurestoreConnectorFacade.update(featurestoreConnector);
  }
  
  /**
   * Drops an online feature store database and removes all associated users
   *
   * @param project the project of the user making the request
   * @throws FeaturestoreException
   */
  public void removeOnlineFeatureStore(Project project) throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore() || !checkIfDatabaseExists(getOnlineFeaturestoreDbName(project))) {
      //Nothing to remove
      return;
    }
    try (Connection connection = onlineFeaturestoreFacade.establishAdminConnection()) {
      for (ProjectTeam member : projectUtils.getProjectTeamCollection(project)) {
        String dbUser = onlineDbUsername(project, member.getUser());
        try {
          secretsController.delete(member.getUser(), dbUser);
        } catch (UserException e) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
            Level.SEVERE, "Problem removing user-secret to online featurestore");
        }
        onlineFeaturestoreFacade.removeOnlineFeaturestoreUser(dbUser, connection);
      }
      
      String db = getOnlineFeaturestoreDbName(project);
      onlineFeaturestoreFacade.removeOnlineFeaturestoreDatabase(db, connection);
    } catch (SQLException se) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE,
        Level.SEVERE, "Error closing connection", se.getMessage(), se);
    }
  }
  
  public void removeOnlineFeaturestoreUser(Featurestore featurestore, Users user) throws FeaturestoreException {
    String db = getOnlineFeaturestoreDbName(featurestore.getProject());
    if (!checkIfDatabaseExists(db)) {
      //Nothing to remove
      return;
    }

    String dbUser = onlineDbUsername(featurestore.getProject().getName(), user.getUsername());

    SecretId id = new SecretId(user.getUid(), dbUser);
    secretsFacade.deleteSecret(id);
    try (Connection connection = onlineFeaturestoreFacade.establishAdminConnection()) {
      onlineFeaturestoreFacade.removeOnlineFeaturestoreUser(dbUser, connection);
    } catch (SQLException se) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE,
        Level.SEVERE, "Error closing connection", se.getMessage(), se);
    }

    featurestoreConnectorFacade.deleteByFeaturestoreName(featurestore,
        dbUser + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX);
  }

  /**
   * Share feature store with project.
   * @param project: project to share the feature store with
   * @param featurestore: the feature store to share
   * @param permission: the permissions the project members will have on the feature store
   * @throws FeaturestoreException
   */
  public void shareOnlineFeatureStore(Project project, Featurestore featurestore,
                                      DatasetAccessPermission permission) throws FeaturestoreException {
    String featureStoreDb = getOnlineFeaturestoreDbName(featurestore.getProject());
    if (!checkIfDatabaseExists(featureStoreDb)) {
      // Nothing to share
      return;
    }

    try (Connection connection = onlineFeaturestoreFacade.establishAdminConnection()) {
      for (ProjectTeam member : projectUtils.getProjectTeamCollection(project)) {
        shareOnlineFeatureStoreUser(project, member.getUser(), member.getTeamRole(), featureStoreDb, permission,
          connection);
      }
    } catch (SQLException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE,
        Level.SEVERE, e.getMessage(), e.getMessage(), e);
    }
  }

  /**
   * Share feature store with project user
   * @param project: project to share the feature store with
   * @param user: the user to share with
   * @param role: the role of the user in the target project
   * @param featurestore: the feature store to share
   * @param permission: the permissions the project members will have on the feature store
   * @throws FeaturestoreException
   */
  public void shareOnlineFeatureStore(Project project, Users user, String role, Featurestore featurestore,
                                      DatasetAccessPermission permission, Connection conn)
    throws FeaturestoreException {
    String featureStoreDb = getOnlineFeaturestoreDbName(featurestore.getProject());
    if (!checkIfDatabaseExists(featureStoreDb)) {
      // Nothing to share
      return;
    }

    shareOnlineFeatureStoreUser(project, user, role, featureStoreDb, permission, conn);
  }

  /**
   * Set share online feature store for a single user
   * @param project
   * @param user
   * @param role
   * @param featureStoreDb
   * @param permission
   * @throws FeaturestoreException
   */
  private void shareOnlineFeatureStoreUser(Project project, Users user, String role,
                                          String featureStoreDb, DatasetAccessPermission permission, Connection conn)
      throws FeaturestoreException {
    String dbUser = onlineDbUsername(project, user);

    if (permission == DatasetAccessPermission.READ_ONLY ||
        (permission == DatasetAccessPermission.EDITABLE_BY_OWNERS &&
            role.equals(ProjectRoleTypes.DATA_SCIENTIST.getRole()))) {
      // Read Only
      onlineFeaturestoreFacade.grantDataScientistPrivileges(featureStoreDb, dbUser, conn);
    } else {
      // Write permissions
      onlineFeaturestoreFacade.grantDataOwnerPrivileges(featureStoreDb, dbUser, conn);
    }
  }

  /**
   * remove project access to a feature store
   * this method is resilient to the fact that existing installations don't have
   * the share online feature store setup correctly. The revoke privileges first checks that
   * there are privileges, if there are, then revokes them.
   * @param project: project to remove access
   * @param featurestore: feature store to remove access
   * @throws FeaturestoreException
   */
  public void unshareOnlineFeatureStore(Project project, Featurestore featurestore) throws FeaturestoreException {
    String featureStoreDb = getOnlineFeaturestoreDbName(featurestore.getProject());
    if (!checkIfDatabaseExists(featureStoreDb)) {
      // Nothing to share
      return;
    }


    try (Connection connection = onlineFeaturestoreFacade.establishAdminConnection()) {
      for (ProjectTeam member : projectUtils.getProjectTeamCollection(project)) {
        String dbUser = onlineDbUsername(project, member.getUser());
        onlineFeaturestoreFacade.revokeUserPrivileges(featureStoreDb, dbUser, connection);
      }
    } catch (SQLException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE,
        Level.SEVERE, e.getMessage(), e.getMessage(), e);
    }
  }

  /**
   * Checks if a mysql database exists
   *
   * @param dbName the name of the database
   * @return true or false depending on if the database exists or not
   */
  public Boolean checkIfDatabaseExists(String dbName) {
    return onlineFeaturestoreFacade.checkIfDatabaseExists(dbName);
  }
}
