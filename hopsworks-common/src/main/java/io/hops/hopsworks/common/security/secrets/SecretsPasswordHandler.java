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

import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.persistence.entity.user.security.secrets.SecretId;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.security.MasterPasswordChangeResult;
import io.hops.hopsworks.common.security.MasterPasswordHandler;
import io.hops.hopsworks.common.security.SymmetricEncryptionDescriptor;
import io.hops.hopsworks.common.security.SymmetricEncryptionService;
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
public class SecretsPasswordHandler implements MasterPasswordHandler {
  private final Logger LOGGER = Logger.getLogger(SecretsPasswordHandler.class.getName());
  
  @EJB
  private SecretsController secretsController;
  @EJB
  private SymmetricEncryptionService symmetricEncryptionService;
  @EJB
  private SecretsFacade secretsFacade;
  
  @Override
  public void pre() {
  }
  
  @Override
  public MasterPasswordChangeResult perform(String oldPassword, String newPassword) {
    Map<SecretId, byte[]> secrets2Rollback = new HashMap<>();
    StringBuilder successLog = new StringBuilder();
    successLog.append("Performing change of master password for Secrets\n");
    SecretId secretId;
    Secret newSecret;
    SymmetricEncryptionDescriptor inDescriptor;
    SymmetricEncryptionDescriptor outDescriptor;
    
    try {
      LOGGER.log(Level.INFO, "Updating Secrets with new Hopsworks master encryption password");
      List<Secret> cipheredSecrets = secretsController.getAllCiphered();
      
      for (Secret cipheredSecret : cipheredSecrets) {
        secretId = cipheredSecret.getId();
        secrets2Rollback.put(secretId, cipheredSecret.getSecret());
        
        // First decrypt with the old password
        byte[][] cryptoPrimitives = symmetricEncryptionService
            .splitPayloadFromCryptoPrimitives(cipheredSecret.getSecret());
        inDescriptor = new SymmetricEncryptionDescriptor.Builder()
            .setPassword(oldPassword)
            .setSalt(cryptoPrimitives[0])
            .setIV(cryptoPrimitives[1])
            .setInput(cryptoPrimitives[2])
            .build();
        outDescriptor = symmetricEncryptionService.decrypt(inDescriptor);
        inDescriptor.clearPassword();
        
        // Then encrypt plaintext secret with the new password
        inDescriptor = new SymmetricEncryptionDescriptor.Builder()
            .setInput(outDescriptor.getOutput())
            .setPassword(newPassword)
            .build();
        outDescriptor = symmetricEncryptionService.encrypt(inDescriptor);
        inDescriptor.clearPassword();
        byte[] newCipheredSecret = symmetricEncryptionService.mergePayloadWithCryptoPrimitives(outDescriptor.getSalt(),
            outDescriptor.getIv(), outDescriptor.getOutput());
        
        // Store new API key
        newSecret = new Secret(secretId, newCipheredSecret, cipheredSecret.getAddedOn());
        newSecret.setVisibilityType(cipheredSecret.getVisibilityType());
        if (cipheredSecret.getProjectIdScope() != null) {
          newSecret.setProjectIdScope(cipheredSecret.getProjectIdScope());
        }
        secretsFacade.update(newSecret);
        successLog.append("Updated Secret <").append(newSecret.getId().getUid()).append(",")
            .append(newSecret.getId().getName()).append(">\n");
      }
      
      return new MasterPasswordChangeResult<>(successLog, secrets2Rollback, null);
    } catch (Exception ex) {
      String errorMsg = "Error while updating master encryption password for Secrets";
      LOGGER.log(Level.SEVERE, errorMsg, ex);
      return new MasterPasswordChangeResult<>(secrets2Rollback, new EncryptionMasterPasswordException(errorMsg, ex));
    }
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void rollback(MasterPasswordChangeResult result) {
    Map<SecretId, byte[]> secrets2rollback = (HashMap<SecretId, byte[]>)result.getRollbackItems();
    LOGGER.log(Level.INFO, "Rolling back Secrets");
    Secret secret;
    for (Map.Entry<SecretId, byte[]> secret2rollback : secrets2rollback.entrySet()) {
      Secret persistedSecret = secretsFacade.findById(secret2rollback.getKey());
      if (persistedSecret == null) {
        continue;
      }
      secret = new Secret(secret2rollback.getKey(), secret2rollback.getValue(), persistedSecret.getAddedOn());
      secret.setVisibilityType(persistedSecret.getVisibilityType());
      if (persistedSecret.getProjectIdScope() != null) {
        secret.setProjectIdScope(persistedSecret.getProjectIdScope());
      }
      secretsFacade.update(secret);
    }
  }
  
  @Override
  public void post() {
  }
}
