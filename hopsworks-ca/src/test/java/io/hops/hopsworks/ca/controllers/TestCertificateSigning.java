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
import io.hops.hopsworks.ca.configuration.CAsConfiguration;
import io.hops.hopsworks.ca.configuration.IntermediateCAConfiguration;
import io.hops.hopsworks.ca.configuration.KubeCAConfiguration;
import io.hops.hopsworks.ca.configuration.SubjectAlternativeName;
import io.hops.hopsworks.ca.configuration.UsernamesConfiguration;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.persistence.entity.pki.PKICertificate;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static io.hops.hopsworks.ca.controllers.PKI.EMPTY_CONFIGURATION;

public class TestCertificateSigning extends PKIMocking {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testSigning() throws Exception {
    setupBasicPKI();
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

    Optional<String> cn = pki.parseX509CommonName(csr);
    Assert.assertTrue(cn.isPresent());
    Assert.assertEquals("owner_1", cn.get());

    Optional<String> l = pki.parseX509Locality(csr);
    Assert.assertTrue(l.isPresent());
    Assert.assertEquals("hdfs", l.get());

    X509Certificate certificate = pki.signCertificateSigningRequest(stringifiedCSR, CertificateType.APP);
    Assert.assertEquals(csr.getSubject().toString(), certificate.getSubjectDN().toString());
    Assert.assertEquals(pki.getCaCertificates().get(CAType.INTERMEDIATE).getSubjectDN().toString(),
        certificate.getIssuerDN().toString());
    certificate.checkValidity();
    Assert.assertEquals(-1, certificate.getBasicConstraints());
    Assert.assertEquals(pki.getCaCertificates().get(CAType.INTERMEDIATE).getSubjectDN().toString(),
        certificate.getIssuerDN().toString());
    Set<String> nonCriticalExtensions = certificate.getNonCriticalExtensionOIDs();
    Assert.assertEquals(2, nonCriticalExtensions.size());
    Assert.assertTrue(nonCriticalExtensions.contains(Extension.subjectKeyIdentifier.toString()));
    Assert.assertTrue(nonCriticalExtensions.contains(Extension.authorityKeyIdentifier.toString()));

    // Called 3 times during initialization and one more for the CSR
    Mockito.verify(pkiCertificateFacade, Mockito.times(4)).saveCertificate(Mockito.any());
  }

  @Test
  public void testSigningAppCertificateExists() throws Exception {
    setupBasicPKI();
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

    X500Name requesterName = new X500Name("CN=owner_1");
    // Mock the certificate already exists
    Mockito.when(pkiCertificateFacade.findBySubjectAndStatus(Mockito.eq(requesterName.toString()), Mockito.any()))
        .thenReturn(Optional.of(new PKICertificate()));

    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        requesterKeypair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(
        new JcaContentSignerBuilder("SHA256withRSA").build(requesterKeypair.getPrivate()));
    String stringifiedCSR = stringifyCSR(csr);


    thrown.expect(CertificateAlreadyExistsException.class);
    pki.signCertificateSigningRequest(stringifiedCSR, CertificateType.APP);
  }

  @Test
  public void testSigningHostCloudCertificateExists() throws Exception {
    setupBasicPKI();
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    pki.initializeCertificateAuthorities();
    Mockito.doNothing().when(pki).maybeInitializeCA();
    Mockito.doNothing().when(pkiCertificateFacade).saveCertificate(Mockito.any());
    PKIUtils mockPKIUtils = Mockito.mock(PKIUtils.class);
    Mockito.when(mockPKIUtils.getValidityPeriod(Mockito.eq(CertificateType.HOST)))
        .thenReturn(Duration.ofMinutes(10));
    Mockito.when(mockPKIUtils.getResponsibleCA(Mockito.any())).thenCallRealMethod();
    pki.setPKIUtils(mockPKIUtils);

    KeyPair requesterKeypair = pki.generateKeyPair();

    X500Name requesterName = new X500Name("CN=host_1");
    // Mock the certificate already exists
    Mockito.when(pkiCertificateFacade.findBySubjectAndStatus(Mockito.eq(requesterName.toString()), Mockito.any()))
        .thenReturn(Optional.of(new PKICertificate()));
    CAConf caConf = Mockito.mock(CAConf.class);
    Mockito.when(caConf.getString(Mockito.eq(CAConf.CAConfKeys.CLOUD_EVENTS_ENDPOINT)))
        .thenReturn("not_empty");
    pki.setCaConf(caConf);
    Mockito.doNothing().when(pki).revokeCertificate(Mockito.any(X500Name.class), Mockito.any());

    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        requesterKeypair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(
        new JcaContentSignerBuilder("SHA256withRSA").build(requesterKeypair.getPrivate()));
    String stringifiedCSR = stringifyCSR(csr);

    X509Certificate certificate = pki.signCertificateSigningRequest(stringifiedCSR, CertificateType.HOST);
    Assert.assertNotNull(certificate);
  }

  @Test
  public void testCertificateExtensionsBuilderCalled() throws Exception {
    setupBasicPKI();
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    pki.initializeCertificateAuthorities();
    Mockito.doNothing().when(pki).maybeInitializeCA();

    PKIUtils mockPKIUtils = Mockito.mock(PKIUtils.class);
    Mockito.when(mockPKIUtils.getValidityPeriod(Mockito.eq(CertificateType.APP)))
        .thenReturn(Duration.ofMinutes(10));
    Mockito.when(mockPKIUtils.getResponsibleCA(Mockito.any())).thenCallRealMethod();
    pki.setPKIUtils(mockPKIUtils);

    KeyPair requesterKeypair = pki.generateKeyPair();

    X500Name requesterName = new X500Name("CN=app_1");
    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        requesterKeypair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(
        new JcaContentSignerBuilder("SHA256withRSA").build(requesterKeypair.getPrivate()));
    String stringifiedCSR = stringifyCSR(csr);

    // We don't care about multi-threaded access but the variable must be final
    // to be used inside the lambda
    final AtomicBoolean check = new AtomicBoolean(false);
    Function<PKI.ExtensionsBuilderParameter, Void> extensionsBuilder = (b) -> {
      check.set(true);
      return null;
    };
    Function<PKI.ExtensionsBuilderParameter, Void>[] extensionsBuilders = new Function[]{ extensionsBuilder };
    pki.signCertificateSigningRequest(stringifiedCSR, CertificateType.APP, CAType.INTERMEDIATE, extensionsBuilders);
    Assert.assertTrue(check.get());
  }

  @Test
  public void testEmptyCertificateExtensionsBuilder() throws Exception {
    KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("RSA");
    KeyPair keyPair = keypairGen.generateKeyPair();
    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        new X500Name("CN=hello"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        new X500Name("CN=hello"),
        keyPair.getPublic()
    );
    PKI.EMPTY_CERTIFICATE_EXTENSIONS_BUILDER.apply(PKI.ExtensionsBuilderParameter.of(builder));
    ContentSigner signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM)
        .build(keyPair.getPrivate());
    X509CertificateHolder holder = builder.build(signer);
    Assert.assertNull(holder.getExtensions());
  }

  @Test
  public void testKubernetesCertificateExtensionsBuilderNoSAN() throws Exception {
    CAsConfiguration casConf = new CAsConfiguration(null, null, null);
    Gson gson = new Gson();
    String jsonConf = gson.toJson(casConf);

    CAConf caConf = Mockito.mock(CAConf.class);
    Mockito.when(caConf.getString(Mockito.any())).thenReturn(jsonConf);

    PKI pki = new PKI();
    pki.setCaConf(caConf);
    pki.init();

    KeyPair keyPair = pki.generateKeyPair();
    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        new X500Name("CN=hello"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        new X500Name("CN=hello"),
        keyPair.getPublic()
    );

    pki.KUBE_CERTIFICATE_EXTENSIONS_BUILDER.apply(PKI.ExtensionsBuilderParameter.of(builder));
    ContentSigner signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM)
        .build(keyPair.getPrivate());
    X509CertificateHolder holder = builder.build(signer);
    Assert.assertEquals(0, holder.getCriticalExtensionOIDs().size());
    Assert.assertEquals(0, holder.getNonCriticalExtensionOIDs().size());
  }

  @Test
  public void testKubernetesCertificateExtensionsBuilderDNSSAN() throws Exception {
    Gson gson = new Gson();
    SubjectAlternativeName san = new SubjectAlternativeName(Arrays.asList("0.dns.name", "1.dns.name"), null);
    CAsConfiguration casConf = new CAsConfiguration(null, null,
        new KubeCAConfiguration(null, null, san));
    String jsonConf = gson.toJson(casConf);

    CAConf caConf = Mockito.mock(CAConf.class);
    Mockito.when(caConf.getString(Mockito.any())).thenReturn(jsonConf);

    PKI pki = new PKI();
    pki.setCaConf(caConf);
    pki.init();
    KeyPair keyPair = pki.generateKeyPair();

    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        new X500Name("CN=hello"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        new X500Name("CN=hello"),
        keyPair.getPublic()
    );

    pki.KUBE_CERTIFICATE_EXTENSIONS_BUILDER.apply(PKI.ExtensionsBuilderParameter.of(builder));
    ContentSigner signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM)
        .build(keyPair.getPrivate());
    X509CertificateHolder holder = builder.build(signer);
    Assert.assertEquals(1, holder.getNonCriticalExtensionOIDs().size());
    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    X509Certificate cert = converter.getCertificate(holder);
    Collection<List<?>> sans = cert.getSubjectAlternativeNames();
    Assert.assertEquals(2, sans.size());
    Iterator<List<?>> sansIter = sans.iterator();
    List<?> encSan = sansIter.next();
    // 2 is the code for SAN dnsName
    Assert.assertEquals(2, encSan.get(0));
    Assert.assertEquals("0.dns.name", encSan.get(1));

    encSan = sansIter.next();
    Assert.assertEquals(2, encSan.get(0));
    Assert.assertEquals("1.dns.name", encSan.get(1));
  }

  @Test
  public void testKubernetesCertificateExtensionsBuilderIPSAN() throws Exception {
    Gson gson = new Gson();
    SubjectAlternativeName san = new SubjectAlternativeName(null, Arrays.asList("10.0.0.1", "10.0.0.2"));
    CAsConfiguration casConf = new CAsConfiguration(null, null,
        new KubeCAConfiguration(null, null, san));
    String jsonConf = gson.toJson(casConf);

    CAConf caConf = Mockito.mock(CAConf.class);
    Mockito.when(caConf.getString(Mockito.any())).thenReturn(jsonConf);

    PKI pki = new PKI();
    pki.setCaConf(caConf);
    pki.init();
    KeyPair keyPair = pki.generateKeyPair();

    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        new X500Name("CN=hello"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        new X500Name("CN=hello"),
        keyPair.getPublic()
    );

    pki.KUBE_CERTIFICATE_EXTENSIONS_BUILDER.apply(PKI.ExtensionsBuilderParameter.of(builder));
    ContentSigner signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM)
        .build(keyPair.getPrivate());
    X509CertificateHolder holder = builder.build(signer);
    Assert.assertEquals(1, holder.getNonCriticalExtensionOIDs().size());
    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    X509Certificate cert = converter.getCertificate(holder);
    Collection<List<?>> sans = cert.getSubjectAlternativeNames();
    Assert.assertEquals(2, sans.size());
    Iterator<List<?>> sansIter = sans.iterator();
    List<?> encSan = sansIter.next();
    // 7 is the code for SAN ip address
    Assert.assertEquals(7, encSan.get(0));
    Assert.assertEquals("10.0.0.1", encSan.get(1));

    encSan = sansIter.next();
    Assert.assertEquals(7, encSan.get(0));
    Assert.assertEquals("10.0.0.2", encSan.get(1));
  }

  @Test
  public void testKubernetesCertificateExtensionsBuilderIPAndDNSSAN() throws Exception {
    Gson gson = new Gson();
    SubjectAlternativeName san = new SubjectAlternativeName(Arrays.asList("0.dns.name"),
        Arrays.asList("10.0.0.1", "10.0.0.2"));
    CAsConfiguration casConf = new CAsConfiguration(null, null,
        new KubeCAConfiguration(null, null, san));
    String jsonConf = gson.toJson(casConf);

    CAConf caConf = Mockito.mock(CAConf.class);
    Mockito.when(caConf.getString(Mockito.any())).thenReturn(jsonConf);

    PKI pki = new PKI();
    pki.setCaConf(caConf);
    pki.init();
    KeyPair keyPair = pki.generateKeyPair();

    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        new X500Name("CN=hello"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        new X500Name("CN=hello"),
        keyPair.getPublic()
    );

    pki.KUBE_CERTIFICATE_EXTENSIONS_BUILDER.apply(PKI.ExtensionsBuilderParameter.of(builder));
    ContentSigner signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM)
        .build(keyPair.getPrivate());
    X509CertificateHolder holder = builder.build(signer);
    Assert.assertEquals(1, holder.getNonCriticalExtensionOIDs().size());
    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    X509Certificate cert = converter.getCertificate(holder);
    Collection<List<?>> sans = cert.getSubjectAlternativeNames();
    Assert.assertEquals(3, sans.size());
    Iterator<List<?>> sansIter = sans.iterator();
    List<?> encSan = sansIter.next();
    // 2 is the code for SAN dnsName
    Assert.assertEquals(2, encSan.get(0));
    Assert.assertEquals("0.dns.name", encSan.get(1));

    encSan = sansIter.next();
    // 7 is the code for SAN ip address
    Assert.assertEquals(7, encSan.get(0));
    Assert.assertEquals("10.0.0.1", encSan.get(1));

    encSan = sansIter.next();
    Assert.assertEquals(7, encSan.get(0));
    Assert.assertEquals("10.0.0.2", encSan.get(1));
  }

  @Test
  public void testAppendSubjectAlternativeNames() throws Exception {
    Gson gson = new Gson();
    CAConf caConf = Mockito.mock(CAConf.class);
    Mockito.when(caConf.getString(Mockito.any())).thenReturn(gson.toJson(EMPTY_CONFIGURATION));
    PKI pki = new PKI();
    pki.setCaConf(caConf);
    pki.init();
    KeyPair keyPair = pki.generateKeyPair();

    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        new X500Name("CN=hello"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        new X500Name("CN=hello"),
        keyPair.getPublic()
    );

    GeneralName[] sanToAdd = new GeneralName[] {
        new GeneralName(GeneralName.dNSName, "0.hopsworks.ai"),
        new GeneralName(GeneralName.dNSName, "1.hopsworks.ai")
    };
    pki.appendSubjectAlternativeNames(builder, sanToAdd);

    ContentSigner signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM)
        .build(keyPair.getPrivate());
    X509CertificateHolder holder = builder.build(signer);
    Assert.assertEquals(1, holder.getNonCriticalExtensionOIDs().size());
    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    X509Certificate cert = converter.getCertificate(holder);
    List<String> sans = extractSubjectAlternativeNames(cert);
    Assert.assertEquals(2, sans.size());

    Assert.assertTrue(sans.contains("0.hopsworks.ai"));
    Assert.assertTrue(sans.contains("1.hopsworks.ai"));

    sanToAdd = new GeneralName[] {
        new GeneralName(GeneralName.dNSName, "1.hopsworks.ai"),
        new GeneralName(GeneralName.dNSName, "a.hopsworks.ai")
    };
    pki.appendSubjectAlternativeNames(builder, sanToAdd);
    holder = builder.build(signer);
    cert = converter.getCertificate(holder);
    sans = extractSubjectAlternativeNames(cert);
    Assert.assertEquals(3, sans.size());

    Assert.assertTrue(sans.contains("0.hopsworks.ai"));
    Assert.assertTrue(sans.contains("1.hopsworks.ai"));
    Assert.assertTrue(sans.contains("a.hopsworks.ai"));
  }

  private List<String> extractSubjectAlternativeNames(X509Certificate certificate) throws CertificateParsingException  {
    List<String> sans = new ArrayList<>();
    Iterator<List<?>> sansIter = certificate.getSubjectAlternativeNames().iterator();
    while (sansIter.hasNext()) {
      sans.add((String) sansIter.next().get(1));
    }
    return sans;
  }

  @Test
  public void testSANCertificateExtensionsBuilderDNSSAN() throws Exception {
    Gson gson = new Gson();

    SubjectAlternativeName hdfsExtraSan = new SubjectAlternativeName(Arrays.asList("h0.hopsworks.ai"), null);
    SubjectAlternativeName yarnExtraSan = new SubjectAlternativeName(Arrays.asList("y0.hopsworks.ai", "y1.hopsworks" +
        ".ai"), null);
    Map<String, SubjectAlternativeName> extraSans = new HashMap<>();
    extraSans.put("hdfs", hdfsExtraSan);
    extraSans.put("rmyarn", yarnExtraSan);

    IntermediateCAConfiguration interCaConf = new IntermediateCAConfiguration(null, null, extraSans);

    CAsConfiguration casConf = new CAsConfiguration(null, interCaConf, null);
    String jsonConf = gson.toJson(casConf);

    CAConf caConf = Mockito.mock(CAConf.class);
    Mockito.when(caConf.getString(Mockito.eq(CAConf.CAConfKeys.CA_CONFIGURATION))).thenReturn(jsonConf);
    Mockito.when(caConf.getString(Mockito.eq(CAConf.CAConfKeys.SERVICE_DISCOVERY_DOMAIN))).thenReturn("consul");

    UsernamesConfiguration usernamesConf = Mockito.mock(UsernamesConfiguration.class);
    Mockito.when(usernamesConf.getNormalizedUsername(Mockito.eq("hdfs")))
        .thenReturn(UsernamesConfiguration.Username.HDFS.name().toLowerCase());
    Mockito.when(usernamesConf.getNormalizedUsername(Mockito.eq("rmyarn")))
        .thenReturn(UsernamesConfiguration.Username.RMYARN.name().toLowerCase());
    Mockito.when(usernamesConf.getNormalizedUsername(Mockito.eq("flink")))
        .thenReturn(UsernamesConfiguration.Username.FLINK.name().toLowerCase());

    PKI pki = new PKI();
    pki.setCaConf(caConf);
    pki.setUsernamesConfiguration(usernamesConf);
    pki.init();
    KeyPair keyPair = pki.generateKeyPair();

    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();

    JcaContentSignerBuilder contentSigner = new JcaContentSignerBuilder("SHA256withRSA");
    X500Name requesterName = new X500Name("CN=host1,L=hdfs");
    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        keyPair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(contentSigner.build(keyPair.getPrivate()));

    // hdfs certificate

    X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
        new X500Name("CN=root"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        csr.getSubject(),
        csr.getSubjectPublicKeyInfo());


    pki.SAN_CERTIFICATE_EXTENSIONS_BUILDER.apply(PKI.ExtensionsBuilderParameter.of(builder, csr, CertificateType.HOST));
    ContentSigner signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM)
        .build(keyPair.getPrivate());
    X509CertificateHolder holder = builder.build(signer);
    Assert.assertEquals(1, holder.getNonCriticalExtensionOIDs().size());
    X509Certificate cert = converter.getCertificate(holder);
    Collection<List<?>> sans = cert.getSubjectAlternativeNames();

    Assert.assertEquals(6, sans.size());
    // 2 is the code for SAN dnsName
    Assert.assertTrue(containsSAN(cert, "host1", 2));
    Assert.assertTrue(containsSAN(cert, "namenode.service.consul", 2));
    Assert.assertTrue(containsSAN(cert, "rpc.namenode.service.consul", 2));
    Assert.assertTrue(containsSAN(cert, "http.namenode.service.consul", 2));
    Assert.assertTrue(containsSAN(cert, "sparkhistoryserver.service.consul", 2));
    Assert.assertTrue(containsSAN(cert, "h0.hopsworks.ai", 2));


    // rmyarn certificate

    requesterName = new X500Name("CN=host1,L=rmyarn");
    csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName, keyPair.getPublic());
    csr = csrBuilder.build(contentSigner.build(keyPair.getPrivate()));


    builder = new X509v3CertificateBuilder(
        new X500Name("CN=root"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        csr.getSubject(),
        csr.getSubjectPublicKeyInfo());


    pki.SAN_CERTIFICATE_EXTENSIONS_BUILDER.apply(PKI.ExtensionsBuilderParameter.of(builder, csr, CertificateType.HOST));
    signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM).build(keyPair.getPrivate());
    holder = builder.build(signer);
    Assert.assertEquals(1, holder.getNonCriticalExtensionOIDs().size());
    cert = converter.getCertificate(holder);
    sans = cert.getSubjectAlternativeNames();

    Assert.assertEquals(6, sans.size());
    // 2 is the code for SAN dnsName
    Assert.assertTrue(containsSAN(cert, "host1", 2));
    Assert.assertTrue(containsSAN(cert, "resourcemanager.service.consul", 2));
    Assert.assertTrue(containsSAN(cert, "rpc.resourcemanager.service.consul", 2));
    Assert.assertTrue(containsSAN(cert, "https.resourcemanager.service.consul", 2));
    Assert.assertTrue(containsSAN(cert, "y0.hopsworks.ai", 2));
    Assert.assertTrue(containsSAN(cert, "y1.hopsworks.ai", 2));


    // flink certificate

    requesterName = new X500Name("CN=host2,L=flink");
    csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName, keyPair.getPublic());
    csr = csrBuilder.build(contentSigner.build(keyPair.getPrivate()));


    builder = new X509v3CertificateBuilder(
        new X500Name("CN=root"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        csr.getSubject(),
        csr.getSubjectPublicKeyInfo());


    pki.SAN_CERTIFICATE_EXTENSIONS_BUILDER.apply(PKI.ExtensionsBuilderParameter.of(builder, csr, CertificateType.HOST));
    signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM).build(keyPair.getPrivate());
    holder = builder.build(signer);
    Assert.assertEquals(1, holder.getNonCriticalExtensionOIDs().size());
    cert = converter.getCertificate(holder);
    sans = cert.getSubjectAlternativeNames();

    Assert.assertEquals(3, sans.size());
    // 2 is the code for SAN dnsName
    Assert.assertTrue(containsSAN(cert, "host2", 2));
    Assert.assertTrue(containsSAN(cert, "flink.service.consul", 2));
    Assert.assertTrue(containsSAN(cert, "historyserver.flink.service.consul", 2));


    // noconsul certificate

    requesterName = new X500Name("CN=host2,L=noconsul");
    csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName, keyPair.getPublic());
    csr = csrBuilder.build(contentSigner.build(keyPair.getPrivate()));


    builder = new X509v3CertificateBuilder(
        new X500Name("CN=root"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(10, ChronoUnit.DAYS)),
        csr.getSubject(),
        csr.getSubjectPublicKeyInfo());


    pki.SAN_CERTIFICATE_EXTENSIONS_BUILDER.apply(PKI.ExtensionsBuilderParameter.of(builder, csr, CertificateType.HOST));
    signer = new JcaContentSignerBuilder(PKI.SIGNATURE_ALGORITHM).build(keyPair.getPrivate());
    holder = builder.build(signer);
    Assert.assertEquals(1, holder.getNonCriticalExtensionOIDs().size());
    cert = converter.getCertificate(holder);
    sans = cert.getSubjectAlternativeNames();

    Assert.assertEquals(1, sans.size());
    // 2 is the code for SAN dnsName
    Assert.assertTrue(containsSAN(cert, "host2", 2));
  }

  private boolean containsSAN(X509Certificate certificate, String expectedValue, Integer expectedType) throws Exception {
    Collection<List<?>> sans = certificate.getSubjectAlternativeNames();
    Iterator<List<?>> sansIter = sans.iterator();
    while (sansIter.hasNext()) {
      List<?> encodedSan = sansIter.next();
      String san = (String) encodedSan.get(1);
      if (expectedValue.equals(san)) {
        return ((Integer)encodedSan.get(0)).equals(expectedType);
      }
    }
    return false;
  }
}
