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

package io.hops.hopsworks.common.airflow;

import io.hops.hopsworks.common.dao.airflow.AirflowDag;
import io.hops.hopsworks.common.dao.airflow.AirflowDagFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.AirflowException;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
@DependsOn("Settings")
public class AirflowJWTManager {
  private final static Logger LOG = Logger.getLogger(AirflowJWTManager.class.getName());
  
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
  private UsersController usersController;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @Resource
  private TimerService timerService;
  
  private GroupPrincipal airflowGroup;
  
  @PostConstruct
  public void init() throws RuntimeException {
    try {
      Path airflowPath = Paths.get(settings.getAirflowDir());
      airflowGroup = Files.getFileAttributeView(airflowPath, PosixFileAttributeView.class,
          LinkOption.NOFOLLOW_LINKS).readAttributes().group();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    long interval = Math.max(3000L, settings.getJWTExpLeewaySec() * 1000 / 2);
    timerService.createIntervalTimer(10L, interval, new TimerConfig("Airflow JWT renewal", false));
  }
  
  @Lock(LockType.READ)
  @AccessTimeout(value = 1, unit = TimeUnit.SECONDS)
  public void prepareSecurityMaterial(Users user, Project project, String[] audience) throws AirflowException {
    LocalDateTime expirationDate = getNow().plus(settings.getJWTLifetimeMs(), ChronoUnit.MILLIS);
    AirflowJWT airflowJWT = new AirflowJWT(user.getUsername(), project.getId(), project.getName(), expirationDate);
    // We use AirflowJWT references to check both for JWT and X.509
    if (!airflowJWTs.contains(airflowJWT)) {
      try {
        String[] roles = usersController.getUserRoles(user).toArray(new String[1]);
        String token = jwtController.createToken(settings.getJWTSigningKeyName(), false, settings.getJWTIssuer(),
            audience, localDateTime2Date(expirationDate), localDateTime2Date(getNow()), user.getUsername(),
            false, settings.getJWTExpLeewaySec(), roles,
            SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg()));
        String projectAirflowDir = getProjectSecretsDirectory(user.getUsername()).toString();
        airflowJWT.tokenFile = Paths.get(projectAirflowDir,getTokenFileName(project.getName(), user.getUsername()));
        
        airflowJWT.token = token;
        writeTokenToFile(airflowJWT);
        certificateMaterializer.materializeCertificatesLocalCustomDir(user.getUsername(), project.getName(),
            projectAirflowDir);
        airflowJWTs.add(airflowJWT);
      } catch (GeneralSecurityException | JWTException ex) {
        throw new AirflowException(RESTCodes.AirflowErrorCode.JWT_NOT_CREATED, Level.SEVERE,
            "Could not generate Airflow JWT for user " + user.getUsername(), ex.getMessage(), ex);
      } catch (IOException ex) {
        LOG.log(Level.WARNING, "Could not write Airflow JWT for user " + hdfsUsersController
            .getHdfsUserName(project, user), ex);
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
  
  private String getTokenFileName(String projectName, String username) {
    return projectName + "__" + username + TOKEN_FILE_SUFFIX;
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 500)
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  @Timeout
  public void monitorSecurityMaterial(Timer timer) {
    try {
      String applicationName = InitialContext.doLookup("java:app/AppName");
      String moduleName = InitialContext.doLookup("java:module/ModuleName");
      if(applicationName.contains("hopsworks-ca") || moduleName.contains("hopsworks-ca")){
        return;
      }
    } catch (NamingException e) {
      LOG.log(Level.SEVERE, null, e);
    }
    LocalDateTime now = getNow();
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
          Date expirationDate = localDateTime2Date(expirationDateTime);
          String token = jwtController.renewToken(airflowJWT.token, expirationDate,
              Date.from(now.toInstant(ZoneOffset.UTC)));
          
          AirflowJWT renewedJWT = new AirflowJWT(airflowJWT.username, airflowJWT.projectId, airflowJWT.projectName,
              expirationDateTime);
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
  }
  
  private LocalDateTime getNow() {
    return LocalDateTime.now();
  }
  
  private Date localDateTime2Date(LocalDateTime time) {
    return Date.from(time.toInstant(ZoneOffset.UTC));
  }
  
  public Path getProjectDagDirectory(Integer projectID) {
    return Paths.get(settings.getAirflowDir(), "dags", generateProjectSecret(projectID));
  }
  
  public Path getProjectSecretsDirectory(String username) {
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
  
  private void cleanStaleSecurityMaterial() {
    Iterator<AirflowJWT> airflowJWTsIt = airflowJWTs.iterator();
    while (airflowJWTsIt.hasNext()) {
      AirflowJWT nextElement = airflowJWTsIt.next();
      try {
        List<AirflowDag> ownedDags = airflowDagFacade.filterByOwner(nextElement.username);
        boolean shouldDelete = true;
        for (AirflowDag dag : ownedDags) {
          if (!dag.getPaused()) {
            shouldDelete = false;
            break;
          }
        }
        if (shouldDelete) {
          certificateMaterializer.removeCertificatesLocalCustomDir(nextElement.username, nextElement.projectName,
              getProjectSecretsDirectory(nextElement.username).toString());
          
          FileUtils.deleteQuietly(nextElement.tokenFile.toFile());
          airflowJWTsIt.remove();
          File[] content = nextElement.tokenFile.getParent().toFile().listFiles();
          if (content != null && content.length == 0) {
            FileUtils.deleteDirectory(nextElement.tokenFile.getParent().toFile());
          }
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
    
    private String token;
    private Path tokenFile;
    
    private AirflowJWT(String username, Integer projectId, String projectName, LocalDateTime expiration) {
      this.username = username;
      this.projectId = projectId;
      this.projectName = projectName;
      this.expiration = expiration;
    }
    
    private boolean maybeRenew(LocalDateTime now) {
      return now.isAfter(expiration) || now.isEqual(expiration);
    }
    
    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + username.hashCode();
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
        return username.equals(other.username) && projectId.equals(other.projectId);
      }
      return false;
    }
    
    @Override
    public String toString() {
      return "Airflow JWT <" + username + ">";
    }
  }
}
