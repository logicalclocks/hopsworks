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

package io.hops.hopsworks.common.airflow;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.common.dao.airflow.AirflowDag;
import io.hops.hopsworks.common.dao.airflow.AirflowDagFacade;
import io.hops.hopsworks.common.dao.airflow.MaterializedJWT;
import io.hops.hopsworks.common.dao.airflow.MaterializedJWTFacade;
import io.hops.hopsworks.common.dao.airflow.MaterializedJWTID;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.AirflowException;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.JWTException;
import org.apache.commons.codec.digest.DigestUtils;
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
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
@DependsOn("Settings")
public class AirflowManager {
  private final static Logger LOG = Logger.getLogger(AirflowManager.class.getName());
  
  private final static String TOKEN_FILE_SUFFIX = ".jwt";
  private final static Set<PosixFilePermission> TOKEN_FILE_PERMISSIONS = new HashSet<>(5);
  static {
    TOKEN_FILE_PERMISSIONS.add(PosixFilePermission.OWNER_READ);
    TOKEN_FILE_PERMISSIONS.add(PosixFilePermission.OWNER_WRITE);
    TOKEN_FILE_PERMISSIONS.add(PosixFilePermission.OWNER_EXECUTE);
  
    TOKEN_FILE_PERMISSIONS.add(PosixFilePermission.GROUP_READ);
    TOKEN_FILE_PERMISSIONS.add(PosixFilePermission.GROUP_EXECUTE);
  }
  
  private final TreeSet<AirflowJWT> airflowJWTs = new TreeSet<>(new Comparator<AirflowJWT>() {
    @Override
    public int compare(AirflowJWT t0, AirflowJWT t1) {
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
  
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private Settings settings;
  @EJB
  private JWTController jwtController;
  @EJB
  private AirflowDagFacade airflowDagFacade;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private MaterializedJWTFacade materializedJWTFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectFacade projectFacade;
  @Resource
  private TimerService timerService;
  
  private GroupPrincipal airflowGroup;
  private volatile boolean initialized = false;
  
  @PostConstruct
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void init() {
    Path airflowPath = Paths.get(settings.getAirflowDir());
    if (airflowPath.toFile().isDirectory()) {
      try {
        airflowGroup = Files.getFileAttributeView(airflowPath, PosixFileAttributeView.class,
            LinkOption.NOFOLLOW_LINKS).readAttributes().group();
        try {
          recover();
        } catch (Exception ex) {
          LOG.log(Level.WARNING, "AirflowManager failed to recover, some already running workloads might be " +
              "disrupted");
        }
        // This is a dummy query to initialize airflowPool metrics for Prometheus
        airflowDagFacade.getAllWithLimit(1);
        long interval = Math.max(1000L, settings.getJWTExpLeewaySec() * 1000 / 2);
        timerService.createIntervalTimer(10L, interval, new TimerConfig("Airflow JWT renewal", false));
        initialized = true;
      } catch (IOException | SQLException ex) {
        LOG.log(Level.SEVERE, "Failed to initialize AirflowManager", ex);
      }
    }
  }
  
  /**
   * Recover security material for Airflow after restart. Read all active material from the database.
   * Check if JWT exists in the local filesystem and it is valid.
   * If not try to create a new one. Finally, materialize X.509 for project specific user.
   */
  private void recover() {
    LOG.log(Level.FINE, "Starting Airflow manager recovery");
    List<MaterializedJWT> failed2recover = new ArrayList<>();
    Project project = null;
    Users user = null;
    // Get last known state from storage
    for (MaterializedJWT material : materializedJWTFacade.findAll4Airflow()) {
      LOG.log(Level.FINEST, "Recovering material: " + material.getIdentifier().getProjectId() + " - "
        + material.getIdentifier().getUserId());
      project = projectFacade.find(material.getIdentifier().getProjectId());
      user = userFacade.find(material.getIdentifier().getUserId());
      if (project == null || user == null) {
        LOG.log(Level.WARNING, "Error while recovering Project with ID: " + material.getIdentifier().getProjectId()
            + " and User ID: " + material.getIdentifier().getUserId() + ". Project or user is null");
        failed2recover.add(material);
        continue;
      }
      
      Path tokenFile = Paths.get(getProjectSecretsDirectory(user.getUsername()).toString(),
          getTokenFileName(project.getName(), user.getUsername()));
      AirflowJWT airflowJWT;
      String token = null;
      String materialIdentifier = "Project: " + project.getName() + " - User: " + user.getUsername();
      try {
        // First try to read JWT from the filesystem. We expect most of the cases this will succeed.
        token = FileUtils.readFileToString(tokenFile.toFile(), Charset.defaultCharset());
        DecodedJWT decoded = jwtController.verifyToken(token, settings.getJWTIssuer());
        airflowJWT = new AirflowJWT(user.getUsername(), project.getId(), project.getName(),
            DateUtils.date2LocalDateTime(decoded.getExpiresAt()), user.getUid());
        airflowJWT.tokenFile = tokenFile;
        airflowJWT.token = token;
        LOG.log(Level.FINE, "Successfully read existing JWT from local filesystem for " + materialIdentifier);
      } catch (IOException | JWTException | JWTDecodeException ex) {
        // JWT does not exist in the filesystem or we cannot read them or it is not valid any longer
        // We will create a new one
        //TODO(Antonis): Not very good that audience is hardcoded, but it is not accessible from hopsworks-common
        String[] audience = new String[]{"api"};
        LocalDateTime expirationDate = DateUtils.getNow().plus(settings.getJWTLifetimeMs(), ChronoUnit.MILLIS);
        String[] roles = getUserRoles(user);
        try {
          LOG.log(Level.FINEST, "JWT for " + materialIdentifier + " does not exist in the local FS or it is not "
              + "valid any longer, creating new one...");
          Map<String, Object> claims = new HashMap<>(3);
          claims.put(Constants.RENEWABLE, false);
          claims.put(Constants.EXPIRY_LEEWAY, settings.getJWTExpLeewaySec());
          claims.put(Constants.ROLES, roles);
          token = jwtController.createToken(settings.getJWTSigningKeyName(), false, settings.getJWTIssuer(),
              audience, DateUtils.localDateTime2Date(expirationDate),
              DateUtils.localDateTime2Date(DateUtils.getNow()), user.getUsername(),
              claims, SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg()));
          airflowJWT = new AirflowJWT(user.getUsername(), project.getId(), project.getName(),
              expirationDate, user.getUid());
          airflowJWT.tokenFile = tokenFile;
          airflowJWT.token = token;
          writeTokenToFile(airflowJWT);
          LOG.log(Level.FINE, "Created new JWT for " + materialIdentifier + " and flushed to local FS");
        } catch (IOException ex1) {
          // Managed to create token but failed to write
          LOG.log(Level.WARNING, "Could not write to local FS new JWT for recovered material " + materialIdentifier
            + ". We will invalidate it and won't renew it.", ex1);
          if (token != null) {
            try {
              LOG.log(Level.FINE, "Failed to write JWT for " + materialIdentifier + ". Invalidating it...");
              jwtController.invalidate(token);
            } catch (InvalidationException ex2) {
              // Not much we can do about it
            }
          }
          failed2recover.add(material);
          continue;
        } catch (GeneralSecurityException | JWTException ex1) {
          LOG.log(Level.WARNING, "Tried to recover JWT for " + materialIdentifier + " but we failed. Giving up... " +
              "JWT will not be available for Airflow DAGs", ex1);
          // Initial token is invalid and could not create new. Give up
          failed2recover.add(material);
          continue;
        }
      }
      
      // If everything went fine with JWT, proceed with X.509
      try {
        LOG.log(Level.FINEST, "Materializing X.509 for " + materialIdentifier);
        certificateMaterializer.materializeCertificatesLocalCustomDir(user.getUsername(), project.getName(),
            getProjectSecretsDirectory(user.getUsername()).toString());
        LOG.log(Level.FINE, "Materialized X.509 for " + materialIdentifier);
        airflowJWTs.add(airflowJWT);
      } catch (IOException ex) {
        LOG.log(Level.WARNING, "Could not materialize X.509 for " + materialIdentifier
            + " Invalidating JWT and deleting from FS. JWT and X.509 will not be available for Airflow DAGs.", ex);
        // Could not materialize X.509
        if (token != null) {
          try {
            LOG.log(Level.FINE, "Failed to materialize X.509 for " + materialIdentifier
              + " Invalidating JWT and deleting it from local FS.");
            jwtController.invalidate(token);
            FileUtils.deleteDirectory(getProjectSecretsDirectory(user.getUsername()).toFile());
          } catch (InvalidationException | IOException ex1) {
            // Not much we can do about it
          }
        }
        failed2recover.add(material);
      }
    }
    
    // Remove failed material from persistent storage
    for (MaterializedJWT failed : failed2recover) {
      materializedJWTFacade.delete(failed.getIdentifier());
    }
  }
  
  private String[] getUserRoles(Users p) {
    Collection<BbcGroup> groupList = p.getBbcGroupCollection();
    String[] roles = new String[groupList.size()];
    int idx = 0;
    for (BbcGroup g : groupList) {
      roles[idx] = g.getGroupName();
      idx++;
    }
    return roles;
  }
  
  private void isInitialized() throws AirflowException {
    if (!initialized) {
      throw new AirflowException(RESTCodes.AirflowErrorCode.AIRFLOW_MANAGER_UNINITIALIZED, Level.WARNING,
          "AirflowManager is not initialized",
          "AirflowManager failed to initialize or Airflow is not deployed");
    }
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 5, unit = TimeUnit.SECONDS)
  public void onProjectRemoval(Project project) throws IOException {
    if (initialized) {
      FileUtils.deleteDirectory(getProjectDagDirectory(project.getId()).toFile());
      // Airflow material will be deleted from the database by the foreign key constraint
      // Monitor will reap all material that do not exist in the database
    }
  }
  
  @Lock(LockType.READ)
  @AccessTimeout(value = 1, unit = TimeUnit.SECONDS)
  public void prepareSecurityMaterial(Users user, Project project, String[] audience) throws AirflowException {
    isInitialized();
    MaterializedJWTID materialID = new MaterializedJWTID(project.getId(), user.getUid(),
        MaterializedJWTID.USAGE.AIRFLOW);
    if (!materializedJWTFacade.exists(materialID)) {
      LocalDateTime expirationDate = DateUtils.getNow().plus(settings.getJWTLifetimeMs(), ChronoUnit.MILLIS);
      AirflowJWT airflowJWT = new AirflowJWT(user.getUsername(), project.getId(), project.getName(), expirationDate,
          user.getUid());
      try {
        String[] roles = getUserRoles(user);
        MaterializedJWT airflowMaterial = new MaterializedJWT(new MaterializedJWTID(project.getId(), user.getUid(),
            MaterializedJWTID.USAGE.AIRFLOW));
        materializedJWTFacade.persist(airflowMaterial);
        Map<String, Object> claims = new HashMap<>(3);
        claims.put(Constants.RENEWABLE, false);
        claims.put(Constants.EXPIRY_LEEWAY, settings.getJWTExpLeewaySec());
        claims.put(Constants.ROLES, roles);
        String token = jwtController.createToken(settings.getJWTSigningKeyName(), false, settings.getJWTIssuer(),
            audience, DateUtils.localDateTime2Date(expirationDate),
            DateUtils.localDateTime2Date(DateUtils.getNow()), user.getUsername(),
            claims, SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg()));
        String projectAirflowDir = getProjectSecretsDirectory(user.getUsername()).toString();
        airflowJWT.tokenFile = Paths.get(projectAirflowDir, getTokenFileName(project.getName(), user.getUsername()));
        
        airflowJWT.token = token;
        writeTokenToFile(airflowJWT);
        certificateMaterializer.materializeCertificatesLocalCustomDir(user.getUsername(), project.getName(),
            projectAirflowDir);
        airflowJWTs.add(airflowJWT);
      } catch (GeneralSecurityException | JWTException ex) {
        deleteAirflowMaterial(materialID);
        throw new AirflowException(RESTCodes.AirflowErrorCode.JWT_NOT_CREATED, Level.SEVERE,
            "Could not generate Airflow JWT for user " + user.getUsername(), ex.getMessage(), ex);
      } catch (IOException ex) {
        LOG.log(Level.WARNING, "Could not write Airflow JWT for user " + hdfsUsersController
            .getHdfsUserName(project, user), ex);
        deleteAirflowMaterial(materialID);
        try {
          jwtController.invalidate(airflowJWT.token);
        } catch (InvalidationException invEx) {
          LOG.log(Level.FINE, "Could not invalidate Airflow JWT. Skipping...", ex);
        }
        throw new AirflowException(RESTCodes.AirflowErrorCode.JWT_NOT_STORED, Level.SEVERE,
            "Could not store Airflow JWT for user " + hdfsUsersController.getHdfsUserName(project, user),
            ex.getMessage(), ex);
      }
    }
  }
  
  /**
   * Timer bean to periodically (JWT expiration lee way / 2) (a) clean stale JWT and X.509 material for Airflow
   * and (b) renew used JWTs.
   *
   * a. Iterate all the material in memory and clean those that don't have any entry in the database, project has been
   * deleted or those that project exist but user does not own any non-paused DAG in Airflow.
   *
   * b. For a valid JWT, renew it if the time has come (after expiration time and before expiration + expLeeWay)
   *
   * @param timer
   */
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 500)
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  @Timeout
  public void monitorSecurityMaterial(Timer timer) {
    try {
      LocalDateTime now = DateUtils.getNow();
      // Clean unused token files and X.509 certificates
      cleanStaleSecurityMaterial();

      // Renew them
      Set<AirflowJWT> newTokens2Add = new HashSet<>();
      Iterator<AirflowJWT> airflowJWTIt = airflowJWTs.iterator();
      while (airflowJWTIt.hasNext()) {
        AirflowJWT airflowJWT = airflowJWTIt.next();
        // Set is sorted by expiration date
        // If first does not need to be renewed, neither do the rest
        if (airflowJWT.maybeRenew(now)) {
          try {
            LocalDateTime expirationDateTime = now.plus(settings.getJWTLifetimeMs(), ChronoUnit.MILLIS);
            Date expirationDate = DateUtils.localDateTime2Date(expirationDateTime);
            String token = jwtController.renewToken(airflowJWT.token, expirationDate,
                DateUtils.localDateTime2Date(DateUtils.getNow()), true, new HashMap<>(3));

            AirflowJWT renewedJWT = new AirflowJWT(airflowJWT.username, airflowJWT.projectId, airflowJWT.projectName,
                expirationDateTime, airflowJWT.uid);
            renewedJWT.tokenFile = airflowJWT.tokenFile;
            renewedJWT.token = token;

            airflowJWTIt.remove();
            writeTokenToFile(renewedJWT);
            newTokens2Add.add(renewedJWT);
          } catch (JWTException ex) {
            LOG.log(Level.WARNING, "Could not renew Airflow JWT for " + airflowJWT, ex);
          } catch (IOException ex) {
            LOG.log(Level.WARNING, "Could not write renewed Airflow JWT for " + airflowJWT, ex);
            try {
              jwtController.invalidate(airflowJWT.token);
            } catch (InvalidationException iex) {
              LOG.log(Level.FINE, "Could not invalidate Airflow JWT. SKipping...");
            }
          } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Generic error while renewing Airflow JWTs", ex);
          }
        } else {
          break;
        }
      }
      airflowJWTs.addAll(newTokens2Add);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Got an exception while renewing/invalidating airflow jwt token", e);
    }
  }
  
  public Path getProjectDagDirectory(Integer projectID) {
    return Paths.get(settings.getAirflowDir(), "dags", generateProjectSecret(projectID));
  }
  
  private Path getProjectSecretsDirectory(String username) {
    return Paths.get(settings.getAirflowDir(), "secrets", generateOwnerSecret(username));
  }
  
  private String generateProjectSecret(Integer projectID) {
    return DigestUtils.sha256Hex(Integer.toString(projectID));
  }
  
  private String generateOwnerSecret(String username) {
    return DigestUtils.sha256Hex(username);
  }
  
  private void writeTokenToFile(AirflowJWT airflowJWT) throws IOException {
    Path parent = airflowJWT.tokenFile.getParent();
    if (!parent.toFile().exists()) {
      parent.toFile().mkdirs();
      Files.setPosixFilePermissions(parent, TOKEN_FILE_PERMISSIONS);
      Files.getFileAttributeView(parent, PosixFileAttributeView.class,
          LinkOption.NOFOLLOW_LINKS).setGroup(airflowGroup);
    }
    FileUtils.writeStringToFile(airflowJWT.tokenFile.toFile(), airflowJWT.token);
    Files.setPosixFilePermissions(airflowJWT.tokenFile, TOKEN_FILE_PERMISSIONS);
    Files.getFileAttributeView(airflowJWT.tokenFile, PosixFileAttributeView.class,
        LinkOption.NOFOLLOW_LINKS).setGroup(airflowGroup);
  }
  
  private void deleteAirflowMaterial(MaterializedJWTID identifier) {
    materializedJWTFacade.delete(identifier);
  }
  
  private String getTokenFileName(String projectName, String username) {
    return projectName + "__" + username + TOKEN_FILE_SUFFIX;
  }
  
  private boolean deleteDirectoryIfEmpty(Path directory) throws IOException {
    File directoryFile = directory.toFile();
    File[] content = directoryFile.listFiles();
    if (content != null && content.length == 0) {
      FileUtils.deleteDirectory(directoryFile);
      return true;
    }
    return false;
  }
  
  private void cleanStaleSecurityMaterial() {
    Iterator<AirflowJWT> airflowJWTsIt = airflowJWTs.iterator();
    while (airflowJWTsIt.hasNext()) {
      AirflowJWT nextElement = airflowJWTsIt.next();
      try {
        MaterializedJWTID materialId = new MaterializedJWTID(nextElement.projectId, nextElement.uid,
            MaterializedJWTID.USAGE.AIRFLOW);
        MaterializedJWT airflowMaterial = materializedJWTFacade.findById(materialId);
        boolean shouldDelete = true;
        
        if (airflowMaterial != null) {
          List<AirflowDag> ownedDags = airflowDagFacade.filterByOwner(nextElement.username);
          for (AirflowDag dag : ownedDags) {
            if (!dag.getPaused()) {
              shouldDelete = false;
              break;
            }
          }
        }
        
        if (shouldDelete) {
          certificateMaterializer.removeCertificatesLocalCustomDir(nextElement.username, nextElement.projectName,
              getProjectSecretsDirectory(nextElement.username).toString());
          
          FileUtils.deleteQuietly(nextElement.tokenFile.toFile());
          airflowJWTsIt.remove();
          if (airflowMaterial != null) {
            deleteAirflowMaterial(materialId);
          }
          deleteDirectoryIfEmpty(nextElement.tokenFile.getParent());
        }
      } catch (Exception ex) {
        // Catch everything here. We don't want the timer thread to get killed (expunging timer)
        // Be on the safe side and renew the token
        LOG.log(Level.WARNING, "Could not determine if token " + nextElement + " is stale. It will be renewed!", ex);
      }
    }
  }
  
  private class AirflowJWT {
    private final String username;
    private final Integer projectId;
    private final String projectName;
    private final LocalDateTime expiration;
    private final Integer uid;
    
    private String token;
    private Path tokenFile;
    
    private AirflowJWT(String username, Integer projectId, String projectName, LocalDateTime expiration, Integer uid) {
      this.username = username;
      this.projectId = projectId;
      this.projectName = projectName;
      this.expiration = expiration;
      this.uid = uid;
    }
    
    private boolean maybeRenew(LocalDateTime now) {
      return now.isAfter(expiration) || now.isEqual(expiration);
    }
    
    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + uid;
      result = 31 * result + projectId;
      return result;
    }
    
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof AirflowJWT) {
        AirflowJWT other = (AirflowJWT) o;
        return uid.equals(other.uid) && projectId.equals(other.projectId);
      }
      return false;
    }
    
    @Override
    public String toString() {
      return "Airflow JWT - Project ID: " + projectId + " User ID: " + uid;
    }
  }
}
