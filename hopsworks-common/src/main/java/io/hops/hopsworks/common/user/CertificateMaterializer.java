/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.common.user;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.ServiceCerts;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.io.FileUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@DependsOn("Settings")
public class CertificateMaterializer {
  
  private final Logger LOG = Logger.getLogger(CertificateMaterializer
      .class.getName());
  
  @EJB
  private CertsFacade certsFacade;
  @EJB
  private Settings settings;
  @Resource
  private ManagedScheduledExecutorService scheduler;
  
  private static final Map<String, TimeUnit> TIME_SUFFIXES;
  static {
    TIME_SUFFIXES = new HashMap<>(5);
    TIME_SUFFIXES.put("ms", TimeUnit.MILLISECONDS);
    TIME_SUFFIXES.put("s", TimeUnit.SECONDS);
    TIME_SUFFIXES.put("m", TimeUnit.MINUTES);
    TIME_SUFFIXES.put("h", TimeUnit.HOURS);
    TIME_SUFFIXES.put("d", TimeUnit.DAYS);
  }
  
  private final Map<MaterialKey, CryptoMaterial> materialMap =
      new ConcurrentHashMap<>();
  private final Map<Integer, Set<String>> openInterpreterGroupsPerProject =
      new ConcurrentHashMap<>();
  private final Map<MaterialKey, FileRemover> scheduledFileRemovers =
      new HashMap<>();
  private String transientDir;
  private Long DELAY_VALUE;
  private TimeUnit DELAY_TIMEUNIT;
  
  public CertificateMaterializer() {
  }
  
  @PostConstruct
  public void init() {
    File tmpDir = new File(settings.getHopsworksTmpCertDir());
    if (tmpDir.exists()) {
      FileUtils.deleteQuietly(tmpDir);
    }
    if (!tmpDir.exists()) {
      tmpDir.setExecutable(false);
      tmpDir.setReadable(true, true);
      tmpDir.setWritable(true, true);
      tmpDir.mkdir();
    }
    
    transientDir = settings.getHopsworksTmpCertDir();
    
    String delayRaw = settings.getCertificateMaterializerDelay();
  
    Matcher matcher = Pattern.compile("([0-9]+)([a-z]+)?").matcher(delayRaw
        .toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid delay value: " + delayRaw);
    }
    
    DELAY_VALUE = Long.parseLong(matcher.group(1));
    String timeUnitStr = matcher.group(2);
    if (null != timeUnitStr && !TIME_SUFFIXES.containsKey(timeUnitStr)) {
      throw new IllegalArgumentException("Invalid delay suffix: " + timeUnitStr);
    }
    DELAY_TIMEUNIT = timeUnitStr == null ? TimeUnit.MINUTES : TIME_SUFFIXES
        .get(timeUnitStr);
  }
  
  @PreDestroy
  public void tearDown() {
    FileUtils.deleteQuietly(new File(transientDir));
  }
  
  public void materializeCertificates(String projectName) throws IOException {
    materializeCertificates(null, projectName);
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value=500)
  public void materializeCertificates(String username, String projectName)
    throws IOException {
    MaterialKey key = new MaterialKey(username, projectName);
    CryptoMaterial material = materialMap.get(key);
    FileRemover scheduledRemover = null;
    LOG.log(Level.FINEST, "Requested materialization for: " + key
        .getProjectSpecificUsername());
    if (material != null) {
      // Crypto material already exists
      material.incrementReference();
      LOG.log(Level.FINEST, "User: " + key.getProjectSpecificUsername() + " " +
          "Material exist, ref " + material.references);
    } else {
      // Crypto material does not exist in cache
      LOG.log(Level.FINEST, "Material for " + key.getProjectSpecificUsername()
          + " does not exist in cache");
      // But there might be scheduled a delayed removal from the local fs
      if ((scheduledRemover = scheduledFileRemovers.get(key)) != null) {
        LOG.log(Level.FINEST, "Exists scheduled removal for " + key
            .getProjectSpecificUsername());
        // Cancel the delayed removal
        boolean canceled = scheduledRemover.scheduledFuture.cancel(false);
        scheduledFileRemovers.remove(key);
        // If managed to cancel properly, just put back the reference
        if (canceled) {
          LOG.log(Level.FINEST, "Successfully canceled delayed removal for " +
              key.getProjectSpecificUsername());
          
          scheduledRemover.material.references++;
          materialMap.put(key, scheduledRemover.material);
        } else {
          // Otherwise materialize
          // Force deletion of material to avoid corrupted data
          LOG.log(Level.FINEST, "Could not cancel delayed removal, " +
              "materializing again for " + key.getProjectSpecificUsername());
          forceRemoveCertificates(key.username, key.projectName);
          materialize(key);
        }
      } else {
        // Otherwise the material has been wiped-out, so materialize it anyway
        LOG.log(Level.FINEST, "Material for " + key.getProjectSpecificUsername()
            + " has been wiped out, materializing!");
        materialize(key);
      }
    }
  }
  
  private void materialize(MaterialKey key) throws IOException {
    // Spark interpreter in Zeppelin runs as user PROJECTNAME and not as
    // PROJECTNAME__USERNAME
    if (key.isSparkInterpreter()) {
      List<ServiceCerts> serviceCerts = certsFacade.findServiceCertsByName(key
          .getProjectSpecificUsername());
      if (null == serviceCerts || serviceCerts.isEmpty()) {
        throw new IOException("Could not find certificates for user " + key
            .getProjectSpecificUsername());
      }
      ServiceCerts storedMaterial = serviceCerts.get(0);
      materializeInternal(key, storedMaterial.getServiceKey(),
          storedMaterial.getServiceCert());
    } else {
      UserCerts projectSpecificCerts = certsFacade.findUserCert(key.projectName,
          key.username);
    
      materializeInternal(key, projectSpecificCerts.getUserKey(),
          projectSpecificCerts.getUserCert());
    }
  }
  
  private void materializeInternal(MaterialKey key, byte[] keyStore, byte[]
      trustStore) throws IOException {
    if (null != keyStore && null != trustStore) {
      flushToLocalFs(key.getProjectSpecificUsername(), keyStore, trustStore);
      materialMap.put(key, new CryptoMaterial(keyStore, trustStore));
      LOG.log(Level.FINEST, "User: " + key.getProjectSpecificUsername() + " " +
          "Material DOES NOT exist, flushing now!!!");
    } else {
      throw new IOException("Certificates for user " + key
          .getProjectSpecificUsername() + " could not be found in the " +
          "database");
    }
  }
  
  public byte[][] getUserMaterial(String projectName) {
    return getUserMaterial(null, projectName);
  }
  
  @Lock(LockType.READ)
  public byte[][] getUserMaterial(String username, String projectName) {
    MaterialKey key = new MaterialKey(username, projectName);
    CryptoMaterial material = materialMap.get(key);
    if (null != material) {
      return new byte[][]{material.getKeyStore(), material.getTrustStore()};
    }
    return null;
  }
  
  public void removeCertificate(String projectName) {
    removeCertificate(null, projectName);
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value=100)
  public void removeCertificate(String username, String projectName) {
    MaterialKey key = new MaterialKey(username, projectName);
    CryptoMaterial material = materialMap.get(key);
    if (null != material) {
      LOG.log(Level.FINEST, "Requested removal of material for " + key
          .getProjectSpecificUsername() + " Ref: " + material.references);
    }
    if (null != material && material.decrementReference()) {
      materialMap.remove(key);
      scheduleFileRemover(key, new FileRemover(key, material));
    }
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value=100)
  public void forceRemoveCertificates(String username, String projectName) {
    // Remove project specific certificates, if any
    MaterialKey key = new MaterialKey(username, projectName);
    FileRemover scheduledRemover = scheduledFileRemovers.remove(key);
    if (null != scheduledRemover) {
      scheduledRemover.scheduledFuture.cancel(true);
    }
    deleteMaterialFromLocalFs(key.getProjectSpecificUsername());
    materialMap.remove(key);
    
    // Remove project generic certificates, if any
    key = new MaterialKey(null, projectName);
    scheduledRemover = scheduledFileRemovers.remove(key);
    if (null != scheduledRemover) {
      scheduledRemover.scheduledFuture.cancel(true);
    }
    deleteMaterialFromLocalFs(key.getProjectSpecificUsername());
    materialMap.remove(key);
  }
  
  private void scheduleFileRemover(MaterialKey key, FileRemover fileRemover) {
    fileRemover.scheduledFuture = scheduler.schedule(fileRemover, DELAY_VALUE,
        DELAY_TIMEUNIT);
    
    scheduledFileRemovers.put(key, fileRemover);
    LOG.log(Level.FINEST, "Scheduled removal of material for " + key.getProjectSpecificUsername());
  }
  
  /**
   * This method is used by the HDFSNotebookRepo via a JNDI lookup
   * It keeps track of the opened interpreter groups to safely remove crypto
   * material when not needed any longer.
   * Spark group: spark, sparksql, pyspark, etc
   * Livy group: livy.spark, livy.sparksql, etc
   * @param projectId
   * @param interpreterGrp
   * @return True when there is no opened interpreter group and the material
   * should be materialized. False when the project has already opened
   * interpreter(s) and it should not materialize the certificates
   */
  public boolean openedInterpreter(Integer projectId, String interpreterGrp) {
    Set<String> openedGrps = openInterpreterGroupsPerProject.get(projectId);
    
    if (openedGrps == null) {
      // Most probably we will run either Spark or Livy interpreter group
      openedGrps = new HashSet<>(2);
      openedGrps.add(interpreterGrp);
      openInterpreterGroupsPerProject.put(projectId, openedGrps);
      return true;
    }
    
    openedGrps.add(interpreterGrp);
    return false;
  }
  
  /**
   * This method is used by the HDFSNotebookRepo via a JNDI lookup
   * It keeps track of the opened interpreter groups to safely remove crypto
   * material when not needed any longer.
   * Spark group: spark, sparksql, pyspark, etc
   * Livy group: livy.spark, livy.sparksql, etc
   * @param projectId
   * @param interpreterGrp
   * @return True when all the interpreter groups for this project are closed
   * and it is safe to remove the material. False when there still open
   * interpreters for either this interpreter group or other.
   */
  public boolean closedInterpreter(Integer projectId, String interpreterGrp) {
    Set<String> openedGrps = openInterpreterGroupsPerProject.get(projectId);
    if (openedGrps == null) {
      return true;
    }
    openedGrps.remove(interpreterGrp);
    if (openedGrps.isEmpty()) {
      openInterpreterGroupsPerProject.remove(projectId);
      return true;
    }
    
    return false;
  }
  
  private void deleteMaterialFromLocalFs(String username) {
    File kstoreFile = Paths.get(transientDir, username
        + "__kstore.jks").toFile();
    File tstoreFile = Paths.get(transientDir, username
        + "__tstore.jks").toFile();
    FileUtils.deleteQuietly(kstoreFile);
    FileUtils.deleteQuietly(tstoreFile);
  }
  
  private void flushToLocalFs(String username, byte[] kstore, byte[] tstore)
      throws IOException {
    File kstoreFile = Paths.get(transientDir, username + "__kstore.jks").toFile();
    File tstoreFile = Paths.get(transientDir, username + "__tstore.jks").toFile();
  
    FileUtils.writeByteArrayToFile(kstoreFile, kstore, false);
    FileUtils.writeByteArrayToFile(tstoreFile, tstore, false);
  }
  
  private class FileRemover implements Runnable {
    private final MaterialKey key;
    private final CryptoMaterial material;
    private ScheduledFuture scheduledFuture;
    
    private FileRemover(MaterialKey key, CryptoMaterial material) {
      this.key = key;
      this.material = material;
    }
    
    @Override
    public void run() {
      deleteMaterialFromLocalFs(key.getProjectSpecificUsername());
      scheduledFileRemovers.remove(key);
      LOG.log(Level.FINEST, "Wiped out material for " + key.getProjectSpecificUsername());
    }
  }
  
  private class MaterialKey {
    private final String username;
    private final String projectName;
    private final boolean isSparkInterpreter;
    
    private MaterialKey(String username, String projectName) {
      this.username = username;
      this.projectName = projectName;
      if (null != username) {
        this.isSparkInterpreter = false;
      } else {
        this.isSparkInterpreter = true;
      }
    }
    
    private boolean isSparkInterpreter() {
      return isSparkInterpreter;
    }
    
    private String getProjectSpecificUsername() {
      if (null != username) {
        return projectName + HdfsUsersController.USER_NAME_DELIMITER + username;
      }
      return projectName;
    }
    
    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      
      if (other instanceof MaterialKey) {
        if (null != this.username && null != ((MaterialKey) other).username) {
          return this.username.equals(((MaterialKey) other).username)
              && this.projectName.equals(((MaterialKey) other).projectName);
        }
        return this.projectName.equals(((MaterialKey) other).projectName);
      }
      
      return false;
    }
    
    @Override
    public int hashCode() {
      int result = 17;
      if (null != username) {
        result = 31 * result + username.hashCode();
      }
      result = 31 * result + projectName.hashCode();
      return result;
    }
  }
  
  private class CryptoMaterial {
    private int references;
    private final byte[] keyStore;
    private final byte[] trustStore;
    
    private CryptoMaterial(byte[] keystore, byte[] trustStore) {
      this.keyStore = keystore;
      this.trustStore = trustStore;
      references = 1;
    }
    
    private byte[] getKeyStore() {
      return keyStore;
    }
    
    private byte[] getTrustStore() {
      return trustStore;
    }
    
    private boolean decrementReference() {
      return --references == 0;
    }
    
    private void incrementReference() {
      references++;
    }
  }
}
