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

package io.hops.hopsworks.common.security.secrets;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.security.MasterPasswordChangeResult;
import io.hops.hopsworks.common.security.MasterPasswordHandler;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;
import org.apache.commons.lang3.tuple.ImmutablePair;

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
public class EncryptionAtRestPasswordHandler implements MasterPasswordHandler {
  private final Logger LOGGER = Logger.getLogger(EncryptionAtRestPasswordHandler.class.getName());
  
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private Settings settings;

  @Override
  public void pre() {
  }
  
  @Override
  public MasterPasswordChangeResult perform(String oldPassword, String newPassword) {
    Map<String, ImmutablePair<String, String>> zfsKeys2Rollback = new HashMap<>();
    StringBuilder successLog = new StringBuilder();
    successLog.append("Performing change of master password for ZFS Keys\n");

    if (!settings.isEncryptionAtRestEnabled()) {
      successLog.append("Nothing to do: ZFS Encryption at REST not enabled.\n");
      return new MasterPasswordChangeResult<>(successLog, zfsKeys2Rollback, null);
    }

    try {
      LOGGER.log(Level.INFO, "Updating ZFS Keys with new Hopsworks master encryption password");
      List<Hosts> hosts = hostsFacade.findAllHosts();
      
      for (Hosts host : hosts) {
        String zfsKey = host.getZfsKey();
        zfsKey = (zfsKey == null) ? "" : zfsKey;
        String zfsKeyRotated = host.getZfsKeyRotated();
        zfsKeyRotated = (zfsKeyRotated == null) ? "" : zfsKeyRotated;
        ImmutablePair<String, String> entries = new ImmutablePair<>(zfsKey, zfsKeyRotated);
        zfsKeys2Rollback.put(host.getHostname(), entries);

        if (!zfsKey.isEmpty()) {
          String secret = this.certificatesMgmService.decryptPasswordWithMasterPassword(zfsKey, oldPassword);
          String encrypted = this.certificatesMgmService.encryptPasswordWithMasterPassword(secret, newPassword);
          host.setZfsKey(encrypted);
        }
        if (!zfsKeyRotated.isEmpty()) {
          String secret = this.certificatesMgmService.decryptPasswordWithMasterPassword(zfsKeyRotated, oldPassword);
          String encrypted = this.certificatesMgmService.encryptPasswordWithMasterPassword(secret, newPassword);
          host.setZfsKeyRotated(encrypted);
        }

        hostsFacade.storeHost(host);
        successLog.append("Updated ZFS keys for <").append(host.getHostIp()).append(">\n");
      }
      
      return new MasterPasswordChangeResult<>(successLog, zfsKeys2Rollback, null);
    } catch (Exception ex) {
      String errorMsg = "Error while updating master encryption password for ZFS keys";
      LOGGER.log(Level.SEVERE, errorMsg, ex);
      return new MasterPasswordChangeResult<>(zfsKeys2Rollback, new EncryptionMasterPasswordException(errorMsg, ex));
    }
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void rollback(MasterPasswordChangeResult result) {
    Map<String, ImmutablePair<String, String>> zfsKeys2Rollback =
            (Map<String, ImmutablePair<String, String>>)result.getRollbackItems();
    LOGGER.log(Level.INFO, "Rolling back ZFS keys");

    for (String hostname : zfsKeys2Rollback.keySet()) {
      ImmutablePair<String, String> zfsKeys = zfsKeys2Rollback.get(hostname);
      Hosts host = hostsFacade.findByHostname(hostname);
      if (host == null) {
        continue;
      }
      host.setZfsKey(zfsKeys.getKey());
      host.setZfsKeyRotated(zfsKeys.getValue());
      hostsFacade.storeHost(host);
    }
  }
  
  @Override
  public void post() {
  }
}
