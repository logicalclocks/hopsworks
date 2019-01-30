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

import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AirflowException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.JWTController;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
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
  
  private final static int MINUTE = 60;
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
      if (t0.expiration.isBefore(t1.expiration)) {
        return -1;
      } else if (t0.expiration.isAfter(t1.expiration)) {
        return 1;
      }
      return 0;
    }
  });
  
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private Settings settings;
  @EJB
  private JWTController jwtController;
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
    timerService.createIntervalTimer(10L, 10000L, new TimerConfig("Airflow JWT renewal", false));
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 1, unit = TimeUnit.SECONDS)
  public void storeJWT(Users user, Project project, String token, LocalDateTime expiration) throws AirflowException {
    
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    Path tokenFile = Paths.get(getSecretProjectDirectory(project).toString(), projectUsername + TOKEN_FILE_SUFFIX);
    
    AirflowJWT airflowJWT = new AirflowJWT(projectUsername, token, expiration, tokenFile);
    try {
      writeTokenToFile(airflowJWT);
      airflowJWTs.add(airflowJWT);
    } catch (IOException ex) {
      throw new AirflowException(RESTCodes.AirflowErrorCode.JWT_NOT_STORED, Level.SEVERE,
          "Could not store JWT for Airflow", ex.getMessage(), ex);
    }
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 500)
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  @Timeout
  public void renewJWTMonitor(Timer timer) {
    LocalDateTime now = LocalDateTime.now();
    // Clean unused token files
    cleanUnusedTokenFiles(now);
  
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
          Date newExpiration = Date.from(expirationDateTime.toInstant(ZoneOffset.UTC));
          String token = jwtController.renewToken(airflowJWT.token, newExpiration,
              Date.from(now.toInstant(ZoneOffset.UTC)));
          
          DecodedJWT renewed = jwtController.decodeToken(token);
          AirflowJWT renewedJWT = new AirflowJWT(airflowJWT.projectUsername, renewed.getToken(), expirationDateTime,
              airflowJWT.tokenFile);
          airflowJWTIt.remove();
          newTokens2Add.add(renewedJWT);
          // Preserve atime
          FileTime atime = getAtime(renewedJWT.tokenFile);
          writeTokenToFile(renewedJWT);
          setAtime(renewedJWT.tokenFile, atime);
        } catch (JWTException | IOException ex) {
          // Do not abort renewing other tokens
          LOG.log(Level.WARNING, "Unable to renew token <" + airflowJWT.tokenFile + ">", ex);
        }
      } else {
        break;
      }
    }
    airflowJWTs.addAll(newTokens2Add);
  }
  
  public Path getSecretProjectDirectory(Project project) {
    return Paths.get(settings.getAirflowDir(), "dags", generateProjectSecret(project));
  }
  
  private String generateProjectSecret(Project project) {
    return DigestUtils.sha256Hex(Integer.toString(project.getId()));
  }
  
  private void writeTokenToFile(AirflowJWT airflowJWT) throws IOException {
    FileUtils.writeStringToFile(airflowJWT.tokenFile.toFile(), airflowJWT.token);
    Files.setPosixFilePermissions(airflowJWT.tokenFile, TOKEN_FILE_PERMISSIONS);
    Files.getFileAttributeView(airflowJWT.tokenFile, PosixFileAttributeView.class,
        LinkOption.NOFOLLOW_LINKS).setGroup(airflowGroup);
  }
  
  private void cleanUnusedTokenFiles(LocalDateTime now) {
    Iterator<AirflowJWT> airflowJWTsIt = airflowJWTs.iterator();
    while (airflowJWTsIt.hasNext()) {
      AirflowJWT nextElement = airflowJWTsIt.next();
      try {
        if (nextElement.maybeDelete(now)) {
          FileUtils.deleteQuietly(nextElement.tokenFile.toFile());
          LOG.fine(">>>>> Deleted: " + nextElement.tokenFile.toFile());
          airflowJWTsIt.remove();
        }
      } catch (IOException ex) {
        // Be on the safe side and renew the token
        LOG.log(Level.WARNING, "Could not determine if token " + nextElement + " is stale. It will be renewed!", ex);
      }
    }
  }
  
  private FileTime getAtime(Path file) throws IOException {
    BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class,
        LinkOption.NOFOLLOW_LINKS);
    return attributes.lastAccessTime();
  }
  
  private void setAtime(Path file, FileTime atime) throws IOException {
    Files.setAttribute(file, "lastAccessTime", atime, LinkOption.NOFOLLOW_LINKS);
  }
  
  private class AirflowJWT {
    private final String projectUsername;
    private final LocalDateTime expiration;
    private final String token;
    private final Path tokenFile;
    
    private AirflowJWT(String projectUsername, String token, LocalDateTime expiration, Path tokenFile) {
      this.projectUsername = projectUsername;
      this.token = token;
      this.expiration = expiration;
      this.tokenFile = tokenFile;
    }
    
    private boolean maybeDelete(LocalDateTime now) throws IOException {
      LocalDateTime atime = getAtime(tokenFile).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
      Duration duration = Duration.between(atime.toInstant(ZoneOffset.UTC), now.toInstant(ZoneOffset.UTC));
      return duration.getSeconds() >= 60 * MINUTE;
    }
    
    private boolean maybeRenew(LocalDateTime now) {
      return now.isAfter(expiration) || now.isEqual(expiration);
    }
    
    @Override
    public int hashCode() {
      return token.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
      if (o instanceof AirflowJWT) {
        AirflowJWT other = (AirflowJWT) o;
        return token.equals(other.token);
      }
      return false;
    }
  }
}
