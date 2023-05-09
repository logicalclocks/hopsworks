/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.security;

import fish.payara.cluster.Clustered;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.inject.Instance;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Clustered
@Singleton
public class CertificateMasterPwdMgm implements Serializable {
  private static final long serialVersionUID = -2885415896363678090L;
  private static final Logger LOGGER = Logger.getLogger(CertificateMasterPwdMgm.class.getName());
  
  @Lock(LockType.READ)
  @AccessTimeout(value = 3, unit = TimeUnit.SECONDS)
  public String getMasterEncryptionPassword(File masterPasswordFile) throws IOException {
    return FileUtils.readFileToString(masterPasswordFile, Charset.defaultCharset()).trim();
  }
  
  @Lock(LockType.READ)
  @AccessTimeout(value = 3, unit = TimeUnit.SECONDS)
  public void checkPassword(String providedPassword, String userRequestedEmail, File masterPasswordFile,
      UserFacade userFacade) throws IOException, EncryptionMasterPasswordException {
    String sha = DigestUtils.sha256Hex(providedPassword);
    if (!getMasterEncryptionPassword(masterPasswordFile).equals(sha)) {
      Users user = userFacade.findByEmail(userRequestedEmail);
      String logMsg = "*** Attempt to change master encryption password with wrong credentials";
      if (user != null) {
        LOGGER.log(Level.INFO, logMsg + " by user <" + user.getUsername() + ">");
      } else {
        LOGGER.log(Level.INFO, logMsg);
      }
      throw new EncryptionMasterPasswordException("Provided password is incorrect");
    }
  }
  
  @SuppressWarnings("unchecked")
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 500)
  public MasterPasswordResetResult resetMasterEncryptionPassword(String newMasterPasswd, File masterPasswordFile,
      Instance<MasterPasswordHandler> handlers, Map<Class, MasterPasswordChangeResult> handlersResult) {
    try {
      String newDigest = DigestUtils.sha256Hex(newMasterPasswd);
      callUpdateHandlers(newDigest, masterPasswordFile, handlers, handlersResult);
      updateMasterEncryptionPassword(newDigest, masterPasswordFile);
      StringBuilder successLog = gatherLogs(handlersResult);
      LOGGER.log(Level.INFO, "Master encryption password changed!");
      return new MasterPasswordResetResult(CertificatesMgmService.UPDATE_STATUS.OK, successLog.toString(), null);
    } catch (EncryptionMasterPasswordException ex) {
      String errorMsg = "*** Master encryption password update failed!!! Rolling back...";
      LOGGER.log(Level.SEVERE, errorMsg, ex);
      callRollbackHandlers(handlers, handlersResult);
      return new MasterPasswordResetResult(CertificatesMgmService.UPDATE_STATUS.FAILED, null,
        errorMsg + "\n" + ex.getMessage());
    } catch (IOException ex) {
      String errorMsg = "*** Failed to write new encryption password to file: " + masterPasswordFile.getAbsolutePath()
        + ". Rolling back...";
      LOGGER.log(Level.SEVERE, errorMsg, ex);
      callRollbackHandlers(handlers, handlersResult);
      return new MasterPasswordResetResult(CertificatesMgmService.UPDATE_STATUS.FAILED, null,
        errorMsg + "\n" + ex.getMessage());
    } finally {
      handlersResult.clear();
    }
  }
  
  private void callUpdateHandlers(String newDigest, File masterPasswordFile, Instance<MasterPasswordHandler> handlers,
      Map<Class, MasterPasswordChangeResult> handlersResult) throws EncryptionMasterPasswordException, IOException {
    for (MasterPasswordHandler handler : handlers) {
      MasterPasswordChangeResult result = handler.perform(getMasterEncryptionPassword(masterPasswordFile), newDigest);
      handlersResult.put(handler.getClass(), result);
      if (result.getCause() != null) {
        throw result.getCause();
      }
    }
  }
  
  private void callRollbackHandlers(Instance<MasterPasswordHandler> handlers,
      Map<Class, MasterPasswordChangeResult> handlersResult) {
    for (MasterPasswordHandler handler : handlers) {
      MasterPasswordChangeResult result = handlersResult.get(handler.getClass());
      if (result != null) {
        handler.rollback(result);
      }
    }
  }
  
  private StringBuilder gatherLogs(Map<Class, MasterPasswordChangeResult> handlersResult) {
    StringBuilder successLog = new StringBuilder();
    for (MasterPasswordChangeResult result : handlersResult.values()) {
      if (result.getSuccessLog() != null) {
        successLog.append(result.getSuccessLog());
        successLog.append("\n\n");
      }
    }
    return successLog;
  }
  
  //TODO move master password to some other storage than the filesystem
  private void updateMasterEncryptionPassword(String newPassword, File masterPasswordFile) throws IOException {
    FileUtils.writeStringToFile(masterPasswordFile, newPassword, Charset.defaultCharset());
  }
}
