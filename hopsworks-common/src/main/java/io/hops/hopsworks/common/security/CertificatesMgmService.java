/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.ProjectGenericUserCerts;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.command.SystemCommand;
import io.hops.hopsworks.common.dao.command.SystemCommandFacade;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificate;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificateFacade;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class CertificatesMgmService {
  private final Logger LOG = Logger.getLogger(CertificatesMgmService.class.getName());
  
  public static final String CERTIFICATE_SUFFIX = ".cert.pem";
  
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
  @EJB
  private SystemCommandFacade systemCommandFacade;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private OpensslOperations opensslOperations;
  @EJB
  private ServiceCertificateRotationTimer serviceCertificateRotationTimer;
  
  private File masterPasswordFile;
  private final Map<Class, MasterPasswordChangeHandler> handlersMap = new ConcurrentHashMap<>();
  private final ReentrantLock opensslLock = new ReentrantLock(true);
  
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
  
  public ReentrantLock getOpensslLock() {
    return opensslLock;
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
      List<String> updatedCertificates = callUpdateHandlers(newDigest);
      updateMasterEncryptionPassword(newDigest);
      sendSuccessfulMessage(updatedCertificates, userRequested);
      LOG.log(Level.INFO, "Master encryption password changed!");
    } catch (EncryptionMasterPasswordException ex) {
      String errorMsg = "*** Master encryption password update failed!!! Rolling back...";
      LOG.log(Level.SEVERE, errorMsg, ex);
      callRollbackHandlers();
      sendUnsuccessfulMessage(errorMsg + "\n" + ex.getMessage(), userRequested);
    } catch (IOException ex) {
      String errorMsg = "*** Failed to write new encryption password to file: " + masterPasswordFile.getAbsolutePath()
          + ". Rolling back...";
      LOG.log(Level.SEVERE, errorMsg, ex);
      callRollbackHandlers();
      sendUnsuccessfulMessage(errorMsg + "\n" + ex.getMessage(), userRequested);
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void issueServiceKeyRotationCommand() {
    List<Hosts> allHosts = hostsFacade.find();
    for (Hosts host : allHosts) {
      SystemCommand rotateCommand = new SystemCommand(host, SystemCommandFacade.OP.SERVICE_KEY_ROTATION);
      systemCommandFacade.persist(rotateCommand);
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void deleteServiceCertificate(Hosts host, Integer commandId) throws IOException, CAException {
    String suffix = serviceCertificateRotationTimer.getToBeRevokedSuffix(Integer.toString(commandId));
    try {
      opensslOperations.revokeCertificate(host.getHostname(), suffix, CertificateType.HOST,
          true, true);
    } catch (IllegalArgumentException e) {
      // Do nothing
    }
  }
  
  @SuppressWarnings("unchecked")
  private List<String> callUpdateHandlers(String newDigest) throws EncryptionMasterPasswordException, IOException {
    List<String> updatedCertificates = new ArrayList<>();
    for (Map.Entry<Class, MasterPasswordChangeHandler> handler : handlersMap.entrySet()) {
      LOG.log(Level.FINE, "Calling master password UPDATE handler for <" + handler.getKey().getName() + ">");
      List<String> updatedCerts = handler.getValue().handleMasterPasswordChange(getMasterEncryptionPassword(),
          newDigest);
      updatedCertificates.addAll(updatedCerts);
    }
    
    return updatedCertificates;
  }
  
  private void callRollbackHandlers() {
    for (Map.Entry<Class, MasterPasswordChangeHandler> handler : handlersMap.entrySet()) {
      LOG.log(Level.SEVERE, "Calling master password ROLLBACK handler for <" + handler.getKey().getName() + ">");
      handler.getValue().rollback();
    }
  }
  
  private void registerMasterPasswordChangeHandler(Class clazz, MasterPasswordChangeHandler handler) {
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
