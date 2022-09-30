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

import io.hops.hadoop.shaded.com.google.gson.Gson;
import io.hops.hopsworks.ca.configuration.CAConf;
import io.hops.hopsworks.ca.configuration.CAConfiguration;
import io.hops.hopsworks.ca.configuration.CAsConfiguration;
import io.hops.hopsworks.ca.configuration.KubeCAConfiguration;
import io.hops.hopsworks.ca.configuration.SubjectAlternativeName;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static io.hops.hopsworks.ca.controllers.PKI.EMPTY_CONFIGURATION;

public class TestPKI extends PKIMocking {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testNoOverrideCASubjectName() {
    CAConf caConf = Mockito.mock(CAConf.class);
    Mockito.when(caConf.getString(Mockito.eq(CAConf.CAConfKeys.CA_CONFIGURATION))).thenReturn("");

    PKI pki = new PKI();
    pki.setCaConf(caConf);

    X500Name rootName = pki.getCaSubjectNames().get(CAType.ROOT);
    X500Name intermediateName = pki.getCaSubjectNames().get(CAType.INTERMEDIATE);
    X500Name kubernetesName = pki.getCaSubjectNames().get(CAType.KUBECA);

    pki.init();
    Assert.assertEquals(rootName, pki.getCaSubjectNames().get(CAType.ROOT));
    Assert.assertEquals(intermediateName, pki.getCaSubjectNames().get(CAType.INTERMEDIATE));
    Assert.assertEquals(kubernetesName, pki.getCaSubjectNames().get(CAType.KUBECA));
  }

  @Test
  public void testOverrideCASubjectName() {
    CAConfiguration rootConf = new CAConfiguration("CN=root_name", null);
    CAsConfiguration casConf = new CAsConfiguration(rootConf, null, null);
    Gson gson = new Gson();
    String jsonConf = gson.toJson(casConf);

    CAConf caConf = Mockito.mock(CAConf.class);
    Mockito.when(caConf.getString(Mockito.eq(CAConf.CAConfKeys.CA_CONFIGURATION))).thenReturn(jsonConf);

    PKI pki = new PKI();
    pki.setCaConf(caConf);

    X500Name intermediateName = pki.getCaSubjectNames().get(CAType.INTERMEDIATE);
    pki.init();

    Assert.assertEquals("CN=root_name", pki.getCaSubjectNames().get(CAType.ROOT).toString());
    Assert.assertEquals(intermediateName, pki.getCaSubjectNames().get(CAType.INTERMEDIATE));

    CAConfiguration intermediateConf = new CAConfiguration("CN=inter_name", null);
    KubeCAConfiguration kubernetesConf = new KubeCAConfiguration("CN=kube_name", null,null);
    casConf = new CAsConfiguration(rootConf, intermediateConf, kubernetesConf);
    jsonConf = gson.toJson(casConf);

    Mockito.when(caConf.getString(Mockito.eq(CAConf.CAConfKeys.CA_CONFIGURATION))).thenReturn(jsonConf);

    pki = new PKI();
    pki.setCaConf(caConf);
    pki.init();

    Assert.assertEquals("CN=root_name", pki.getCaSubjectNames().get(CAType.ROOT).toString());
    Assert.assertEquals("CN=inter_name", pki.getCaSubjectNames().get(CAType.INTERMEDIATE).toString());
    Assert.assertEquals("CN=kube_name", pki.getCaSubjectNames().get(CAType.KUBECA).toString());
  }

  @Test
  public void testMaybeInitializeCA() throws Exception {
    PKI realPki = new PKI();
    PKI pki = Mockito.spy(realPki);

    Mockito.doNothing().when(pki).initializeCertificateAuthorities();
    pki.maybeInitializeCA();
    pki.maybeInitializeCA();

    // Although called twice, it must have been initialized once
    Mockito.verify(pki, Mockito.times(1)).initializeCertificateAuthorities();

    realPki = new PKI();
    pki = Mockito.spy(realPki);

    Mockito
        .doThrow(new CertificateException("oops"))
        .when(pki).initializeCertificateAuthorities();

    thrown.expect(CertificateException.class);
    pki.maybeInitializeCA();
  }

  @Test
  public void testInitializeCertificateAuthorities() throws Exception {
    PKI realPKI = new PKI();
    PKI pki = Mockito.spy(realPKI);
    Mockito.doNothing().when(pki).caInitializeSerialNumber(Mockito.any());
    Mockito.doNothing().when(pki).caInitializeKeys(Mockito.any());
    Mockito.doNothing().when(pki).caInitializeCertificate(Mockito.any());
    Mockito.doNothing().when(pki).caInitializeCRL(Mockito.any());

    pki.initializeCertificateAuthorities();

    Mockito.verify(pki, Mockito.times(CAType.values().length)).caInitializeSerialNumber(Mockito.any());
    Mockito.verify(pki, Mockito.times(CAType.values().length)).caInitializeKeys(Mockito.any());
    Mockito.verify(pki, Mockito.times(CAType.values().length)).caInitializeCertificate(Mockito.any());
    Mockito.verify(pki, Mockito.times(CAType.values().length)).caInitializeCRL(Mockito.any());
  }

  @Test
  public void testChainOfTrust() throws Exception {
    setupBasicPKI();
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    pki.initializeCertificateAuthorities();
    Mockito.doNothing().when(pki).maybeInitializeCA();

    String expectedRootPEM = pkiUtils.convertToPEM(pki.getCaCertificates().get(CAType.ROOT));
    String expectedIntermediatePEM = pkiUtils.convertToPEM(pki.getCaCertificates().get(CAType.INTERMEDIATE));
    String expectedKubePEM = pkiUtils.convertToPEM(pki.getCaCertificates().get(CAType.KUBECA));

    Pair<String, String> chain = pki.getChainOfTrust(CAType.ROOT);
    Assert.assertEquals(expectedRootPEM, chain.getLeft());
    Assert.assertNull(chain.getRight());

    chain = pki.getChainOfTrust(CAType.INTERMEDIATE);
    Assert.assertEquals(expectedRootPEM, chain.getLeft());
    Assert.assertEquals(expectedIntermediatePEM, chain.getRight());

    chain = pki.getChainOfTrust(CAType.KUBECA);
    Assert.assertEquals(expectedRootPEM, chain.getLeft());
    Assert.assertEquals(expectedKubePEM, chain.getRight());
  }

  @Test
  public void testCSRValidationPass() throws Exception {
    PKI realPKI = new PKI();
    PKI pki = Mockito.spy(realPKI);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    KeyPair requesterKeypair = pki.generateKeyPair();

    X500Name requesterName = new X500Name("CN=owner_1");
    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        requesterKeypair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(
        new JcaContentSignerBuilder("SHA256withRSA").build(requesterKeypair.getPrivate()));
    pki.validateCertificateSigningRequest(csr, CAType.INTERMEDIATE);
  }

  @Test
  public void testCSRValidationSameRootCAName() throws Exception {
    PKI realPKI = new PKI();
    PKI pki = Mockito.spy(realPKI);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    KeyPair requesterKeypair = pki.generateKeyPair();

    X500Name requesterName = new X500Name(pki.getCaSubjectNames().get(CAType.ROOT).toString());
    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        requesterKeypair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(
        new JcaContentSignerBuilder("SHA256withRSA").build(requesterKeypair.getPrivate()));

    thrown.expect(CertificationRequestValidationException.class);
    pki.validateCertificateSigningRequest(csr, CAType.INTERMEDIATE);
  }

  @Test
  public void testCSRValidationSameIntermediateCAName() throws Exception {
    PKI realPKI = new PKI();
    PKI pki = Mockito.spy(realPKI);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    KeyPair requesterKeypair = pki.generateKeyPair();

    X500Name requesterName = new X500Name(pki.getCaSubjectNames().get(CAType.INTERMEDIATE).toString());
    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        requesterKeypair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(
        new JcaContentSignerBuilder("SHA256withRSA").build(requesterKeypair.getPrivate()));

    thrown.expect(CertificationRequestValidationException.class);
    pki.validateCertificateSigningRequest(csr, CAType.INTERMEDIATE);
  }

  @Test
  public void testCSRValidationSameKubeCAName() throws Exception {
    PKI realPKI = new PKI();
    PKI pki = Mockito.spy(realPKI);
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    KeyPair requesterKeypair = pki.generateKeyPair();

    X500Name requesterName = new X500Name(pki.getCaSubjectNames().get(CAType.KUBECA).toString());
    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        requesterKeypair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(
        new JcaContentSignerBuilder("SHA256withRSA").build(requesterKeypair.getPrivate()));

    thrown.expect(CertificationRequestValidationException.class);
    pki.validateCertificateSigningRequest(csr, CAType.KUBECA);
  }
}
