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
      String mapKey = null, oldPassword, newEncCertPassword;
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
      } catch (Exception ex) {
        String errorMsg = "Something went wrong while updating master encryption password for Cluster Certificates. " +
            "Cluster certificate provoked the error was: " + mapKey;
        LOG.log(Level.SEVERE, errorMsg + " rolling back...", ex);
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
