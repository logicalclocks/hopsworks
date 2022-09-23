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

import io.hops.hopsworks.ca.configuration.CAConfiguration;
import io.hops.hopsworks.ca.configuration.CAsConfiguration;
import io.hops.hopsworks.ca.persistence.PKICertificateFacade;
import io.hops.hopsworks.ca.persistence.SerialNumberFacade;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.persistence.entity.pki.PKICertificate;
import io.hops.hopsworks.persistence.entity.pki.PKICertificateId;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.CertIOException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.hops.hopsworks.ca.controllers.PKI.EMPTY_CONFIGURATION;

public class TestPKICertificate {

  @Test
  public void testGenerateRootCACertificate() throws Exception {
    PKI realPKI = new PKI();

    SerialNumberFacade snFacadeMock = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(snFacadeMock.nextSerialNumber(Mockito.eq(CAType.ROOT))).thenReturn(1L);

    PKI pki = Mockito.spy(realPKI);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.setSerialNumberFacade(snFacadeMock);
    pki.init();

    KeyPair kp = pki.generateKeyPair();
    pki.getCaKeys().put(CAType.ROOT, kp);

    X509Certificate rootCert = pki.generateRootCACertificate();
    Assert.assertEquals(1L, rootCert.getSerialNumber().longValue());
    X500Name rootName = pki.getCaSubjectNames().get(CAType.ROOT);
    Assert.assertEquals(rootName.toString(), rootCert.getSubjectDN().toString());
    Assert.assertEquals(rootName.toString(), rootCert.getIssuerDN().toString());
    // Root CA can have maximum 10 sub-CAs
    Assert.assertEquals(10, rootCert.getBasicConstraints());
    Set<String> criticalExtensions = rootCert.getCriticalExtensionOIDs();
    Assert.assertEquals(2, criticalExtensions.size());
    Assert.assertTrue(criticalExtensions.contains(Extension.keyUsage.toString()));
    Assert.assertTrue(criticalExtensions.contains(Extension.basicConstraints.toString()));

    Set<String> nonCriticalExtensions = rootCert.getNonCriticalExtensionOIDs();
    Assert.assertEquals(2, nonCriticalExtensions.size());
    Assert.assertTrue(nonCriticalExtensions.contains(Extension.subjectKeyIdentifier.toString()));
    Assert.assertTrue(nonCriticalExtensions.contains(Extension.authorityKeyIdentifier.toString()));

    rootCert.checkValidity();
    rootCert.verify(kp.getPublic());
  }

  @Test
  public void testGenerateCACertificate() throws Exception {
    PKI realPKI = new PKI();

    SerialNumberFacade snFacadeMock = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(snFacadeMock.nextSerialNumber(Mockito.eq(CAType.ROOT)))
        .thenReturn(1L);

    PKI pki = Mockito.spy(realPKI);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.setSerialNumberFacade(snFacadeMock);
    pki.init();

    KeyPair rkp = pki.generateKeyPair();
    pki.getCaKeys().put(CAType.ROOT, rkp);

    KeyPair ikp = pki.generateKeyPair();

    X509Certificate rootCert = pki.generateRootCACertificate();
    X500Name name = new X500Name("CN=hello,O=hopsworks");
    Instant notBefore = Instant.now();
    Instant notAfter = notBefore.plus(1, ChronoUnit.DAYS);

    PKI.CertificateGenerationParameters params = new PKI.CertificateGenerationParameters(
        new PKI.CertificateSigner(rkp, rootCert),
        ikp,
        2L,
        name,
        new PKI.CertificateValidityPeriod(notBefore, notAfter),
        (builder -> {
          try {
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(50));
            return null;
          } catch (CertIOException ex) {
            throw new RuntimeException(ex);
          }
        }));
    X509Certificate cert = pki.generateCertificate(params);
    cert.checkValidity();
    cert.verify(rkp.getPublic());

    Assert.assertEquals(2L, cert.getSerialNumber().longValue());
    Assert.assertEquals(name.toString(), cert.getSubjectDN().toString());
    Assert.assertEquals(pki.getCaSubjectNames().get(CAType.ROOT).toString(), cert.getIssuerDN().toString());
    Assert.assertEquals(notBefore.getEpochSecond() * 1000, cert.getNotBefore().getTime());
    Assert.assertEquals(notAfter.getEpochSecond() * 1000, cert.getNotAfter().getTime());

    // These are implicitly added
    Set<String> nonCriticalExtensions = cert.getNonCriticalExtensionOIDs();
    Assert.assertEquals(2, nonCriticalExtensions.size());
    Assert.assertTrue(nonCriticalExtensions.contains(Extension.authorityKeyIdentifier.toString()));
    Assert.assertTrue(nonCriticalExtensions.contains(Extension.subjectKeyIdentifier.toString()));

    // This is added by the extensions function in CertificateGenerationParameter
    Set<String> criticalExtensions = cert.getCriticalExtensionOIDs();
    Assert.assertEquals(1, criticalExtensions.size());
    Assert.assertTrue(criticalExtensions.contains(Extension.basicConstraints.toString()));
    Assert.assertEquals(50, cert.getBasicConstraints());
  }

  @Test
  public void testLoadCertificate() throws Exception {
    SerialNumberFacade snFacadeMock = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(snFacadeMock.nextSerialNumber(Mockito.eq(CAType.ROOT)))
        .thenReturn(1L);

    PKI realPki = new PKI();
    PKI pki = Mockito.spy(realPki);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.setSerialNumberFacade(snFacadeMock);
    pki.init();

    KeyPair rkp = pki.generateKeyPair();
    pki.getCaKeys().put(CAType.ROOT, rkp);


    X509Certificate rootCert = pki.generateRootCACertificate();
    PKICertificate pkiCertificate = new PKICertificate(
        new PKICertificateId(PKICertificate.Status.VALID, rootCert.getSubjectDN().toString()),
        CAType.ROOT,
        1L,
        rootCert.getEncoded(),
        rootCert.getNotBefore(),
        rootCert.getNotAfter());

    PKICertificateFacade certificateFacade = Mockito.mock(PKICertificateFacade.class);
    Mockito.when(
        certificateFacade
            .findBySubjectAndStatus(Mockito.eq("CN=owner_1"), Mockito.eq(PKICertificate.Status.VALID)))
        .thenReturn(Optional.empty());
    Mockito.when(
        certificateFacade
            .findBySubjectAndStatus(Mockito.eq(pki.getCaSubjectNames().get(CAType.ROOT).toString()),
                Mockito.eq(PKICertificate.Status.VALID)))
        .thenReturn(Optional.of(pkiCertificate));

    pki.setPkiCertificateFacade(certificateFacade);

    Optional<X509Certificate> maybeCertificate = pki.loadCertificate("CN=owner_1");
    Assert.assertFalse(maybeCertificate.isPresent());

    maybeCertificate = pki.loadCertificate(pki.getCaSubjectNames().get(CAType.ROOT).toString());
    Assert.assertTrue(maybeCertificate.isPresent());
    X509Certificate certificate = maybeCertificate.get();
    Assert.assertEquals(pki.getCaSubjectNames().get(CAType.ROOT).toString(), certificate.getSubjectDN().toString());
    Assert.assertEquals(1L, certificate.getSerialNumber().longValue());
  }

  @Test
  public void testLoadOrGenerateCACertificate() throws Exception {
    PKI realPKI = new PKI();
    SerialNumberFacade snFacadeMock = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(snFacadeMock.nextSerialNumber(Mockito.eq(CAType.ROOT)))
        .thenReturn(1L);
    PKI pki = Mockito.spy(realPKI);
    pki.setSerialNumberFacade(snFacadeMock);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();

    KeyPair rkp = pki.generateKeyPair();
    pki.getCaKeys().put(CAType.ROOT, rkp);

    X509Certificate cert = pki.generateRootCACertificate();
    Mockito
        .doReturn(Optional.of(cert))
        .doReturn(Optional.empty())
        .doReturn(Optional.empty())
        .doReturn(Optional.empty())
        .when(pki).loadCertificate(Mockito.any());


    Mockito.doReturn(cert).when(pki).generateRootCACertificate();
    Mockito.doReturn(null).when(pki).prepareIntermediateCAGenerationParams();
    Mockito.doReturn(cert).when(pki).generateCertificate(Mockito.any());

    // Ten years default validity period
    Assert.assertEquals(Instant.now().plus(PKIUtils.TEN_YEARS, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
        cert.getNotAfter().toInstant().truncatedTo(ChronoUnit.DAYS));

    // First time we call it, it should load the existing certificate
    Pair<Boolean, X509Certificate> result = pki.loadOrGenerateCACertificate(CAType.ROOT);
    Assert.assertTrue(result.getLeft());
    Assert.assertArrayEquals(cert.getSignature(), result.getRight().getSignature());

    // Following times loadCertificate should return that certificate does not exist
    // and it should generate certificates
    result = pki.loadOrGenerateCACertificate(CAType.ROOT);
    Assert.assertFalse(result.getLeft());

    result = pki.loadOrGenerateCACertificate(CAType.INTERMEDIATE);
    Assert.assertFalse(result.getLeft());

    Mockito.verify(pki, Mockito.times(2)).generateRootCACertificate();
    Mockito.verify(pki, Mockito.times(1)).generateCertificate(Mockito.any());
  }

  private final ArgumentCaptor<CAType> caType = ArgumentCaptor.forClass(CAType.class);

  @Test
  public void testCAInitializeCertificate() throws Exception {
    PKI realPKI = new PKI();
    SerialNumberFacade snFacadeMock = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(snFacadeMock.nextSerialNumber(Mockito.eq(CAType.ROOT)))
        .thenReturn(1L);
    PKI pki = Mockito.spy(realPKI);
    pki.setSerialNumberFacade(snFacadeMock);
    CAConfiguration rootCAConf = new CAConfiguration(null, "10d");
    CAsConfiguration casConf = new CAsConfiguration(rootCAConf, null, null);
    Mockito.doReturn(casConf).when(pki).loadConfiguration();
    PKIUtils pkiUtils = new PKIUtils();
    pki.setPKIUtils(pkiUtils);
    pki.init();

    KeyPair rkp = pki.generateKeyPair();
    pki.getCaKeys().put(CAType.ROOT, rkp);

    X509Certificate cert = pki.generateRootCACertificate();
    Mockito
        .doReturn(Pair.of(false, cert))
        .doReturn(Pair.of(false, cert))
        .doReturn(Pair.of(true, cert))
        .when(pki).loadOrGenerateCACertificate(Mockito.any());

    Mockito
        .doNothing().when(pki).saveNewCertificate(caType.capture(), Mockito.any());

    Assert.assertFalse(pki.getCaCertificates().containsKey(CAType.ROOT));
    Assert.assertFalse(pki.getCaCertificates().containsKey(CAType.INTERMEDIATE));
    Assert.assertFalse(pki.getCaCertificates().containsKey(CAType.KUBECA));

    pki.caInitializeCertificate(CAType.ROOT);
    pki.caInitializeCertificate(CAType.INTERMEDIATE);
    pki.caInitializeCertificate(CAType.KUBECA);

    Mockito.verify(pki, Mockito.times(2)).saveNewCertificate(Mockito.any(), Mockito.any());
    List<CAType> arguments = caType.getAllValues();
    Assert.assertEquals(2, arguments.size());

    // All Intermediate CAs have root CA for issuer
    Assert.assertEquals(CAType.ROOT, arguments.get(0));
    Assert.assertEquals(CAType.ROOT, arguments.get(1));

    X509Certificate caCert = pki.getCaCertificates().get(CAType.ROOT);
    Assert.assertEquals(Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
        caCert.getNotAfter().toInstant().truncatedTo(ChronoUnit.DAYS));
  }
}
