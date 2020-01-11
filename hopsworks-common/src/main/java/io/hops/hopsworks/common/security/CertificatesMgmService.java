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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.command.SystemCommand;
import io.hops.hopsworks.common.dao.command.SystemCommandFacade;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificateFacade;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.message.MessageController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;
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
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
  private CertsFacade certsFacade;
  @EJB
  private ClusterCertificateFacade clusterCertificateFacade;
  @EJB
  private MessageController messageController;
  @EJB
  private SystemCommandFacade systemCommandFacade;
  @EJB
  private HostsFacade hostsFacade;
  @Inject
  @Any
  private Instance<MasterPasswordHandler> handlers;
  
  public enum UPDATE_STATUS {
    OK,
    WORKING,
    FAILED,
    NOT_FOUND
  }
  
  private File masterPasswordFile;
  private final Map<Class, MasterPasswordChangeResult> handlersResult = new HashMap<>();
  private Cache<Integer, UPDATE_STATUS> updateStatus;
  private Random rand;

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
      
      updateStatus = CacheBuilder.newBuilder()
          .maximumSize(100)
          .expireAfterWrite(12L, TimeUnit.HOURS)
          .build();
      rand = new Random();
    } catch (UnsupportedOperationException ex) {
      LOG.log(Level.WARNING, "Associated filesystem is not POSIX compliant. " +
          "Continue without checking the permissions of " + masterPasswordFile.getAbsolutePath()
          + " This might be a security problem.");
    } catch (IOException ex) {
      throw new IllegalStateException("Error while getting POSIX permissions of " + masterPasswordFile
          .getAbsolutePath());
    }
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
  
  public Integer initUpdateOperation() {
    Integer operationId = rand.nextInt();
    updateStatus.put(operationId, UPDATE_STATUS.WORKING);
    return operationId;
  }
  
  public UPDATE_STATUS getOperationStatus(Integer operationId) {
    UPDATE_STATUS status = updateStatus.getIfPresent(operationId);
    return status != null ? status : UPDATE_STATUS.NOT_FOUND;
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
  public void resetMasterEncryptionPassword(Integer operationId, String newMasterPasswd, String userRequested) {
    try {
      String newDigest = DigestUtils.sha256Hex(newMasterPasswd);
      callUpdateHandlers(newDigest);
      updateMasterEncryptionPassword(newDigest);
      StringBuilder successLog = gatherLogs();
      sendSuccessfulMessage(successLog, userRequested);
      updateStatus.put(operationId, UPDATE_STATUS.OK);
      LOG.log(Level.INFO, "Master encryption password changed!");
    } catch (EncryptionMasterPasswordException ex) {
      String errorMsg = "*** Master encryption password update failed!!! Rolling back...";
      LOG.log(Level.SEVERE, errorMsg, ex);
      updateStatus.put(operationId, UPDATE_STATUS.FAILED);
      callRollbackHandlers();
      sendUnsuccessfulMessage(errorMsg + "\n" + ex.getMessage(), userRequested);
    } catch (IOException ex) {
      String errorMsg = "*** Failed to write new encryption password to file: " + masterPasswordFile.getAbsolutePath()
          + ". Rolling back...";
      LOG.log(Level.SEVERE, errorMsg, ex);
      updateStatus.put(operationId, UPDATE_STATUS.FAILED);
      callRollbackHandlers();
      sendUnsuccessfulMessage(errorMsg + "\n" + ex.getMessage(), userRequested);
    } finally {
      handlersResult.clear();
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void issueServiceKeyRotationCommand() {
    List<Hosts> allHosts = hostsFacade.findAll();
    for (Hosts host : allHosts) {
      SystemCommand rotateCommand = new SystemCommand(host, SystemCommandFacade.OP.SERVICE_KEY_ROTATION);
      systemCommandFacade.persist(rotateCommand);
    }
  }
  
  private void callUpdateHandlers(String newDigest) throws EncryptionMasterPasswordException, IOException {
    for (MasterPasswordHandler handler : handlers) {
      MasterPasswordChangeResult result = handler.perform(getMasterEncryptionPassword(), newDigest);
      handlersResult.put(handler.getClass(), result);
      if (result.getCause() != null) {
        throw result.getCause();
      }
    }
  }
  
  private void callRollbackHandlers() {
    for (MasterPasswordHandler handler : handlers) {
      MasterPasswordChangeResult result = handlersResult.get(handler.getClass());
      if (result != null) {
        handler.rollback(result);
      }
    }
  }
  
  private StringBuilder gatherLogs() {
    StringBuilder successLog = new StringBuilder();
    for (MasterPasswordChangeResult result : handlersResult.values()) {
      if (result.getSuccessLog() != null) {
        successLog.append(result.getSuccessLog());
        successLog.append("\n\n");
      }
    }
    return successLog;
  }
  
  private void updateMasterEncryptionPassword(String newPassword) throws IOException {
    FileUtils.writeStringToFile(masterPasswordFile, newPassword);
  }
  
  private void sendSuccessfulMessage(StringBuilder successLog, String userRequested) {
    sendInbox(successLog.toString(), "Changed successfully", userRequested);
  }
  
  private void sendUnsuccessfulMessage(String message, String userRequested) {
    sendInbox(message, "Change failed!", userRequested);
  }
  
  private void sendInbox(String message, String preview, String userRequested) {
    Users to = userFacade.findByEmail(userRequested);
    Users from = userFacade.findByEmail(settings.getAdminEmail());
    messageController.send(to, from, "Master encryption password changed", preview, message, "");
  }
}
