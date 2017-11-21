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

import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificate;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificateFacade;
import io.hops.hopsworks.common.exception.EncryptionMasterPasswordException;
import io.hops.hopsworks.common.util.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for clusters certificates for Dela when the master encryption password changes.
 * If something goes wrong during update, the old passwords are kept in the database.
 * The handler needs to register with the CertificateMgmService.
 * @see CertificatesMgmService#init()
 */
public class DelaCertsMasterPasswordHandler implements CertificatesMgmService
    .MasterPasswordChangeHandler<ClusterCertificateFacade> {
  private final Logger LOG = Logger.getLogger(DelaCertsMasterPasswordHandler.class.getName());
  private final Map<String, String> oldPasswordsForRollback;
  private final Settings settings;
  
  private ClusterCertificateFacade clusterCertificateFacade;
  
  DelaCertsMasterPasswordHandler(Settings settings) {
    this.settings = settings;
    oldPasswordsForRollback = new HashMap<>();
  }
  
  @Override
  public void setFacade(ClusterCertificateFacade certsFacade) {
    this.clusterCertificateFacade = certsFacade;
  }
  
  @Override
  public List<String> handleMasterPasswordChange(String oldMasterPassword, String newMasterPassword)
      throws EncryptionMasterPasswordException {
    List<String> updatedCertsName = new ArrayList<>();
    
    Optional<List<ClusterCertificate>> maybe = clusterCertificateFacade.getAllClusterCerts();
    if (maybe.isPresent()) {
      String mapKey, oldPassword, newEncCertPassword;
      try {
        for (ClusterCertificate cert : maybe.get()) {
          mapKey = cert.getClusterName();
          oldPassword = cert.getCertificatePassword();
          oldPasswordsForRollback.putIfAbsent(mapKey, oldPassword);
          newEncCertPassword = getNewUserPassword(settings.getHopsSiteClusterPswd().get(), oldPassword,
              oldMasterPassword, newMasterPassword);
          cert.setCertificatePassword(newEncCertPassword);
          clusterCertificateFacade.saveClusterCerts(cert);
          updatedCertsName.add(mapKey);
        }
        oldPasswordsForRollback.clear();
      } catch (Exception ex) {
        String errorMsg = "Something went wrong while updating master encryption password for Cluster Certificates";
        LOG.log(Level.SEVERE, errorMsg + " rolling back...", ex);
        rollback();
        throw new EncryptionMasterPasswordException(errorMsg);
      }
    }
    return updatedCertsName;
  }
  
  @Override
  public void rollback() {
    LOG.log(Level.FINE, "Rolling back");
    for (Map.Entry<String, String> cert : oldPasswordsForRollback.entrySet()) {
      String key = cert.getKey();
      String value = cert.getValue();
      Optional<ClusterCertificate> optional = clusterCertificateFacade.getClusterCert(key);
      if (optional.isPresent()) {
        ClusterCertificate cc = optional.get();
        cc.setCertificatePassword(value);
        clusterCertificateFacade.saveClusterCerts(cc);
      }
    }
    oldPasswordsForRollback.clear();
  }
}
