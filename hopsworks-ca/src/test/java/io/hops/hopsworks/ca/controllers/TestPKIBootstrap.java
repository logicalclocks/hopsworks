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

import io.hops.hopsworks.ca.persistence.SerialNumberFacade;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import static io.hops.hopsworks.ca.controllers.PKI.EMPTY_CONFIGURATION;


public class TestPKIBootstrap {

  @Test
  public void testInitSerialNumber() throws Exception {
    SerialNumberFacade serialNumberFacade = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(serialNumberFacade.isInitialized(Mockito.any())).thenReturn(false);
    PKI realPki = new PKI();
    PKI pki = Mockito.spy(realPki);
    pki.setSerialNumberFacade(serialNumberFacade);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    Mockito.doReturn(false).when(pki).loadFromFile();

    pki.caInitializeSerialNumber(CAType.ROOT);

    Mockito.verify(serialNumberFacade).initialize(Mockito.eq(CAType.ROOT));
    Mockito.verify(pki, Mockito.never()).getPathToSerialNumber(Mockito.any());
  }

  @Test
  public void testBootstrapSerialNumber() throws Exception {
    SerialNumberFacade serialNumberFacade = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(serialNumberFacade.isInitialized(Mockito.any())).thenReturn(false);
    PKI realPki = new PKI();
    PKI pki = Mockito.spy(realPki);
    pki.setSerialNumberFacade(serialNumberFacade);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    Mockito.doReturn(true).when(pki).loadFromFile();
    Path pathToSerialNumber = Paths.get("/tmp/file");
    Mockito.doReturn(pathToSerialNumber).when(pki).getPathToSerialNumber(CAType.ROOT);
    Mockito.doThrow(new FileNotFoundException("oops")).when(pki).getPathToSerialNumber(CAType.INTERMEDIATE);
    Mockito.doThrow(new FileNotFoundException("oops")).when(pki).getPathToSerialNumber(CAType.KUBECA);
    ArgumentCaptor<Path> pathToArtifactCaptor = ArgumentCaptor.forClass(Path.class);
    Mockito.doNothing().when(pki).migrateSerialNumber(Mockito.any(), Mockito.any());

    pki.caInitializeSerialNumber(CAType.ROOT);
    Mockito.verify(pki).getPathToSerialNumber(Mockito.eq(CAType.ROOT));
    Mockito.verify(pki).migrateSerialNumber(Mockito.eq(CAType.ROOT), pathToArtifactCaptor.capture());
    Assert.assertEquals(pathToSerialNumber.toString(), pathToArtifactCaptor.getValue().toString());
    Mockito.verify(serialNumberFacade, Mockito.never()).initialize(Mockito.any());

    // When type is KUBECA we swallow the exception as the cluster might have not been configured with Kubernete
    // and we generate new serial number
    Assert.assertThrows(IOException.class, () -> {
      pki.caInitializeSerialNumber(CAType.KUBECA);
    });

    Assert.assertThrows(IOException.class, () -> {
      pki.caInitializeSerialNumber(CAType.INTERMEDIATE);
    });
  }

  @Test
  public void testMigrateSerialNumber() throws Exception {
    Path snFile = Paths.get(System.getProperty("java.io.tmpdir"), "serial_number_test");
    try {
      // openssl serial number is hex
      FileUtils.writeStringToFile(snFile.toFile(), "1023", Charset.defaultCharset());
      SerialNumberFacade serialNumberFacade = Mockito.mock(SerialNumberFacade.class);
      PKI realPki = new PKI();
      PKI pki = Mockito.spy(realPki);
      pki.setSerialNumberFacade(serialNumberFacade);
      Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();

      ArgumentCaptor<Long> serialNumberCaptor = ArgumentCaptor.forClass(Long.class);
      pki.migrateSerialNumber(CAType.ROOT, snFile);

      Mockito.verify(serialNumberFacade).initializeWithNumber(Mockito.eq(CAType.ROOT), serialNumberCaptor.capture());
      Long expectedSerialNumber = 4131L;
      Assert.assertEquals(expectedSerialNumber, serialNumberCaptor.getValue());
    } finally {
      FileUtils.deleteQuietly(snFile.toFile());
    }
  }

  @Test
  public void testInitializeKeyPair() throws Exception {
    PKI realPki = new PKI();
    PKI pki = Mockito.spy(realPki);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    Mockito.doReturn(Optional.empty()).when(pki).loadKeyPair(Mockito.any());
    Mockito.doReturn(false).when(pki).loadFromFile();
    Mockito.doReturn(null).when(pki).generateKeyPair();
    Pair<Boolean, KeyPair> keyPair = pki.loadOrGenerateKeypair("ROOT");
    Assert.assertFalse(keyPair.getLeft());
    Assert.assertNull(keyPair.getRight());
    Mockito.verify(pki, Mockito.never()).loadKeyPairFromFile(Mockito.any());
  }

  @Test
  public void testBootstrapKeyPair() throws Exception {
    PKI realPki = new PKI();
    PKI pki = Mockito.spy(realPki);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    Mockito.doReturn(Optional.empty()).when(pki).loadKeyPair(Mockito.any());
    Mockito.doReturn(true).when(pki).loadFromFile();
    Mockito.doReturn(null).when(pki).loadKeyPairFromFile(CAType.ROOT.name());
    Mockito.doReturn(null).when(pki).generateKeyPair();
    Mockito.doThrow(new FileNotFoundException("oops")).when(pki).loadKeyPairFromFile(CAType.INTERMEDIATE.name());
    Mockito.doThrow(new FileNotFoundException("oops")).when(pki).loadKeyPairFromFile(CAType.KUBECA.name());

    Pair<Boolean, KeyPair> keyPair = pki.loadOrGenerateKeypair(CAType.ROOT.name());
    Mockito.verify(pki, Mockito.never()).generateKeyPair();
    Assert.assertFalse(keyPair.getLeft());
    Assert.assertNull(keyPair.getRight());

    Assert.assertThrows(InvalidKeySpecException.class, () -> {
      pki.loadOrGenerateKeypair(CAType.KUBECA.name());
    });

    Assert.assertThrows(InvalidKeySpecException.class, () -> {
      pki.loadOrGenerateKeypair(CAType.INTERMEDIATE.name());
    });
  }

  @Test
  public void testInitializeCertificate() throws Exception {
    PKI realPki = new PKI();
    PKI pki = Mockito.spy(realPki);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    Mockito.doReturn(Optional.empty()).when(pki).loadCertificate(Mockito.any());
    Mockito.doReturn(false).when(pki).loadFromFile();
    Mockito.doReturn(null).when(pki).generateRootCACertificate();

    Pair<Boolean, X509Certificate> certificate = pki.loadOrGenerateCACertificate(CAType.ROOT);
    Assert.assertFalse(certificate.getLeft());
    Assert.assertNull(certificate.getRight());
    Mockito.verify(pki, Mockito.never()).loadCACertificate(Mockito.any());
  }

  @Test
  public void testBootstrapCertificate() throws Exception {
    PKI realPki = new PKI();
    PKI pki = Mockito.spy(realPki);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    Mockito.doReturn(Optional.empty()).when(pki).loadCertificate(Mockito.any());
    Mockito.doReturn(true).when(pki).loadFromFile();
    Mockito.doReturn(null).when(pki).loadCACertificate(CAType.ROOT);
    Mockito.doThrow(new FileNotFoundException("oops")).when(pki).loadCACertificate(CAType.INTERMEDIATE);
    Mockito.doThrow(new FileNotFoundException("oops")).when(pki).loadCACertificate(CAType.KUBECA);
    Mockito.doReturn(null).when(pki).generateCertificate(Mockito.any());
    Mockito.doReturn(null).when(pki).prepareKubernetesCAGenerationParams();

    Pair<Boolean, X509Certificate> certificate = pki.loadOrGenerateCACertificate(CAType.ROOT);
    Assert.assertFalse(certificate.getLeft());
    Assert.assertNull(certificate.getRight());

    Mockito.verify(pki, Mockito.never()).generateRootCACertificate();

    Assert.assertThrows(CertificateException.class, () -> {
      pki.loadOrGenerateCACertificate(CAType.KUBECA);
    });

    Assert.assertThrows(CertificateException.class, () -> {
      pki.loadOrGenerateCACertificate(CAType.INTERMEDIATE);
    });
  }
}
