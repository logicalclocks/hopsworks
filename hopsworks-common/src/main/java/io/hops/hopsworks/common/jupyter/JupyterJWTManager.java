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

package io.hops.hopsworks.common.jupyter;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.common.dao.airflow.MaterializedJWT;
import io.hops.hopsworks.common.dao.airflow.MaterializedJWTFacade;
import io.hops.hopsworks.common.dao.airflow.MaterializedJWTID;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterManager;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
@DependsOn("Settings")
public class JupyterJWTManager {
  private static final Logger LOG = Logger.getLogger(JupyterJWTManager.class.getName());
  public static final String TOKEN_FILE_NAME = "token.jwt";
  
  private final TreeSet<JupyterJWT> jupyterJWTs = new TreeSet<>(new Comparator<JupyterJWT>() {
    @Override
    public int compare(JupyterJWT t0, JupyterJWT t1) {
      if (t0.equals(t1)) {
        return 0;
      } else {
        if (t0.expiration.isBefore(t1.expiration)) {
          return -1;
        } else if (t0.expiration.isAfter(t1.expiration)) {
          return 1;
        }
        return 0;
      }
    }
  });

  private final HashMap<PidAndPort, JupyterJWT> pidAndPortToJWT = new HashMap<>();

  @EJB
  private Settings settings;
  @EJB
  private MaterializedJWTFacade materializedJWTFacade;
  @EJB
  private JWTController jwtController;
  @EJB
  private UsersController usersController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private JupyterFacade jupyterFacade;
  @Inject
  private JupyterManager jupyterManager;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  @Inject
  private JupyterJWTTokenWriter jupyterJWTTokenWriter;
  @Resource
  private TimerService timerService;
  
  @PostConstruct
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void init() {
    try {
      recover();
    } catch (Exception ex) {
      LOG.log(Level.WARNING, "Exception while recovering Jupyter JWTs. Keep going on...", ex);
    }
    long monitorInterval = 5000L;
    timerService.createIntervalTimer(1000L, monitorInterval, new TimerConfig("Jupyter JWT renewal service", false));
  }

  private void addToken(JupyterJWT jupyterJWT) {
    jupyterJWTs.add(jupyterJWT);
    pidAndPortToJWT.put(jupyterJWT.pidAndPort, jupyterJWT);
  }

  private void removeToken(PidAndPort pidAndPort) {
    JupyterJWT jupyterJWT = pidAndPortToJWT.remove(pidAndPort);
    jupyterJWTs.remove(jupyterJWT);
  }

  private void recover() {
    LOG.log(INFO, "Starting Jupyter JWT manager recovery");
    List<MaterializedJWT> failed2recover = new ArrayList<>();
    
    // Get state from the database
    for (MaterializedJWT materializedJWT : materializedJWTFacade.findAll4Jupyter()) {
      LOG.log(Level.FINEST, "Recovering Jupyter JWT " + materializedJWT.getIdentifier());
      
      // First lookup project and user in db
      Project project = projectFacade.find(materializedJWT.getIdentifier().getProjectId());
      Users user = userFacade.find(materializedJWT.getIdentifier().getUserId());
      if (project == null || user == null) {
        LOG.log(Level.WARNING, "Tried to recover " + materializedJWT.getIdentifier() + " but could not find " +
          "either Project or User");
        failed2recover.add(materializedJWT);
        continue;
      }
      
      // Get Jupyter configuration from db
      String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
      JupyterProject jupyterProject = jupyterFacade.findByUser(hdfsUsername);
      if (jupyterProject == null) {
        LOG.log(Level.FINEST, "There is no Jupyter configuration persisted for " + materializedJWT.getIdentifier());
        failed2recover.add(materializedJWT);
        continue;
      }
      
      // Check if Jupyter is still running
      if (!jupyterManager.ping(jupyterProject)) {
        LOG.log(Level.FINEST, "Jupyter server is not running for " + materializedJWT.getIdentifier()
          + " Skip recovering...");
        failed2recover.add(materializedJWT);
        continue;
      }
      
      JupyterSettings jupyterSettings = jupyterSettingsFacade.findByProjectUser(project.getId(), user.getEmail());
      
      Path tokenFile = constructTokenFilePath(jupyterSettings);
      String token = null;
      JupyterJWT jupyterJWT = null;
      PidAndPort pidAndPort = new PidAndPort(jupyterProject.getPid(), jupyterProject.getPort());
      try {
        token = FileUtils.readFileToString(tokenFile.toFile());
        DecodedJWT decodedJWT = jwtController.verifyToken(token, settings.getJWTIssuer());
        jupyterJWT = new JupyterJWT(project, user, pidAndPort, DateUtils.date2LocalDateTime(decodedJWT.getExpiresAt()));
        jupyterJWT.token = token;
        jupyterJWT.tokenFile = tokenFile;
        LOG.log(Level.FINE, "Successfully read existing JWT from local filesystem");
      } catch (IOException | JWTException | JWTDecodeException ex) {
        LOG.log(Level.FINE, "Could not recover Jupyter JWT from local filesystem, generating new!", ex);
        // JWT does not exist or it is not valid any longer
        // We should create a new one
        String[] audience = new String[]{"api"};
        LocalDateTime expirationDate = LocalDateTime.now().plus(settings.getJWTLifetimeMs(), ChronoUnit.MILLIS);
        String[] userRoles = usersController.getUserRoles(user).toArray(new String[1]);
        try {
          Map<String, Object> claims = new HashMap<>(3);
          claims.put(Constants.RENEWABLE, false);
          claims.put(Constants.EXPIRY_LEEWAY, settings.getJWTExpLeewaySec());
          claims.put(Constants.ROLES, userRoles);
          token = jwtController.createToken(settings.getJWTSigningKeyName(), false, settings.getJWTIssuer(),
              audience, DateUtils.localDateTime2Date(expirationDate), DateUtils.localDateTime2Date(DateUtils.getNow()),
              user.getUsername(), claims, SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg()));
          jupyterJWT = new JupyterJWT(project, user, pidAndPort, expirationDate);
          jupyterJWT.token = token;
          jupyterJWT.tokenFile = tokenFile;
          jupyterJWTTokenWriter.writeToken(settings, jupyterJWT);
          LOG.log(Level.FINE, "Generated new Jupyter JWT cause could not recover existing");
        } catch (IOException recIOEx) {
          LOG.log(Level.WARNING, "Failed to recover Jupyter JWT for " + materializedJWT.getIdentifier()
            + ", generated new valid JWT but failed to write to local filesystem. Invalidating new token!" +
            " Continue recovering...");
          if (token != null) {
            try {
              jwtController.invalidate(token);
            } catch (InvalidationException jwtInvEx) {
              // NO-OP
            }
          }
          failed2recover.add(materializedJWT);
          continue;
        } catch (GeneralSecurityException | JWTException jwtEx) {
          LOG.log(Level.WARNING, "Failed to recover Jupyter JWT for " + materializedJWT.getIdentifier()
            + ", tried to generate new token and it failed as well. Could not recover! Continue recovering...");
          // Did our best, it's good to know when you should give up
          failed2recover.add(materializedJWT);
          continue;
        }
      }
      addToken(jupyterJWT);
    }
    
    // Remove from the database entries that we failed to recover
    for (MaterializedJWT failedRecovery : failed2recover) {
      materializedJWTFacade.delete(failedRecovery.getIdentifier());
    }
    LOG.log(INFO, "Finished Jupyter JWT recovery");
  }
  
  private Path constructTokenFilePath(JupyterSettings jupyterSettings) {
    return Paths.get(settings.getStagingDir(), Settings.PRIVATE_DIRS, jupyterSettings.getSecret(), TOKEN_FILE_NAME);
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 2000)
  public void materializeJWT(Users user, Project project, JupyterSettings jupyterSettings, Long pid,
      Integer port, String[] audience) throws ServiceException {
    MaterializedJWTID materialID = new MaterializedJWTID(project.getId(), user.getUid(),
      MaterializedJWTID.USAGE.JUPYTER);
    if (!materializedJWTFacade.exists(materialID)) {
      LocalDateTime expirationDate = LocalDateTime.now().plus(settings.getJWTLifetimeMs(), ChronoUnit.MILLIS);
      JupyterJWT jupyterJWT = new JupyterJWT(project, user, new PidAndPort(pid, port), expirationDate);
      try {
        String[] roles = usersController.getUserRoles(user).toArray(new String[1]);
        MaterializedJWT materializedJWT = new MaterializedJWT(materialID);
        materializedJWTFacade.persist(materializedJWT);
        
        Map<String, Object> claims = new HashMap<>(3);
        claims.put(Constants.RENEWABLE, false);
        claims.put(Constants.EXPIRY_LEEWAY, settings.getJWTExpLeewaySec());
        claims.put(Constants.ROLES, roles);
        String token = jwtController.createToken(settings.getJWTSigningKeyName(), false, settings.getJWTIssuer(),
            audience, DateUtils.localDateTime2Date(expirationDate), DateUtils.localDateTime2Date(DateUtils.getNow()),
            user.getUsername(), claims, SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg()));
        
        jupyterJWT.tokenFile = constructTokenFilePath(jupyterSettings);
        
        jupyterJWT.token = token;
        jupyterJWTTokenWriter.writeToken(settings, jupyterJWT);
        
        addToken(jupyterJWT);
      } catch (GeneralSecurityException | JWTException ex) {
        LOG.log(Level.SEVERE, "Error generating Jupyter JWT for " + jupyterJWT, ex);
        materializedJWTFacade.delete(materialID);
        throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.SEVERE,
          "Could not generate Jupyter JWT", ex.getMessage(), ex);
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Error writing Jupyter JWT to file for " + jupyterJWT, ex);
        materializedJWTFacade.delete(materialID);
        try {
          jwtController.invalidate(jupyterJWT.token);
        } catch (InvalidationException invEx) {
          LOG.log(Level.FINE, "Could not invalidate Jupyter JWT after failure to write to file", ex);
        }
        throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.SEVERE,
          "Could not write Jupyter JWT to file", ex.getMessage(), ex);
      }
    }
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 500)
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  @Timeout
  public void monitorJupyterJWT() {
    try {
      // Renew the rest of them
      Set<JupyterJWT> renewedJWTs = new HashSet<>(this.jupyterJWTs.size());
      Iterator<JupyterJWT> jupyterJWTs = this.jupyterJWTs.iterator();
      LocalDateTime now = DateUtils.getNow();

      while (jupyterJWTs.hasNext()) {
        JupyterJWT element = jupyterJWTs.next();
        // Elements are sorted by their expiration date.
        // If element N does not need to be renewed neither does N+1
        if (element.maybeRenew(now)) {
          LocalDateTime newExpirationDate = now.plus(settings.getJWTLifetimeMs(), ChronoUnit.MILLIS);
          String newToken = null;
          try {
            newToken = jwtController.renewToken(element.token, DateUtils.localDateTime2Date(newExpirationDate),
                DateUtils.localDateTime2Date(now), true, new HashMap<>(3));

            JupyterJWT renewedJWT = new JupyterJWT(element.project, element.user,
                element.pidAndPort, newExpirationDate);
            renewedJWT.tokenFile = element.tokenFile;
            renewedJWT.token = newToken;
            jupyterJWTTokenWriter.writeToken(settings, renewedJWT);
            renewedJWTs.add(renewedJWT);
          } catch (JWTException ex) {
            LOG.log(Level.WARNING, "Could not renew Jupyter JWT for " + element, ex);
          } catch (IOException ex) {
            LOG.log(Level.WARNING, "Could not write renewed Jupyter JWT to file for " + element, ex);
            if (newToken != null) {
              try {
                jwtController.invalidate(newToken);
              } catch (InvalidationException invEx) {
                LOG.log(Level.FINE, "Could not invalidate failed token", invEx);
              }
            }
          } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Generic error renewing Jupyter JWT for " + element, ex);
          }
        } else {
          break;
        }
      }
      renewedJWTs.forEach(t -> {
        removeToken(t.pidAndPort);
        addToken(t);
      });
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Got an exception while renewing jupyter jwt token" , e);
    }
  }

  @Lock(LockType.WRITE)
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void cleanJWT(Long pid, Integer port) {
    Optional<JupyterJWT> optional = Optional.ofNullable(pidAndPortToJWT.get(new PidAndPort(pid, port)));

    if (!optional.isPresent()) {
      LOG.log(WARNING, "JupyterJWT not found for pid " + pid + " and port " + port);
      return;
    }

    JupyterJWT element = optional.get();
    try {
      MaterializedJWTID materializedJWTID = new MaterializedJWTID(element.project.getId(), element.user.getUid(),
        MaterializedJWTID.USAGE.JUPYTER);
      MaterializedJWT material = materializedJWTFacade.findById(materializedJWTID);
      jupyterJWTTokenWriter.deleteToken(element);
      if (material != null) {
        materializedJWTFacade.delete(materializedJWTID);
      }
      removeToken(element.pidAndPort);
      jwtController.invalidate(element.token);
    } catch (Exception ex) {
      // Catch everything and do not fail. If we failed to determine the status of Jupyter, we renew the token
      // to be safe
      LOG.log(Level.FINE, "Could not determine if Jupyter JWT for " + element + " is still valid. Renewing it...");
    }
  }
}
