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

import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKey;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKeyId;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKeysFacade;
import io.hops.hopsworks.common.user.ThirdPartyApiKeysController;
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
public class ThirdPartyAPITokensPasswordHander implements MasterPasswordHandler {
  private final Logger LOGGER = Logger.getLogger(ThirdPartyAPITokensPasswordHander.class.getName());
  
  @EJB
  private ThirdPartyApiKeysController thirdPartyApiKeysController;
  @EJB
  private SymmetricEncryptionService symmetricEncryptionService;
  @EJB
  private ThirdPartyApiKeysFacade thirdPartyApiKeysFacade;
  
  @Override
  public void pre() {
  }
  
  @Override
  public MasterPasswordChangeResult perform(String oldPassword, String newPassword) {
    Map<ThirdPartyApiKeyId, byte[]> keys2Rollback = new HashMap<>();
    StringBuilder successLog = new StringBuilder();
    successLog.append("Performing change of master password for Third-party API keys\n");
    ThirdPartyApiKeyId keyId;
    ThirdPartyApiKey newApiKey;
    SymmetricEncryptionDescriptor descriptor;
    
    try {
      LOGGER.log(Level.INFO, "Updating third-party API keys with new Hopsworks master encryption password");
      List<ThirdPartyApiKey> cipheredKeys = thirdPartyApiKeysController.getAllCipheredApiKeys();
      
      for (ThirdPartyApiKey cipheredKey : cipheredKeys) {
        keyId = cipheredKey.getId();
        keys2Rollback.put(keyId, cipheredKey.getKey());
        
        // First decrypt with the old password
        byte[][] cryptoPrimitives = symmetricEncryptionService.splitPayloadFromCryptoPrimitives(cipheredKey.getKey());
        descriptor = new SymmetricEncryptionDescriptor.Builder()
            .setPassword(oldPassword)
            .setSalt(cryptoPrimitives[0])
            .setIV(cryptoPrimitives[1])
            .setInput(cryptoPrimitives[2])
            .build();
        descriptor = symmetricEncryptionService.decrypt(descriptor);
        
        // Then encrypt plaintext key with the new password
        descriptor = new SymmetricEncryptionDescriptor.Builder()
            .setInput(descriptor.getOutput())
            .setPassword(newPassword)
            .build();
        descriptor = symmetricEncryptionService.encrypt(descriptor);
        byte[] newCipheredKey = symmetricEncryptionService.mergePayloadWithCryptoPrimitives(descriptor.getSalt(),
            descriptor.getIv(), descriptor.getOutput());
        
        // Store new API key
        newApiKey = new ThirdPartyApiKey(keyId, newCipheredKey, cipheredKey.getAddedOn());
        thirdPartyApiKeysFacade.update(newApiKey);
        successLog.append("Updated key <").append(newApiKey.getId().getUid()).append(",")
            .append(newApiKey.getId().getName()).append(">\n");
      }
      
      return new MasterPasswordChangeResult<>(successLog, keys2Rollback, null);
    } catch (Exception ex) {
      String errorMsg = "Error while updating master encryption password for Third-party API keys";
      LOGGER.log(Level.SEVERE, errorMsg, ex);
      return new MasterPasswordChangeResult<>(keys2Rollback, new EncryptionMasterPasswordException(errorMsg, ex));
    }
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void rollback(MasterPasswordChangeResult result) {
    Map<ThirdPartyApiKeyId, byte[]> keys2rollback = (HashMap<ThirdPartyApiKeyId, byte[]>)result.getRollbackItems();
    LOGGER.log(Level.INFO, "Rolling back third-party API keys");
    ThirdPartyApiKey key;
    for (Map.Entry<ThirdPartyApiKeyId, byte[]> key2rollback : keys2rollback.entrySet()) {
      ThirdPartyApiKey persistedKey = thirdPartyApiKeysFacade.findById(key2rollback.getKey());
      if (persistedKey == null) {
        continue;
      }
      key = new ThirdPartyApiKey(key2rollback.getKey(), key2rollback.getValue(), persistedKey.getAddedOn());
      thirdPartyApiKeysFacade.update(key);
    }
  }
  
  @Override
  public void post() {
  }
}
