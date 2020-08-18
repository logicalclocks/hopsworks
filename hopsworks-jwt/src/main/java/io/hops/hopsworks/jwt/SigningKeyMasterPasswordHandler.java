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
package io.hops.hopsworks.jwt;

import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;
import io.hops.hopsworks.jwt.dao.JwtSigningKeyFacade;
import io.hops.hopsworks.persistence.entity.jwt.JwtSigningKey;
import io.hops.hopsworks.security.password.MasterPasswordChangeResult;
import io.hops.hopsworks.security.password.MasterPasswordHandler;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SigningKeyMasterPasswordHandler implements MasterPasswordHandler {
  private final Logger LOGGER = Logger.getLogger(SigningKeyMasterPasswordHandler.class.getName());
  @EJB
  private JwtSigningKeyFacade jwtSigningKeyFacade;
  @EJB
  private SigningKeyEncryptionService signingKeyEncryptionService;
  
  @Override
  public void pre() {
  
  }
  
  @Override
  public MasterPasswordChangeResult perform(String oldPassword, String newPassword) {
    StringBuilder successLog = new StringBuilder();
    //signingKeyEncryptionService.invalidateCache();
    Map<Integer, String> items2rollback = new HashMap<>();
    for (JwtSigningKey key : jwtSigningKeyFacade.findAll()) {
      try {
        String newSecret = signingKeyEncryptionService.getNewEncryptedSecret(key.getSecret(), oldPassword, newPassword);
        key.setSecret(newSecret);
        jwtSigningKeyFacade.merge(key);
        items2rollback.putIfAbsent(key.getId(), key.getSecret());
        successLog.append("Updated jwt signing key: ").append(key.getName()).append("\n");
      } catch (Exception ex) {
        String errorMsg = "Something went wrong while updating master encryption password for jwt signing key. Update" +
          " failed on key: " + key.getName();
        LOGGER.log(Level.SEVERE, errorMsg + " rolling back...", ex);
        return new MasterPasswordChangeResult<>(items2rollback, new EncryptionMasterPasswordException(errorMsg));
      }
    }
    return new MasterPasswordChangeResult<>(successLog, items2rollback, null);
  }
  
  @Override
  public void rollback(MasterPasswordChangeResult result) {
  
  }
  
  @Override
  public void post() {
  
  }
}
