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
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.EncryptionMasterPasswordException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for Project *Specific* User certificates when the master encryption password changes.
 * If something goes wrong during update, the old passwords are kept in the database.
 * The handler needs to register with the CertificateMgmService.
 * @see CertificatesMgmService#init()
 */
public class PSUserCertsMasterPasswordHandler implements CertificatesMgmService
    .MasterPasswordChangeHandler<CertsFacade> {
  private final Logger LOG = Logger.getLogger(PSUserCertsMasterPasswordHandler.class.getName());
  
  private final Map<String, String> oldPasswordsForRollback;
  private CertsFacade certsFacade;
  private final UserFacade userFacade;
  
  PSUserCertsMasterPasswordHandler(UserFacade userFacade) {
    this.userFacade = userFacade;
    oldPasswordsForRollback = new HashMap<>();
  }
  
  @Override
  public void setFacade(CertsFacade certsFacade) {
    this.certsFacade = certsFacade;
  }
  
  @Override
  public List<String> handleMasterPasswordChange(String oldMasterPassword, String newMasterPassword)
      throws EncryptionMasterPasswordException {
    List<String> updatedCertsName = new ArrayList<>();
    
    List<UserCerts> allPSCerts = certsFacade.findAllUserCerts();
    String mapKey = null, oldPassword, newEncCertPassword;
    Users user;
    
    try {
      for (UserCerts psCert : allPSCerts) {
        mapKey = psCert.getUserCertsPK().getProjectname() + HdfsUsersController.USER_NAME_DELIMITER
            + psCert.getUserCertsPK().getUsername();
        oldPassword = psCert.getUserKeyPwd();
        oldPasswordsForRollback.putIfAbsent(mapKey, oldPassword);
        user = userFacade.findByUsername(psCert.getUserCertsPK().getUsername());
        if (user == null) {
          throw new Exception("Could not find Hopsworks user for certificate " + mapKey);
        }
        newEncCertPassword = getNewUserPassword(user.getPassword(), oldPassword, oldMasterPassword,
            newMasterPassword);
        psCert.setUserKeyPwd(newEncCertPassword);
        certsFacade.persist(psCert);
        updatedCertsName.add(mapKey);
      }
    
      return updatedCertsName;
    } catch (Exception ex) {
      String errorMsg = "Something went wrong while updating master encryption password for Project Specific User " +
          "certificates. PSU certificate provoked the error was: " + mapKey;
      LOG.log(Level.SEVERE, errorMsg + " rolling back...", ex);
      throw new EncryptionMasterPasswordException(errorMsg);
    }
  }
  
  @Override
  public void rollback() {
    LOG.log(Level.FINE, "Rolling back");
    for (Map.Entry<String, String> oldPassword : oldPasswordsForRollback.entrySet()) {
      String key = oldPassword.getKey();
      String value = oldPassword.getValue();
      String[] project__username = key.split(HdfsUsersController.USER_NAME_DELIMITER, 2);
      UserCerts userCerts = certsFacade.findUserCert(project__username[0], project__username[1]);
      userCerts.setUserKeyPwd(value);
      certsFacade.persist(userCerts);
    }
    oldPasswordsForRollback.clear();
  }
}
