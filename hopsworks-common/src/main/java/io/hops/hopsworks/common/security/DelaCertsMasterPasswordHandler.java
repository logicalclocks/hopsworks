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

import io.hops.hopsworks.persistence.entity.dela.certs.ClusterCertificate;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificateFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DelaCertsMasterPasswordHandler implements MasterPasswordHandler {
  private final Logger LOGGER = Logger.getLogger(DelaCertsMasterPasswordHandler.class.getName());
  
  @EJB
  private ClusterCertificateFacade clusterCertificateFacade;
  @EJB
  private Settings settings;
  
  @Override
  public void pre() {
  
  }
  
  @Override
  public MasterPasswordChangeResult perform(String oldMasterPassword, String newMasterPassword) {
    StringBuilder successLog = new StringBuilder();
    successLog.append("Performing change of master password for Dela certificates\n");
    Map<String, String> items2rollback = new HashMap<>();
    
    Optional<List<ClusterCertificate>> maybe = clusterCertificateFacade.getAllClusterCerts();
    if (maybe.isPresent()) {
      LOGGER.log(Level.INFO, "Updating Dela certs with new Hopsworks master encryption password");
      String mapKey = null, oldPassword, newEncCertPassword;
      try {
        for (ClusterCertificate cert : maybe.get()) {
          mapKey = cert.getClusterName();
          oldPassword = cert.getCertificatePassword();
          items2rollback.putIfAbsent(mapKey, oldPassword);
          newEncCertPassword = getNewUserPassword(settings.getHopsSiteClusterPswd().get(), oldPassword,
              oldMasterPassword, newMasterPassword);
          cert.setCertificatePassword(newEncCertPassword);
          clusterCertificateFacade.updateClusterCerts(cert);
          successLog.append("Updated certificate: ").append(mapKey).append("\n");
        }
      } catch (Exception ex) {
        String errorMsg = "Something went wrong while updating master encryption password for Cluster Certificates. " +
            "Cluster certificate provoked the error was: " + mapKey;
        LOGGER.log(Level.SEVERE, errorMsg + " rolling back...", ex);
        return new MasterPasswordChangeResult<>(items2rollback,
            new EncryptionMasterPasswordException(errorMsg));
      }
    }
    return new MasterPasswordChangeResult<>(successLog, items2rollback, null);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void rollback(MasterPasswordChangeResult result) {
    LOGGER.log(Level.INFO, "Rolling back Dela certificates");
    Map<String, String> items2rollback = (HashMap<String, String>) result.getRollbackItems();
    for (Map.Entry<String, String> cert : items2rollback.entrySet()) {
      String key = cert.getKey();
      String value = cert.getValue();
      Optional<ClusterCertificate> optional = clusterCertificateFacade.getClusterCert(key);
      if (optional.isPresent()) {
        ClusterCertificate cc = optional.get();
        cc.setCertificatePassword(value);
        clusterCertificateFacade.updateClusterCerts(cc);
      }
    }
  }
  
  @Override
  public void post() {
  
  }
}
