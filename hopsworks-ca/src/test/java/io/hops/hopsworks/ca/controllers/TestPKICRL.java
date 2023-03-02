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

import io.hops.hopsworks.ca.persistence.CRLFacade;
import io.hops.hopsworks.ca.persistence.SerialNumberFacade;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.persistence.entity.pki.PKICrl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static io.hops.hopsworks.ca.controllers.PKI.EMPTY_CONFIGURATION;

public class TestPKICRL {

  @Test
  public void testCAInitializeCRLAlreadyExists() {
    boolean noException = true;
    try {
      CRLFacade crlFacade = Mockito.mock(CRLFacade.class);
      Mockito.when(crlFacade.exist(Mockito.eq(CAType.ROOT))).thenReturn(true);
      PKI realPKI = new PKI();
      PKI pki = Mockito.spy(realPKI);
      Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
      pki.setCRLFacade(crlFacade);
      pki.init();
      pki.caInitializeCRL(CAType.ROOT);
    } catch (Exception e) {
      noException = false;
    }
    Assert.assertTrue(noException);
  }

  @Test
  public void testCAInitializeCRL() throws Exception {
    CRLFacade crlFacade = Mockito.mock(CRLFacade.class);
    Mockito.doNothing().when(crlFacade).init(pkiCRL.capture());
    SerialNumberFacade snFacadeMock = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(snFacadeMock.nextSerialNumber(Mockito.eq(CAType.ROOT))).thenReturn(1L);

    PKI realPKI = new PKI();
    PKI pki = Mockito.spy(realPKI);
    Mockito.doReturn(false).when(pki).loadFromFile();
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.setCRLFacade(crlFacade);
    pki.setSerialNumberFacade(snFacadeMock);
    pki.init();

    KeyPair rootKeypair = pki.generateKeyPair();
    pki.getCaKeys().put(CAType.ROOT, rootKeypair);
    X509Certificate rootCertificate = pki.generateRootCACertificate();
    pki.getCaCertificates().put(CAType.ROOT, rootCertificate);

    pki.caInitializeCRL(CAType.ROOT);
    PKICrl pkiCrl = pkiCRL.getValue();
    Mockito.verify(crlFacade, Mockito.times(1)).init(Mockito.any());
  }

  private final ArgumentCaptor<PKICrl> pkiCRL = ArgumentCaptor.forClass(PKICrl.class);
  private final ArgumentCaptor<X509CRL> x509CRL = ArgumentCaptor.forClass(X509CRL.class);

  @Test
  public void testLoadCRL() throws Exception {
    CRLFacade crlFacade = Mockito.mock(CRLFacade.class);
    Mockito.doNothing().when(crlFacade).init(pkiCRL.capture());
    SerialNumberFacade snFacadeMock = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(snFacadeMock.nextSerialNumber(Mockito.eq(CAType.ROOT))).thenReturn(1L);

    PKI realPKI = new PKI();
    PKI pki = Mockito.spy(realPKI);
    Mockito.doReturn(false).when(pki).loadFromFile();
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.setCRLFacade(crlFacade);
    pki.setSerialNumberFacade(snFacadeMock);
    pki.init();

    KeyPair rootKeypair = pki.generateKeyPair();
    pki.getCaKeys().put(CAType.ROOT, rootKeypair);
    X509Certificate rootCertificate = pki.generateRootCACertificate();
    pki.getCaCertificates().put(CAType.ROOT, rootCertificate);

    pki.caInitializeCRL(CAType.ROOT);
    Mockito.verify(pki).initCRL(Mockito.eq(CAType.ROOT), x509CRL.capture());
    X509CRL crl1 = x509CRL.getValue();;

    PKICrl pkiCrl = pkiCRL.getValue();
    Mockito.when(crlFacade.getCRL(Mockito.eq(CAType.ROOT)))
        .thenReturn(Optional.of(pkiCrl));
    X509CRL crl2 = pki.loadCRL(CAType.ROOT);
    Assert.assertArrayEquals(crl1.getSignature(), crl2.getSignature());
    Assert.assertEquals(rootCertificate.getSubjectDN().toString(), crl1.getIssuerDN().toString());
  }
}
