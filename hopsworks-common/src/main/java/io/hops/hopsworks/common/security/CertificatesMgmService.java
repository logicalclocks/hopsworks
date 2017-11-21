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

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.ProjectGenericUserCerts;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificate;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificateFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.EncryptionMasterPasswordException;
import io.hops.hopsworks.common.message.MessageController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class CertificatesMgmService {
  private final Logger LOG = Logger.getLogger(CertificatesMgmService.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private CertsFacade certsFacade;
  @EJB
  private ClusterCertificateFacade clusterCertificateFacade;
  @EJB
  private MessageController messageController;
  
  private File masterPasswordFile;
  private final Map<Class, MasterPasswordChangeHandler> handlersMap = new ConcurrentHashMap<>();
  
  public CertificatesMgmService() {
  
  }
  
  @PostConstruct
  public void init() {
    masterPasswordFile = new File(settings.getHopsworksMasterEncPasswordFile());
    if (!masterPasswordFile.exists()) {
      throw new IllegalStateException("Master encryption file does not exist");
    }
    
    try {
      PosixFileAttributeView fileView = Files.getFileAttributeView(masterPasswordFile.toPath(),
          PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
      Set<PosixFilePermission> filePermissions = fileView.readAttributes().permissions();
      boolean ownerRead = filePermissions.contains(PosixFilePermission.OWNER_READ);
      boolean ownerWrite = filePermissions.contains(PosixFilePermission
          .OWNER_WRITE);
      boolean ownerExecute = filePermissions.contains(PosixFilePermission
          .OWNER_EXECUTE);
      
      boolean groupRead = filePermissions.contains(PosixFilePermission.GROUP_READ);
      boolean groupWrite = filePermissions.contains(PosixFilePermission
          .GROUP_WRITE);
      boolean groupExecute = filePermissions.contains(PosixFilePermission
          .GROUP_EXECUTE);
      
      boolean othersRead = filePermissions.contains(PosixFilePermission
          .OTHERS_READ);
      boolean othersWrite = filePermissions.contains(PosixFilePermission
          .OTHERS_WRITE);
      boolean othersExecute = filePermissions.contains(PosixFilePermission
          .OTHERS_EXECUTE);
      
      // Permissions should be 700
      if ((ownerRead && ownerWrite && ownerExecute)
          && (!groupRead && !groupWrite && !groupExecute)
          && (!othersRead && !othersWrite && !othersExecute)) {
        String owner = fileView.readAttributes().owner().getName();
        String group = fileView.readAttributes().group().getName();
        String permStr = PosixFilePermissions.toString(filePermissions);
        LOG.log(Level.INFO, "Passed permissions check for file " + masterPasswordFile.getAbsolutePath()
            + ". Owner: " + owner + " Group: " + group + " Permissions: " + permStr);
      } else {
        throw new IllegalStateException("Wrong permissions for file " + masterPasswordFile.getAbsolutePath()
            + ", it should be 700");
      }
      
    } catch (UnsupportedOperationException ex) {
      LOG.log(Level.WARNING, "Associated filesystem is not POSIX compliant. " +
          "Continue without checking the permissions of " + masterPasswordFile.getAbsolutePath()
          + " This might be a security problem.");
    } catch (IOException ex) {
      throw new IllegalStateException("Error while getting POSIX permissions of " + masterPasswordFile
          .getAbsolutePath());
    }
  
    // Register handlers when master encryption password changes
    MasterPasswordChangeHandler<CertsFacade> psUserCertsHandler = new PSUserCertsMasterPasswordHandler(userFacade);
    psUserCertsHandler.setFacade(certsFacade);
    registerMasterPasswordChangeHandler(UserCerts.class, psUserCertsHandler);
    
    MasterPasswordChangeHandler<CertsFacade> pgUserCertsHandler = new PGUserCertsMasterPasswordHandler(projectFacade);
    pgUserCertsHandler.setFacade(certsFacade);
    registerMasterPasswordChangeHandler(ProjectGenericUserCerts.class, pgUserCertsHandler);
    
    MasterPasswordChangeHandler<ClusterCertificateFacade> delaClusterCertsHandler =
        new DelaCertsMasterPasswordHandler(settings);
    delaClusterCertsHandler.setFacade(clusterCertificateFacade);
    registerMasterPasswordChangeHandler(ClusterCertificate.class, delaClusterCertsHandler);
  }
  
  @Lock(LockType.READ)
  @AccessTimeout(value = 3, unit = TimeUnit.SECONDS)
  public String getMasterEncryptionPassword() throws IOException {
    return FileUtils.readFileToString(masterPasswordFile).trim();
  }
  
  /**
   * Validates the provided password against the configured one
   * @param providedPassword Password to validate
   * @param userRequestedEmail User requested the password check
   * @throws IOException
   * @throws EncryptionMasterPasswordException
   */
  @Lock(LockType.READ)
  @AccessTimeout(value = 3, unit = TimeUnit.SECONDS)
  public void checkPassword(String providedPassword, String userRequestedEmail)
      throws IOException, EncryptionMasterPasswordException {
    String sha = DigestUtils.sha256Hex(providedPassword);
    if (!getMasterEncryptionPassword().equals(sha)) {
      Users user = userFacade.findByEmail(userRequestedEmail);
      String logMsg = "*** Attempt to change master encryption password with wrong credentials";
      if (user != null) {
        LOG.log(Level.INFO, logMsg + " by user <" + user.getUsername() + ">");
      } else {
        LOG.log(Level.INFO, logMsg);
      }
      throw new EncryptionMasterPasswordException("Provided password is incorrect");
    }
  }
  
  /**
   * Decrypt secrets using the old master password and encrypt them with the new
   * Both for project specific and project generic certificates
   * @param newMasterPasswd new master encryption password
   * @param userRequested User requested password change
   */
  @SuppressWarnings("unchecked")
  @Asynchronous
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 500)
  public void resetMasterEncryptionPassword(String newMasterPasswd, String userRequested) {
    try {
      String newDigest = DigestUtils.sha256Hex(newMasterPasswd);
      List<String> updatedCertificates = new ArrayList<>();
      for (Map.Entry<Class, MasterPasswordChangeHandler> handler : handlersMap.entrySet()) {
        LOG.log(Level.FINE, "Calling master password change handler for <" + handler.getKey().getName() + ">");
        List<String> updatedCerts = handler.getValue().handleMasterPasswordChange(getMasterEncryptionPassword(),
            newDigest);
        updatedCertificates.addAll(updatedCerts);
      }
      updateMasterEncryptionPassword(newDigest);
      sendSuccessfulMessage(updatedCertificates, userRequested);
      LOG.log(Level.INFO, "Master encryption password changed!");
    } catch (EncryptionMasterPasswordException ex) {
      String errorMsg = "*** Master encryption password update failed!!!";
      LOG.log(Level.SEVERE, errorMsg, ex);
      sendUnsuccessfulMessage(errorMsg + "\n" + ex.getMessage(), userRequested);
    } catch (IOException ex) {
      String errorMsg = "*** Failed to write new encryption password to file: " + newMasterPasswd;
      LOG.log(Level.SEVERE, errorMsg, ex);
      sendUnsuccessfulMessage(errorMsg + "\n" + ex.getMessage(), userRequested);
    }
  }
  
  public void registerMasterPasswordChangeHandler(Class clazz, MasterPasswordChangeHandler handler) {
    handlersMap.putIfAbsent(clazz, handler);
  }
  
  private void updateMasterEncryptionPassword(String newPassword) throws IOException {
    FileUtils.writeStringToFile(masterPasswordFile, newPassword);
  }
  
  private void sendSuccessfulMessage(List<String> updatedCerts, String userRequested) {
    StringBuilder sb = new StringBuilder();
    sb.append("CertificateMgmService successfully updated the following certificates password:").append("\n");
    updatedCerts.stream()
        .forEach(c -> sb.append("* ").append(c).append("\n"));
    sendInbox(sb.toString(), "Changed successfully", userRequested);
  }
  
  private void sendUnsuccessfulMessage(String message, String userRequested) {
    sendInbox(message, "Change failed!", userRequested);
  }
  
  private void sendInbox(String message, String preview, String userRequested) {
    Users to = userFacade.findByEmail(userRequested);
    Users from = userFacade.findByEmail(Settings.SITE_EMAIL);
    messageController.send(to, from, "Master encryption password changed", preview, message, "");
  }
  
  /**
   * Interface for handling master encryption password change. A handler should implement this interface
   * and register itself with the CertificateMgmService (see init() method)
   * @param <T> Facade class used to retrieve and store certificates information from the database
   */
  public interface MasterPasswordChangeHandler<T> {
    /**
     * Set the facade for the certificates
     * @param certsFacade
     */
    void setFacade(T certsFacade);
    /**
     * Action performed when the master encryption password has changed
     */
    List<String> handleMasterPasswordChange(String oldMasterPassword, String newMasterPassword)
        throws EncryptionMasterPasswordException;
  
    /**
     * Action performed when an exception has been thrown during the master password change
     */
    void rollback();
    
    default String getNewUserPassword(String userPassword, String cipherText, String oldMasterPassword,
        String newMasterPassword) throws Exception {
      String plainCertPassword = HopsUtils.decrypt(userPassword, cipherText, oldMasterPassword);
      return HopsUtils.encrypt(userPassword, plainCertPassword, newMasterPassword);
    }
  }
}
