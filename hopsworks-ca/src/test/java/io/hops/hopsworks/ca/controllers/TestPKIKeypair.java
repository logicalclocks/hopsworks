/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.ca.controllers;

import io.hops.hopsworks.ca.persistence.KeyFacade;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.persistence.entity.pki.PKIKey;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.util.Optional;

import static io.hops.hopsworks.ca.controllers.PKI.EMPTY_CONFIGURATION;

public class TestPKIKeypair {

  private final ArgumentCaptor<PKIKey> privateKeyCaptor = ArgumentCaptor.forClass(PKIKey.class);
  private final ArgumentCaptor<PKIKey> publicKeyCaptor = ArgumentCaptor.forClass(PKIKey.class);

  @Test
  public void testLoadKeyPair() throws Exception {
    KeyFacade kf = Mockito.mock(KeyFacade.class);
    Mockito.when(kf.getEncodedKey(Mockito.eq("owner_1"), Mockito.any())).thenReturn(null);

    PKI pki = new PKI();
    PKI pkiSpy = Mockito.spy(pki);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pkiSpy).loadConfiguration();
    pkiSpy.setKeyFacade(kf);
    pkiSpy.init();

    Optional<KeyPair> mkp = pkiSpy.loadKeyPair("owner_1");
    Assert.assertFalse(mkp.isPresent());

    KeyPair kp = pkiSpy.generateKeyPair();
    Mockito.when(kf.getEncodedKey(Mockito.eq("owner_2"), Mockito.eq(PKIKey.Type.PRIVATE)))
        .thenReturn(kp.getPrivate().getEncoded());
    Mockito.when(kf.getEncodedKey(Mockito.eq("owner_2"), Mockito.eq(PKIKey.Type.PUBLIC)))
        .thenReturn(kp.getPublic().getEncoded());
    mkp = pkiSpy.loadKeyPair("owner_2");
    Assert.assertTrue(mkp.isPresent());
    Assert.assertArrayEquals(kp.getPrivate().getEncoded(), mkp.get().getPrivate().getEncoded());
    Assert.assertArrayEquals(kp.getPublic().getEncoded(), mkp.get().getPublic().getEncoded());
  }

  @Test
  public void testLoadOrGenerateKeypair() throws Exception {
    PKI pki = new PKI();
    PKI pkiSpy = Mockito.spy(pki);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pkiSpy).loadConfiguration();
    Mockito.doReturn(false).when(pkiSpy).loadFromFile();
    pkiSpy.init();

    KeyPair mockKeypairForOwner1 = pkiSpy.generateKeyPair();
    Mockito.doReturn(Optional.of(mockKeypairForOwner1)).when(pkiSpy).loadKeyPair("owner_1");
    Mockito.doReturn(Optional.empty()).when(pkiSpy).loadKeyPair("owner_2");

    Pair<Boolean, KeyPair> kp = pkiSpy.loadOrGenerateKeypair("owner_1");
    Assert.assertTrue(kp.getLeft());
    Assert.assertEquals(mockKeypairForOwner1, kp.getRight());

    kp = pkiSpy.loadOrGenerateKeypair("owner_2");
    Assert.assertFalse(kp.getLeft());
    Assert.assertNotNull(kp.getRight());
    // First by the test itself above and second by loadOrGenerateKeypair("owner_2")
    Mockito.verify(pkiSpy, Mockito.times(2)).generateKeyPair();
  }

  @Test
  public void testCAInitializeKeys() throws Exception {
    KeyFacade keyFacade = Mockito.mock(KeyFacade.class);
    Mockito.doNothing().when(keyFacade).saveKey(Mockito.any());

    PKI pki = new PKI();
    PKI pkiSpy = Mockito.spy(pki);
    pkiSpy.setKeyFacade(keyFacade);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pkiSpy).loadConfiguration();
    pkiSpy.init();

    Assert.assertFalse(pkiSpy.getCaKeys().containsKey(CAType.ROOT));
    Assert.assertFalse(pkiSpy.getCaKeys().containsKey(CAType.INTERMEDIATE));

    KeyPair mockKeypair = pkiSpy.generateKeyPair();
    Mockito.doReturn(Pair.of(false, mockKeypair)).when(pkiSpy).loadOrGenerateKeypair(CAType.ROOT.name());
    Mockito.doReturn(Pair.of(true, mockKeypair)).when(pkiSpy).loadOrGenerateKeypair(CAType.INTERMEDIATE.name());

    pkiSpy.caInitializeKeys(CAType.ROOT);
    pkiSpy.caInitializeKeys(CAType.INTERMEDIATE);
    Mockito.verify(pkiSpy).saveKeys(privateKeyCaptor.capture(), publicKeyCaptor.capture());

    Assert.assertArrayEquals(mockKeypair.getPublic().getEncoded(),
        pkiSpy.getCaKeys().get(CAType.ROOT).getPublic().getEncoded());
    Assert.assertArrayEquals(mockKeypair.getPrivate().getEncoded(),
        pkiSpy.getCaKeys().get(CAType.ROOT).getPrivate().getEncoded());

    // CA keys cache should have been populated
    Assert.assertArrayEquals(mockKeypair.getPublic().getEncoded(),
        pkiSpy.getCaKeys().get(CAType.INTERMEDIATE).getPublic().getEncoded());
    Assert.assertArrayEquals(mockKeypair.getPrivate().getEncoded(),
        pkiSpy.getCaKeys().get(CAType.INTERMEDIATE).getPrivate().getEncoded());

    // Called twice to save the Public and Private key of ROOT.
    // INTERMEDIATE has already been initialized so no need to save them
    Mockito.verify(keyFacade, Mockito.times(2)).saveKey(Mockito.any());
    PKIKey prKeyArg = privateKeyCaptor.getValue();
    Assert.assertEquals(CAType.ROOT.name(), prKeyArg.getIdentifier().getOwner());
    Assert.assertEquals(PKIKey.Type.PRIVATE, prKeyArg.getIdentifier().getType());
    Assert.assertNotNull(prKeyArg.getKey());

    PKIKey puKeyArg = publicKeyCaptor.getValue();
    Assert.assertEquals(CAType.ROOT.name(), puKeyArg.getIdentifier().getOwner());
    Assert.assertEquals(PKIKey.Type.PUBLIC, puKeyArg.getIdentifier().getType());
    Assert.assertNotNull(puKeyArg.getKey());
  }
}
