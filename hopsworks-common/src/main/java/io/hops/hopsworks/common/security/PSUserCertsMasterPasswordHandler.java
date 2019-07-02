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

package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PSUserCertsMasterPasswordHandler implements MasterPasswordHandler {
  private final Logger LOGGER = Logger.getLogger(PSUserCertsMasterPasswordHandler.class.getName());
  
  @EJB
  private UserFacade userFacade;
  @EJB
  private CertsFacade certsFacade;
  
  
  @Override
  public void pre() {
  }
  
  @Override
  public MasterPasswordChangeResult perform(String oldMasterPassword, String newMasterPassword) {
    StringBuilder successLog = new StringBuilder();
    successLog.append("Performing change of master password for PSU certificates\n");
    
    Map<String, String> oldPasswords4Rollback = new HashMap<>();
    List<UserCerts> allPSCerts = certsFacade.findAllUserCerts();
    String mapKey = null, oldPassword, newEncCertPassword;
    Users user;
    
    try {
      LOGGER.log(Level.INFO, "Updating PSU certs with new Hopsworks master encryption password");
      for (UserCerts psCert : allPSCerts) {
        mapKey = psCert.getUserCertsPK().getProjectname() + HdfsUsersController.USER_NAME_DELIMITER
            + psCert.getUserCertsPK().getUsername();
        oldPassword = psCert.getUserKeyPwd();
        oldPasswords4Rollback.putIfAbsent(mapKey, oldPassword);
        user = userFacade.findByUsername(psCert.getUserCertsPK().getUsername());
        if (user == null) {
          throw new Exception("Could not find Hopsworks user for certificate " + mapKey);
        }
        newEncCertPassword = getNewUserPassword(user.getPassword(), oldPassword, oldMasterPassword,
            newMasterPassword);
        psCert.setUserKeyPwd(newEncCertPassword);
        certsFacade.update(psCert);
        successLog.append("Updated certificate: ").append(mapKey).append("\n");
      }
      return new MasterPasswordChangeResult<>(successLog, oldPasswords4Rollback, null);
    } catch (Exception ex) {
      String errorMsg = "Something went wrong while updating master encryption password for Project Specific User " +
          "certificates. PSU certificate provoked the error was: " + mapKey;
      LOGGER.log(Level.SEVERE, errorMsg + " rolling back...", ex);
      return new MasterPasswordChangeResult<Map<String, String>>(oldPasswords4Rollback,
          new EncryptionMasterPasswordException(errorMsg));
    }
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void rollback(MasterPasswordChangeResult result) {
    Map<String, String> items2rollback = (HashMap<String, String>) result.getRollbackItems();
    LOGGER.log(Level.INFO, "Rolling back PSU certificates");
    for (Map.Entry<String, String> oldPassword : items2rollback.entrySet()) {
      String key = oldPassword.getKey();
      String value = oldPassword.getValue();
      String[] project__username = key.split(HdfsUsersController.USER_NAME_DELIMITER, 2);
      UserCerts userCerts = certsFacade.findUserCert(project__username[0], project__username[1]);
      userCerts.setUserKeyPwd(value);
      certsFacade.update(userCerts);
    }
  }
  
  @Override
  public void post() {
  
  }
}
