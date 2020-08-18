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

import io.hops.hopsworks.jwt.dao.JwtSigningKeyFacade;
import io.hops.hopsworks.jwt.exception.SigningKeyEncryptionException;
import io.hops.hopsworks.persistence.entity.jwt.JwtSigningKey;
import io.hops.hopsworks.security.encryption.SymmetricEncryptionService;
import io.hops.hopsworks.security.password.MasterPasswordService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;

public class TestSigningKeyEncryptionService {
  
  private SigningKeyEncryptionService signingKeyEncryptionService;
  private JwtSigningKeyFacade jwtSigningKeyFacade;
  private SymmetricEncryptionService symmetricEncryptionService;
  private SigningKeyGenerator signingKeyGenerator;
  private MasterPasswordService masterPasswordService;
  
  byte[] signingKey1;
  byte[] signingKey2;
  private String masterPassword;
  private Random random = new Random();
  
  @Before
  public void beforeTest() throws IOException, SigningKeyEncryptionException, NoSuchAlgorithmException {
    
    jwtSigningKeyFacade = Mockito.mock(JwtSigningKeyFacade.class);
    masterPasswordService = Mockito.mock(MasterPasswordService.class);
    signingKeyGenerator = new SigningKeyGenerator();
    symmetricEncryptionService = new SymmetricEncryptionService();
    symmetricEncryptionService.init();
    Mockito.when(masterPasswordService.getMasterEncryptionPassword()).thenReturn("master password");
    
    signingKeyEncryptionService = new SigningKeyEncryptionService(jwtSigningKeyFacade, signingKeyGenerator,
      symmetricEncryptionService, masterPasswordService);
    
    masterPassword = masterPasswordService.getMasterEncryptionPassword();
    signingKey1 = signingKeyGenerator.getSigningKey(SignatureAlgorithm.HS512.getJcaName());

    createJwtSigningKey(1, "api", signingKey1);
  
    signingKey2 = signingKeyGenerator.getSigningKey(SignatureAlgorithm.HS512.getJcaName());
  
    createJwtSigningKey(2, "service", signingKey2);

  }
  
  @Test
  public void testGetDecryptedSigningKey() throws Exception {
    DecryptedSigningKey decryptedSigningKey = signingKeyEncryptionService.getSigningKey(1);
    assertTrue (Arrays.equals(decryptedSigningKey.getDecryptedSecret(), signingKey1));
  
    decryptedSigningKey = signingKeyEncryptionService.getSigningKey(2);
    assertTrue (Arrays.equals(decryptedSigningKey.getDecryptedSecret(), signingKey2));
  }
  
  @Test
  public void testGetDecryptedSigningKeyByName() throws Exception {
    DecryptedSigningKey decryptedSigningKey =
      signingKeyEncryptionService.getOrCreateSigningKey("api", SignatureAlgorithm.HS512);
    assertTrue(Arrays.equals(decryptedSigningKey.getDecryptedSecret(), signingKey1));
    
    decryptedSigningKey = signingKeyEncryptionService.getOrCreateSigningKey("service", SignatureAlgorithm.HS512);
    assertTrue(Arrays.equals(decryptedSigningKey.getDecryptedSecret(), signingKey2));
  }
  
  @Test
  public void testCreateSigningKey() throws Exception {
    Integer id = whenPersist();
    DecryptedSigningKey decryptedSigningKey = signingKeyEncryptionService.getOrCreateSigningKey("oneTime",
      SignatureAlgorithm.HS512);
    DecryptedSigningKey decryptedSigningKey1 = signingKeyEncryptionService.getSigningKey(id);
    assertTrue (Arrays.equals(decryptedSigningKey.getDecryptedSecret(), decryptedSigningKey1.getDecryptedSecret()));
  }
  
  private Integer whenPersist() {
    Integer id = random.ints(3,10000).findFirst().getAsInt();
    doAnswer(new Answer<Object>(){
      @Override
      public Object answer(InvocationOnMock invocation){
        JwtSigningKey jwtSigningKey = (JwtSigningKey) invocation.getArguments()[0];
        persist(jwtSigningKey, id);
        return id;
      }
    }).when(jwtSigningKeyFacade).persist(Mockito.any(JwtSigningKey.class));
    return id;
  }
  
  private void createJwtSigningKey(Integer id, String name, byte[] signingKey) throws SigningKeyEncryptionException {
    String encryptedSecret = signingKeyEncryptionService.encrypt(signingKey, masterPassword);
    JwtSigningKey jwtSigningKey = new JwtSigningKey(encryptedSecret, name);
    persist(jwtSigningKey, id);
  }
  
  private void persist(JwtSigningKey jwtSigningKey, Integer id) {
    jwtSigningKey.setId(id);
    Mockito.when(jwtSigningKeyFacade.find(id)).thenReturn(jwtSigningKey);
    Mockito.when(jwtSigningKeyFacade.findByName(jwtSigningKey.getName())).thenReturn(jwtSigningKey);
  }
  
}