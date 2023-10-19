/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

import io.hops.hopsworks.persistence.entity.pki.CAType;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static io.hops.hopsworks.ca.controllers.PKI.EMPTY_CONFIGURATION;

public class TestCreateKeystores extends PKIMocking {

  @Test
  public void testCreateKeystores() throws Exception {
    setupBasicPKI();
    pkiUtils.init();
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    pki.initializeCertificateAuthorities();
    Mockito.doNothing().when(pki).maybeInitializeCA();
    Mockito.doNothing().when(pkiCertificateFacade).saveCertificate(Mockito.any());
    PKIUtils mockPKIUtils = Mockito.mock(PKIUtils.class);
    Mockito.when(mockPKIUtils.getValidityPeriod(Mockito.eq(CertificateType.APP)))
        .thenReturn(Duration.ofMinutes(10));
    Mockito.when(mockPKIUtils.getResponsibleCA(Mockito.any())).thenCallRealMethod();
    pki.setPKIUtils(mockPKIUtils);

    KeyPair requesterKeypair = pki.generateKeyPair();

    X500Name requesterName = new X500Name("CN=owner_1,L=hdfs");
    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        requesterKeypair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(
        new JcaContentSignerBuilder("SHA256withRSA").build(requesterKeypair.getPrivate()));
    String stringifiedCSR = stringifyCSR(csr);
    X509Certificate certificate = pki.signCertificateSigningRequest(stringifiedCSR, CertificateType.APP);
    Assert.assertNotNull(certificate);

    X509Certificate rootCA = pki.getCaCertificates().get(CAType.ROOT);
    Assert.assertNotNull(rootCA);
    X509Certificate intermediateCA = pki.getCaCertificates().get(CAType.INTERMEDIATE);
    Assert.assertNotNull(intermediateCA);

    String privateKeyStr = stringifyPrivateKey(requesterKeypair.getPrivate());
    PKIUtils.KeyStores<String> keyStores = pkiUtils.createB64Keystores(privateKeyStr, certificate, intermediateCA,
        rootCA);

    // Make sure we have indeed gotten something
    Assert.assertNotNull(keyStores.getKeyStore());
    Assert.assertNotNull(keyStores.getTrustStore());
    Assert.assertNotNull(keyStores.getPassword());

    Base64 b64 = new Base64();
    ByteArrayInputStream kbis = new ByteArrayInputStream(b64.decode(keyStores.getKeyStore()));
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(kbis, keyStores.getPassword());

    ByteArrayInputStream tbis = new ByteArrayInputStream(b64.decode(keyStores.getTrustStore()));
    KeyStore ts = KeyStore.getInstance("JKS");
    ts.load(tbis, keyStores.getPassword());

    // Inspect keystore
    Assert.assertTrue(ks.containsAlias("own"));
    // Signature of my certificate - first in the bundle
    X509Certificate myCert = (X509Certificate) ks.getCertificate("own");
    Assert.assertArrayEquals(certificate.getSignature(), myCert.getSignature());
    // Signature of the intermediate - second in the bundle
    Certificate[] bundle = ks.getCertificateChain("own");
    Assert.assertEquals(2, bundle.length);
    X509Certificate kInterCA = (X509Certificate) bundle[1];
    Assert.assertArrayEquals(intermediateCA.getSignature(), kInterCA.getSignature());

    // Inspect truststore
    // Signature of Root
    Assert.assertTrue(ts.containsAlias("hw_root_ca"));
    X509Certificate tRootCA = (X509Certificate) ts.getCertificate("hw_root_ca");
    Assert.assertArrayEquals(rootCA.getSignature(), tRootCA.getSignature());
  }

  private String stringifyPrivateKey(PrivateKey key) throws IOException  {
    try (StringWriter sw = new StringWriter()) {
      PemWriter pw = new JcaPEMWriter(sw);
      PemObjectGenerator pog = new JcaMiscPEMGenerator(key);
      pw.writeObject(pog.generate());
      pw.flush();
      pw.close();
      return sw.toString();
    }
  }
}
