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
package io.hops.hopsworks.common.dao.featurestore.online_featurestore;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.secrets.Secret;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretId;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.VisibilityType;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the online featurestore databases in Hopsworks and the associated
 * business logic.
 */
@Stateless
public class OnlineFeaturestoreController {

  private static final Logger LOGGER = Logger.getLogger(OnlineFeaturestoreController.class.getName());
  
  @EJB
  private SecretsFacade secretsFacade;
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
  @EJB
  private FeaturestoreJdbcConnectorController featurestoreJdbcConnectorController;
  @EJB
  private OnlineFeaturestoreFacade onlineFeaturestoreFacade;
  
  /**
   * Runs the online featurestore bash script with given arguments
   * @param args add|rm|update, databasename, db_user, [db_passwords_file|
   * @return exist status of the local process that calls the DB
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private int onlineFeaturestoreBashScript(String... args) {
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
   * Sets up the online feature store database for a project and the project-owner
   *
   * @param project the project that should own the online featurestore database
   * @param user the project owner
   * @param featurestore the featurestore metadata entity
   * @throws FeaturestoreException
   * @throws IOException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void setupOnlineFeaturestoreUser(Project project, Users user, Featurestore featurestore)
    throws FeaturestoreException, IOException {
    if (!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online feature store service is not enabled for this Hopsworks instance");
    }
    
    // Step 1: Create Database User
    // Get dbusername (combination of project and user prefixed to 32 characters
    String dbuser = onlineDbUsername(project, user);
    //Generate random pw
    String onlineFsPw = createOnlineFeaturestoreUserSecret(dbuser, user, project);
    //Write the pw to a temp file for the bash script
    String pwPath = writePwTotempFile(dbuser, onlineFsPw);
    
    //Step 2: Create Database
    addOnlineFeatureStoreDB(project, user, pwPath);
    
    //Step 3: Store jdbc connector for the online featurestore
    featurestoreJdbcConnectorController.createJdbcConnectorForOnlineFeaturestore(project, dbuser, featurestore);
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
  private String createOnlineFeaturestoreUserSecret(String dbuser, Users user, Project project)
    throws FeaturestoreException {
    String onlineFsPw = securityUtils.generateSecureRandomString();
    SecretId id = new SecretId(user.getUid(), dbuser);
    Secret storedSecret = secretsFacade.findById(id);
    if (storedSecret == null) {
      try {
        secretsController.add(user, dbuser, onlineFsPw, VisibilityType.PRIVATE, project.getId());
      } catch (UserException e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
          Level.SEVERE, "Problem adding online featurestore password to hopsworks secretsmgr");
      }
    }
    return onlineFsPw;
  }
  
  /**
   * Writes a online-feature-store password to a temporary file to be used by bash script to create the online
   * featurestore
   *
   * @param dbuser the db-user that owns the password
   * @param onlineFsPassword  the password to the database for the db-user
   * @return the path to the temp file
   * @throws IOException
   * @throws FeaturestoreException
   */
  private String writePwTotempFile(String dbuser, String onlineFsPassword) throws IOException, FeaturestoreException {
    Set<PosixFilePermission> perms = new HashSet<>();
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    java.nio.file.Path path = null;
    path = java.nio.file.Paths.get("/tmp/" + dbuser);
    if (!path.toFile().exists()) {
      if (!path.toFile().createNewFile()) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
          Level.SEVERE, "Problem creating pw-file for online feature store");
      }
    }
    PrintWriter out = new PrintWriter(path.toFile());
    out.println(onlineFsPassword);
    out.flush();
    out.close();
    Files.setPosixFilePermissions(path, perms);
    return path.toString();
  }
  
  /**
   * Gets the role of a user in a string-format that can be passed to the bash script for creating the online
   * feature store.
   *
   * @param project the project of the user
   * @param user the user to get the role for
   * @return the role as a string
   */
  private String getRoleAsString(Project project, Users user) {
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
   * The mysql username can be at most 32 characters in length. 
   * Clip the username to 32 chars if it is longer than 32.
   * 
   * @param project projectname
   * @param user username
   * @return the mysql username for the online featuer store
   */
  private String onlineDbUsername(String project, String user) {
    String username = project + "_" + user;
    if (username.length() > 32) {
      username = username.substring(0, 31);
    }
    return username;
  }
  
  /**
   * Runs the online-featurestore bash-script for an update operation
   *
   * @param project the project of the user making the request
   * @param user the user making the request
   * @return the return-code of the bash-script
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public int updateUserOnlineFeatureStoreDB(Project project, Users user) {
    if (!settings.isOnlineFeaturestore()) {
      return 1;
    }    
    String role = getRoleAsString(project, user);
    String[] args = new String[5];
    args[0] = "update";
    args[1] = project.getName();
    args[2] = onlineDbUsername(project, user);
    args[3] = role;
    return onlineFeaturestoreBashScript(args);
  }
  
  /**
   * Runs the online-featurestore bash-script for an add operation (to add a user to a database)
   *
   * @param project the project of the user making the request
   * @param user the user making the request
   * @param pwPath path to the password
   * @return the return-code of the bash-script
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public int addOnlineFeatureStoreDB(Project project, Users user, String pwPath) {
    if (!settings.isOnlineFeaturestore()) {
      return 1;
    }
    String[] args = new String[5];
    args[0] = "add";
    args[1] = project.getName();
    args[2] = onlineDbUsername(project, user);
    args[3] = getRoleAsString(project, user);
    args[4] = pwPath;
    return onlineFeaturestoreBashScript(args);
  }
  
  /**
   * Runs the online-featurestore bash-script for an rm operation
   *
   * @param project the project of the user making the request
   * @param user the user making the request
   * @return the return-code of the bash-script
   */
  private int rmOnlineFeatureStore(String project, String user) {
    String[] args = new String[5];
    args[0] = "rm";
    args[1] = project;
    if (user.compareTo("all") == 0) {
      args[2] = user;
      
    } else {
      args[2] = onlineDbUsername(project, user);
    }
    return onlineFeaturestoreBashScript(args);
  }
  
  /**
   * Removes a user from a online feature store database in the project
   *
   * @param project the project that owns the online feature store
   * @param user the user to remove
   * @return the return-code of the bash script
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public int rmUserOnlineFeatureStore(String project, Users user) {
    if (!settings.isOnlineFeaturestore()) {
      return 1;
    }
    SecretId id = new SecretId(user.getUid(), onlineDbUsername(project, user.getUsername()));
    secretsFacade.deleteSecret(id);
    return rmOnlineFeatureStore(project, user.getUsername());
  }
  
  /**
   * Drops a online featurestore database of a project
   *
   * @param project the project for which the online featurestore database should be dropped
   * @return the return code of the bash script
   */
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
   * Gets the size of an online featurestore database. I.e the size of a MySQL-cluster database.
   *
   * @param dbName the name of the database
   * @return the size in MB
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Double getDbSize(String dbName) {
    return onlineFeaturestoreFacade.getDbSize(dbName);
  }
  
}
