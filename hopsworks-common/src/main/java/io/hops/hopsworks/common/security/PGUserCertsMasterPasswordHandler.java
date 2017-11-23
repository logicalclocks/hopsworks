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
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
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
 * Handler for Project *Generic* User certificates when the master encryption password changes.
 * If something goes wrong during update, the old passwords are kept in the database.
 * The handler needs to register with the CertificateMgmService.
 * @see CertificatesMgmService#init()
 */
public class PGUserCertsMasterPasswordHandler implements CertificatesMgmService
    .MasterPasswordChangeHandler<CertsFacade> {
  private final Logger LOG = Logger.getLogger(PGUserCertsMasterPasswordHandler.class.getName());
  
  private final Map<String, String> oldPasswordsForRollback;
  private final ProjectFacade projectFacade;
  private CertsFacade certsFacade;
  
  PGUserCertsMasterPasswordHandler(ProjectFacade projectFacade) {
    this.projectFacade = projectFacade;
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
    
    List<ProjectGenericUserCerts> allPGUCerts = certsFacade.findAllProjectGenericUserCerts();
    String mapKey = null, oldPassword, newEncCertPassword;
    Users user;
    
    try {
      for (ProjectGenericUserCerts pguCert : allPGUCerts) {
        mapKey = pguCert.getProjectGenericUsername();
        oldPassword = pguCert.getCertificatePassword();
        oldPasswordsForRollback.putIfAbsent(mapKey, oldPassword);
        String projectName = mapKey.split(HdfsUsersController.USER_NAME_DELIMITER)[0];
        Project project = projectFacade.findByName(projectName);
        if (project == null) {
          throw new Exception("Could not find Hopsworks project for certificate: " + mapKey);
        }
        user = project.getOwner();
        newEncCertPassword = getNewUserPassword(user.getPassword(), oldPassword, oldMasterPassword,
            newMasterPassword);
        pguCert.setCertificatePassword(newEncCertPassword);
        certsFacade.persistPGUCert(pguCert);
        updatedCertsName.add(mapKey);
      }
      return updatedCertsName;
    } catch (Exception ex) {
      String errorMsg = "Something went wrong while updating master encryption password for Project Generic User " +
          "certificates. PGU certificate provoked the error was: " + mapKey;
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
      ProjectGenericUserCerts pguCert = certsFacade.findProjectGenericUserCerts(key);
      pguCert.setCertificatePassword(value);
      certsFacade.persistPGUCert(pguCert);
    }
    oldPasswordsForRollback.clear();
  }
}
