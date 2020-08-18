/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.security.password;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;
import io.hops.hopsworks.persistence.entity.message.Message;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.security.dao.MessageFacade;
import io.hops.hopsworks.security.dao.UsersFacade;
import io.hops.hopsworks.security.util.SecuritySettings;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class MasterPasswordService {
  private final Logger LOG = Logger.getLogger(MasterPasswordService.class.getName());
  
  @EJB
  private SecuritySettings securitySettings;
  @EJB
  private UsersFacade userFacade;
  @EJB
  private MessageFacade messageFacade;
  
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
  
  @PostConstruct
  public void init() {
    masterPasswordFile = new File(securitySettings.getHopsworksMasterEncPasswordFile());
    if (!masterPasswordFile.exists()) {
      throw new IllegalStateException("Master encryption file does not exist");
    }
    
    try {
      PosixFileAttributeView fileView = Files.getFileAttributeView(masterPasswordFile.toPath(),
        PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
      Set<PosixFilePermission> filePermissions = fileView.readAttributes().permissions();
      boolean ownerRead = filePermissions.contains(PosixFilePermission.OWNER_READ);
      boolean ownerWrite = filePermissions.contains(PosixFilePermission.OWNER_WRITE);
      boolean ownerExecute = filePermissions.contains(PosixFilePermission.OWNER_EXECUTE);
      boolean groupRead = filePermissions.contains(PosixFilePermission.GROUP_READ);
      boolean groupWrite = filePermissions.contains(PosixFilePermission.GROUP_WRITE);
      boolean groupExecute = filePermissions.contains(PosixFilePermission.GROUP_EXECUTE);
      boolean othersRead = filePermissions.contains(PosixFilePermission.OTHERS_READ);
      boolean othersWrite = filePermissions.contains(PosixFilePermission.OTHERS_WRITE);
      boolean othersExecute = filePermissions.contains(PosixFilePermission.OTHERS_EXECUTE);
      
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
    Users from = userFacade.findByEmail(securitySettings.getAdminEmail());
    send(to, from, message, preview);
  }
  
  private void send(Users to, Users from, String msg, String preview) {
    Date now = new Date();
    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(now);
    String dateAndWriter = "On " + date + ", " + from.getFname() + " " + from.getLname() + " wrote: <br><br>";
    String message = "<hr>" + dateAndWriter + msg;
    Message newMsg = new Message(from, to, now, message, true, false);
    newMsg.setPath("");
    newMsg.setSubject("Master encryption password changed");
    newMsg.setPreview(preview);
    messageFacade.save(newMsg);
  }
  
}
