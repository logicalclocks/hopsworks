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
package io.hops.hopsworks.common.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.ProjectGenericUserCerts;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.HopsUtils;
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private CertificatesMgmService certificatesMgmService;
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
  
  public static final String CERT_PASS_SUFFIX = "__cert.key";
  
  private final Map<MaterialKey, InternalCryptoMaterial> materialMap =
      new ConcurrentHashMap<>();
  
  private final Set<Integer> projectsWithOpenInterpreters = new ConcurrentSkipListSet<>();
  private final Map<MaterialKey, FileRemover> scheduledFileRemovers =
      new ConcurrentHashMap<>();
  private String transientDir;
  private Long DELAY_VALUE;
  private TimeUnit DELAY_TIMEUNIT;
  
  public CertificateMaterializer() {
  }
  
  @PostConstruct
  public void init() {
    File tmpDir = new File(settings.getHopsworksTmpCertDir());
    if (!tmpDir.exists()) {
      throw new IllegalStateException("Transient certificates directory <" +
        tmpDir.getAbsolutePath() + "> does NOT exist!");
    }
  
    try {
      PosixFileAttributeView fileView = Files.getFileAttributeView(tmpDir
          .toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
      Set<PosixFilePermission> permissions = fileView.readAttributes()
          .permissions();
      boolean ownerRead = permissions.contains(PosixFilePermission.OWNER_READ);
      boolean ownerWrite = permissions.contains(PosixFilePermission
          .OWNER_WRITE);
      boolean ownerExecute = permissions.contains(PosixFilePermission
          .OWNER_EXECUTE);
    
      boolean groupRead = permissions.contains(PosixFilePermission.GROUP_READ);
      boolean groupWrite = permissions.contains(PosixFilePermission
          .GROUP_WRITE);
      boolean groupExecute = permissions.contains(PosixFilePermission
          .GROUP_EXECUTE);
    
      boolean othersRead = permissions.contains(PosixFilePermission
          .OTHERS_READ);
      boolean othersWrite = permissions.contains(PosixFilePermission
          .OTHERS_WRITE);
      boolean othersExecute = permissions.contains(PosixFilePermission
          .OTHERS_EXECUTE);
    
      // Permissions should be 750
      if ((ownerRead && ownerWrite && ownerExecute)
          && (groupRead && !groupWrite && groupExecute)
          && (!othersRead && !othersWrite & !othersExecute)) {
        String owner = fileView.readAttributes().owner().getName();
        String group = fileView.readAttributes().group().getName();
        String permStr = PosixFilePermissions.toString(permissions);
        LOG.log(Level.INFO, "Passed permissions check for " + tmpDir.getAbsolutePath()
            + ". Owner: " + owner + " Group: " + group + " permissions: " + permStr);
      } else {
        throw new IllegalStateException("Wrong permissions for " +
            tmpDir.getAbsolutePath() + ", it should be 0750");
      }
    } catch (UnsupportedOperationException ex) {
      LOG.log(Level.WARNING, "Associated filesystem is not POSIX compliant. " +
          "Continue without checking the permissions of " + tmpDir
          .getAbsolutePath() + " This might be a security problem.");
    } catch (IOException ex) {
      throw new IllegalStateException("Error while getting filesystem " +
          "permissions of " + tmpDir.getAbsolutePath(), ex);
    }
    
    try {
      FileUtils.cleanDirectory(tmpDir);
    } catch (IOException ex) {
      LOG.log(Level.WARNING, "Could not clean directory " + tmpDir
          .getAbsolutePath() + " during startup, there might be stale " +
          "certificates", ex);
    }
    transientDir = tmpDir.getAbsolutePath();
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
    try {
      FileUtils.cleanDirectory(new File(transientDir));
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Could not clean directory " + transientDir
        + " Administrator should clean it manually!", ex);
    }
  }
  
  public void materializeCertificates(String projectName) throws IOException {
    materializeCertificates(null, projectName);
  }

  @Lock(LockType.WRITE)
  @AccessTimeout(value=500)
  public void materializeCertificates(String username, String projectName)
    throws IOException {
    MaterialKey key = new MaterialKey(username, projectName);
    InternalCryptoMaterial material = materialMap.get(key);
    FileRemover scheduledRemover = null;
    LOG.log(Level.FINEST, "Requested materialization for: " + key
        .getExtendedUsername());
    if (material != null) {
      // Crypto material already exists
      material.incrementReference();
      LOG.log(Level.FINEST, "User: " + key.getExtendedUsername() + " " +
          "Material exist, ref " + material.references);
    } else {
      // Crypto material does not exist in cache
      LOG.log(Level.FINEST, "Material for " + key.getExtendedUsername()
          + " does not exist in cache");
      // But there might be scheduled a delayed removal from the local fs
      if ((scheduledRemover = scheduledFileRemovers.get(key)) != null) {
        LOG.log(Level.FINEST, "Exists scheduled removal for " + key
            .getExtendedUsername());
        // Cancel the delayed removal
        boolean canceled = scheduledRemover.scheduledFuture.cancel(false);
        scheduledFileRemovers.remove(key);
        // If managed to cancel properly, just put back the reference
        if (canceled) {
          LOG.log(Level.FINEST, "Successfully canceled delayed removal for " +
              key.getExtendedUsername());
          
          scheduledRemover.material.references++;
          materialMap.put(key, scheduledRemover.material);
        } else {
          // Otherwise materialize
          // Force deletion of material to avoid corrupted data
          LOG.log(Level.FINEST, "Could not cancel delayed removal, " +
              "materializing again for " + key.getExtendedUsername());
          forceRemoveCertificates(key.username, key.projectName, false);
          materialize(key);
        }
      } else {
        // Otherwise the material has been wiped-out, so materialize it anyway
        LOG.log(Level.FINEST, "Material for " + key.getExtendedUsername()
            + " has been wiped out, materializing!");
        materialize(key);
      }
    }
  }
  
  private void materialize(MaterialKey key) throws IOException {
    String decryptedPass;
    // Spark/Livy and Hive in Zeppelin runs as user PROJECTNAME__PROJECTGENERICUSER and not as
    // PROJECTNAME__USERNAME
    if (key.isProjectUser()) {
      ProjectGenericUserCerts projectGenericUserCerts = certsFacade.findProjectGenericUserCerts(key
          .getExtendedUsername());
      if (null == projectGenericUserCerts) {
        throw new IOException("Could not find certificates for user " + key
            .getExtendedUsername());
      }
      decryptedPass = decryptMaterialPassword(key.projectName,
          projectGenericUserCerts.getCertificatePassword(),
          ProjectGenericUserCerts.class);
      materializeInternal(key, projectGenericUserCerts.getKey(),
          projectGenericUserCerts.getCert(), decryptedPass);
    } else {
      UserCerts projectSpecificCerts = certsFacade.findUserCert(key.projectName,
          key.username);
      decryptedPass = decryptMaterialPassword(key
          .getExtendedUsername(), projectSpecificCerts.getUserKeyPwd(),
          UserCerts.class);
      materializeInternal(key, projectSpecificCerts.getUserKey(),
          projectSpecificCerts.getUserCert(), decryptedPass);
    }
  }
  
  private <T> String decryptMaterialPassword(String certificateIdentifier,
      String encryptedPassword, Class<T> cls) throws IOException {
    String userPassword;
    if (cls == ProjectGenericUserCerts.class) {
      // Project generic certificate
      // Certificate identifier would be the project name
      Project project = projectFacade.findByName(certificateIdentifier);
      if (project == null) {
        throw new IOException("Project with name " + certificateIdentifier +
            " could not be found");
      }
      Users owner = project.getOwner();
      userPassword = owner.getPassword();
    } else if (cls == UserCerts.class) {
      // Project specific certificate
      // Certificate identifier would be the project specific username
      String username = hdfsUsersController.getUserName(certificateIdentifier);
      Users user = userFacade.findByUsername(username);
      if (user == null) {
        throw new IOException("Could not find user: " + username);
      }
      userPassword = user.getPassword();
    } else {
      throw new IllegalArgumentException("Certificate type " + cls.getName()
          + " is unknown");
    }
    
    try {
      return HopsUtils.decrypt(userPassword, encryptedPassword, certificatesMgmService.getMasterEncryptionPassword());
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
  
  private void materializeInternal(MaterialKey key, byte[] keyStore, byte[]
      trustStore, String password) throws IOException {
    if (null != keyStore && null != trustStore) {
      flushToLocalFs(key.getExtendedUsername(), keyStore, trustStore,
          password);
      materialMap.put(key, new InternalCryptoMaterial(keyStore, trustStore,
          password));
      LOG.log(Level.FINEST, "User: " + key.getExtendedUsername() + " " +
          "Material DOES NOT exist, flushing now!!!");
    } else {
      throw new IOException("Certificates for user " + key
          .getExtendedUsername() + " could not be found in the " +
          "database");
    }
  }
  
  public CryptoMaterial getUserMaterial(String projectName)
    throws CryptoPasswordNotFoundException {
    return getUserMaterial(null, projectName);
  }
  
  @Lock(LockType.READ)
  public CryptoMaterial getUserMaterial(String username, String projectName)
    throws CryptoPasswordNotFoundException {
    MaterialKey key = new MaterialKey(username, projectName);
    InternalCryptoMaterial material = materialMap.get(key);
    if (null != material) {
      return new CryptoMaterial(material.getKeyStore(), material
          .getTrustStore(), material.getPassword());
    }
    
    throw new CryptoPasswordNotFoundException(
        "Cryptographic material for user " + key.getExtendedUsername() +
            " does not exist!");
  }
  
  public void removeCertificate(String projectName) {
    removeCertificate(null, projectName);
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value=100)
  public void removeCertificate(String username, String projectName) {
    MaterialKey key = new MaterialKey(username, projectName);
    InternalCryptoMaterial material = materialMap.get(key);
    if (null != material) {
      LOG.log(Level.FINEST, "Requested removal of material for " + key
          .getExtendedUsername() + " Ref: " + material.references);
    }
    if (null != material && material.decrementReference()) {
      materialMap.remove(key);
      scheduleFileRemover(key, new FileRemover(key, material));
    }
  }
  
  @Lock(LockType.WRITE)
  @AccessTimeout(value=100)
  public void forceRemoveCertificates(String username, String projectName,
      boolean bothUserAndProject) {
    // Remove project specific certificates, if any
    MaterialKey key = new MaterialKey(username, projectName);
    FileRemover scheduledRemover = scheduledFileRemovers.remove(key);
    if (null != scheduledRemover) {
      scheduledRemover.scheduledFuture.cancel(true);
    }
    deleteMaterialFromLocalFs(key.getExtendedUsername());
    materialMap.remove(key);
    
    if (bothUserAndProject) {
      // Remove project generic certificates, if any
      key = new MaterialKey(null, projectName);
      scheduledRemover = scheduledFileRemovers.remove(key);
      if (null != scheduledRemover) {
        scheduledRemover.scheduledFuture.cancel(true);
      }
      deleteMaterialFromLocalFs(key.getExtendedUsername());
      materialMap.remove(key);
    }
  }
  
  @Lock(LockType.READ)
  @AccessTimeout(value=200)
  public boolean existsInStore(String username, String projectName) {
    MaterialKey key = new MaterialKey(username, projectName);
    return materialMap.containsKey(key);
  }
  
  private void scheduleFileRemover(MaterialKey key, FileRemover fileRemover) {
    fileRemover.scheduledFuture = scheduler.schedule(fileRemover, DELAY_VALUE,
        DELAY_TIMEUNIT);
    
    scheduledFileRemovers.put(key, fileRemover);
    LOG.log(Level.FINEST, "Scheduled removal of material for " + key.getExtendedUsername());
  }
  
  /**
   * It is called every time a paragraph is executed in Zeppelin. If the certificates for a Project has already been
   * materialized, this method will return false and they will not be materialized again.
   * @param projectId
   * @return True if it is the first time a paragraph is executed for that project. Otherwise false
   */
  public boolean openedInterpreter(Integer projectId) {
    return projectsWithOpenInterpreters.add(projectId);
  }
  
  /**
   * It is called only when a project has not running interpreters, thus it is safe to remove the certificates.
   * @param projectId ID of the project
   */
  public void closedInterpreter(Integer projectId) {
    projectsWithOpenInterpreters.remove(projectId);
  }
  
  private void deleteMaterialFromLocalFs(String username) {
    File kstoreFile = Paths.get(transientDir, username
        + "__kstore.jks").toFile();
    File tstoreFile = Paths.get(transientDir, username
        + "__tstore.jks").toFile();
    File certPassFile = Paths.get(transientDir, username
        + CERT_PASS_SUFFIX).toFile();
    FileUtils.deleteQuietly(kstoreFile);
    FileUtils.deleteQuietly(tstoreFile);
    FileUtils.deleteQuietly(certPassFile);
  }
  
  private void flushToLocalFs(String username, byte[] kstore, byte[] tstore,
      String password) throws IOException {
    File kstoreFile = Paths.get(transientDir, username + "__kstore.jks")
        .toFile();
    File tstoreFile = Paths.get(transientDir, username + "__tstore.jks")
        .toFile();
    File certPassFile = Paths.get(transientDir, username + CERT_PASS_SUFFIX)
        .toFile();
  
    FileUtils.writeByteArrayToFile(kstoreFile, kstore, false);
    FileUtils.writeByteArrayToFile(tstoreFile, tstore, false);
    FileUtils.writeStringToFile(certPassFile, password, false);
  }
  
  private class FileRemover implements Runnable {
    private final MaterialKey key;
    private final InternalCryptoMaterial material;
    private ScheduledFuture scheduledFuture;
    
    private FileRemover(MaterialKey key, InternalCryptoMaterial material) {
      this.key = key;
      this.material = material;
    }
    
    @Override
    public void run() {
      deleteMaterialFromLocalFs(key.getExtendedUsername());
      scheduledFileRemovers.remove(key);
      LOG.log(Level.FINEST, "Wiped out material for " + key.getExtendedUsername());
    }
  }

  private class MaterialKey {
    private final String username;
    private final String projectName;
    private final boolean isProjectUser;
    
    private MaterialKey(String username, String projectName) {
      this.username = username;

      if (username == null) {
        // Project Generic User certificate
        this.isProjectUser = true;
        this.projectName = projectName;
      } else {
        // Project Specific User certificate
        this.isProjectUser = false;
        this.projectName = projectName;
      }
    }
    
    private boolean isProjectUser() {
      return isProjectUser;
    }
    
    private String getExtendedUsername() {
      if (isProjectUser) {
        return projectName + Settings.PROJECT_GENERIC_USER_SUFFIX;
      }

      return projectName + HdfsUsersController.USER_NAME_DELIMITER + username;
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
  
  public class CryptoMaterial {
    private final byte[] keyStore;
    private final byte[] trustStore;
    private final String password;
    
    public CryptoMaterial(byte[] keyStore, byte[] trustStore, String password) {
      this.keyStore = keyStore;
      this.trustStore = trustStore;
      this.password = password;
    }
    
    public byte[] getKeyStore() {
      return keyStore;
    }
    
    public byte[] getTrustStore() {
      return trustStore;
    }
    
    public String getPassword() {
      return password;
    }
  }
  
  private class InternalCryptoMaterial extends CryptoMaterial {
    private int references;
    
    private InternalCryptoMaterial(byte[] keystore, byte[] trustStore,
        String password) {
      super(keystore, trustStore, password);
      references = 1;
    }
    
    private boolean decrementReference() {
      return --references == 0;
    }
    
    private void incrementReference() {
      references++;
    }
  }
  
  /**
   * This section provides methods for monitoring and control.
   */
  
  /**
   * Return an immutable state of the CertificateMaterializer service. Both the materialized and those scheduled for
   * removal
   * @return Immutable state of CertificateMaterializer service
   */
  @SuppressWarnings("unchecked")
  public MaterializerState<Map<String, Integer>, Set<String>> getState() {
    MaterializerState<ImmutableMap<MaterialKey, InternalCryptoMaterial>, ImmutableSet<MaterialKey>>
        materializerState = getImmutableState();
    
    ImmutableMap<MaterialKey, InternalCryptoMaterial> materializedState = materializerState.getMaterializedState();
    Map<String, Integer> occurencies = new HashMap<>(materializedState.size());
    ImmutableSet<Map.Entry<MaterialKey, InternalCryptoMaterial>> entrySet = materializedState.entrySet();
    entrySet.stream()
        .forEach(es ->
            occurencies.put(es.getKey().getExtendedUsername(), es.getValue().references));
    
    ImmutableSet<MaterialKey> scheduledRemovals = materializerState.getScheduledRemovals();
    Set<String> removals = scheduledRemovals.stream()
        .map(mt -> mt.getExtendedUsername())
        .collect(Collectors.toSet());
    
    return new MaterializerState<>(occurencies, removals);
  }
  
  @Lock(LockType.WRITE)
  private MaterializerState getImmutableState() {
    ImmutableMap<MaterialKey, InternalCryptoMaterial> materializedState = ImmutableMap.copyOf(materialMap);
    ImmutableSet<MaterialKey> scheduledRemovals = ImmutableSet.copyOf(scheduledFileRemovers.keySet());
    return new MaterializerState<ImmutableMap<MaterialKey, InternalCryptoMaterial>, ImmutableSet<MaterialKey>>(
        materializedState, scheduledRemovals);
  }
  
  public class MaterializerState<T, S> {
    private final T materializedState;
    private final S scheduledRemovals;
    
    MaterializerState(T materializedState, S scheduledRemovals) {
      this.materializedState = materializedState;
      this.scheduledRemovals = scheduledRemovals;
    }
    
    public T getMaterializedState() {
      return materializedState;
    }
    
    public S getScheduledRemovals() {
      return scheduledRemovals;
    }
  }
}
