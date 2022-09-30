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

import io.hops.hopsworks.ca.persistence.PKICertificateFacade;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.persistence.entity.pki.PKICertificate;
import io.hops.hopsworks.persistence.entity.pki.PKICertificateId;
import org.apache.commons.lang3.SerializationUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.security.cert.CRLReason;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static io.hops.hopsworks.ca.controllers.PKI.EMPTY_CONFIGURATION;

public class TestCertificateRevocation extends PKIMocking {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private final ArgumentCaptor<PKICertificate> pkiCertificateCaptor = ArgumentCaptor.forClass(PKICertificate.class);

  @Test
  public void testUpdateRevokedCertificate() {
    PKI realPKI = new PKI();
    PKI pki = Mockito.spy(realPKI);

    PKICertificate toRevoke = new PKICertificate(
        new PKICertificateId(PKICertificate.Status.VALID, "CN=hello"),
        CAType.INTERMEDIATE,
        1L,
        new byte[]{},
        Date.from(Instant.now()),
        Date.from(Instant.now()));
    PKICertificate toRevokeClone = SerializationUtils.clone(toRevoke);

    PKICertificateFacade pkiCertificateFacade = Mockito.mock(PKICertificateFacade.class);
    Mockito.when(pkiCertificateFacade.findById(Mockito.any())).thenReturn(Optional.of(toRevokeClone));
    Mockito.doNothing()
            .when(pkiCertificateFacade).updateCertificate(pkiCertificateCaptor.capture());
    Mockito.doNothing()
            .when(pkiCertificateFacade).deleteCertificate(pkiCertificateCaptor.capture());

    pki.setPkiCertificateFacade(pkiCertificateFacade);

    pki.updateRevokedCertificate(toRevoke);
    PKICertificate revokedUpdated = pkiCertificateCaptor.getAllValues().get(0);
    Assert.assertEquals(PKICertificate.Status.REVOKED, revokedUpdated.getCertificateId().getStatus());
    Assert.assertNull(revokedUpdated.getCertificate());
    Assert.assertEquals(toRevoke.getCertificateId().getSubject(), revokedUpdated.getCertificateId().getSubject());

    PKICertificate revokedDeleted = pkiCertificateCaptor.getAllValues().get(1);
    Assert.assertEquals(toRevoke.getCertificateId().getStatus(), revokedDeleted.getCertificateId().getStatus());
    Assert.assertEquals(toRevoke.getCertificateId().getSubject(), revokedDeleted.getCertificateId().getSubject());
  }

  private final ArgumentCaptor<X509CRL> crlCaptor = ArgumentCaptor.forClass(X509CRL.class);

  @Test
  public void testAddRevocationToCRL() throws Exception {
    setupBasicPKI();
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    pki.initializeCertificateAuthorities();
    Mockito.doNothing().when(pki).maybeInitializeCA();
    Mockito.doNothing().when(pki).initCRL(Mockito.eq(CAType.ROOT), Mockito.any());
    Mockito.verify(pki).initCRL(Mockito.eq(CAType.ROOT), crlCaptor.capture());
    Mockito.doNothing().when(pki).initCRL(Mockito.any(), Mockito.any());
    Mockito.doReturn(crlCaptor.getValue()).when(pki).loadCRL(Mockito.eq(CAType.ROOT));

    X509Certificate intermediateCertificate = pki.getCaCertificates().get(CAType.INTERMEDIATE);
    X509CRL updatedCRL = pki.addRevocationToCRL(CAType.ROOT, intermediateCertificate);

    Assert.assertNull(crlCaptor.getValue().getRevokedCertificates());
    Assert.assertEquals(1, updatedCRL.getRevokedCertificates().size());
    X509CRLEntry crlEntry = updatedCRL.getRevokedCertificate(intermediateCertificate);
    Assert.assertNotNull(crlEntry);
    Assert.assertEquals(CRLReason.PRIVILEGE_WITHDRAWN.toString(), crlEntry.getRevocationReason().toString());
  }

  @Test
  public void testRevokeCertificateDoesNotExist() throws Exception {
    setupBasicPKI();
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    pki.initializeCertificateAuthorities();
    Mockito.doNothing().when(pki).maybeInitializeCA();
    Mockito.doNothing().when(pki).initCRL(Mockito.any(), Mockito.any());
    Mockito.when(pkiCertificateFacade.findBySubjectAndStatus(Mockito.any(), Mockito.any()))
        .thenReturn(Optional.empty());


    thrown.expect(CertificateNotFoundException.class);
    pki.revokeCertificate(new X500Name("CN=test"), CertificateType.APP);
  }

  @Test
  public void testRevokeCertificate() throws Exception {
    setupBasicPKI();
    Mockito.doReturn(EMPTY_CONFIGURATION).when(pki).loadConfiguration();
    pki.init();
    Mockito.doNothing().when(pki).maybeInitializeCA();
    Mockito.doNothing().when(pki).initCRL(Mockito.any(), Mockito.any());
    Mockito.doNothing().when(pkiCertificateFacade).saveCertificate(Mockito.any());
    pki.initializeCertificateAuthorities();

    PKIUtils mockPKIUtils = Mockito.mock(PKIUtils.class);
    Mockito.when(mockPKIUtils.getValidityPeriod(Mockito.eq(CertificateType.HOST)))
        .thenReturn(Duration.ofMinutes(10));
    Mockito.when(mockPKIUtils.getResponsibleCA(Mockito.any())).thenCallRealMethod();
    pki.setPKIUtils(mockPKIUtils);

    KeyPair requesterKeypair = pki.generateKeyPair();

    X500Name requesterName = new X500Name("CN=owner_1");
    JcaPKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(requesterName,
        requesterKeypair.getPublic());
    PKCS10CertificationRequest csr = csrBuilder.build(
        new JcaContentSignerBuilder("SHA256withRSA").build(requesterKeypair.getPrivate()));
    String stringifiedCSR = stringifyCSR(csr);

    pki.signCertificateSigningRequest(stringifiedCSR, CertificateType.HOST);
    Mockito.verify(pkiCertificateFacade, Mockito.atLeastOnce()).saveCertificate(pkiCertificateCaptor.capture());
    // First 3 captures come from PKI initialization
    PKICertificate pkiCertificate = pkiCertificateCaptor.getAllValues().get(3);
    Assert.assertNotNull(pkiCertificate);
    Mockito.when(pkiCertificateFacade.findById(Mockito.any())).thenReturn(Optional.of(pkiCertificate));

    Mockito.doReturn(null).when(pki).addRevocationToCRL(Mockito.any(), Mockito.any());
    Mockito.doNothing().when(pki).updateCRL(Mockito.any(), Mockito.any());
    Mockito.doNothing().when(pki).updateRevokedCertificate(Mockito.any());

    pki.revokeCertificate(new X500Name(pkiCertificate.getCertificateId().getSubject()), CertificateType.HOST);
    Mockito.verify(pki).addRevocationToCRL(Mockito.any(), Mockito.any());
  }

  @Test
  public void testRevokeCertificateSkipCRL() throws Exception {
    setupBasicPKI();
    Mockito.doNothing().when(pki).maybeInitializeCA();
    Mockito.when(pkiCertificateFacade.findById(Mockito.any())).thenReturn(Optional.of(new PKICertificate()));

    JcaX509CertificateConverter mockConverter = Mockito.mock(JcaX509CertificateConverter.class);
    Mockito.when(mockConverter.getCertificate(Mockito.any())).thenReturn(null);

    pki.setConverter(mockConverter);
    Mockito.doReturn(null).when(pki).parseToX509CertificateHolder(Mockito.any());
    Mockito.doReturn(true).when(pki).shouldCertificateTypeSkipCRL(Mockito.any());
    Mockito.doNothing().when(pki).updateRevokedCertificate(Mockito.any());

    pki.revokeCertificate(new X500Name("CN=hello"), CertificateType.APP);
    Mockito.verify(pki, Mockito.never()).addRevocationToCRL(Mockito.any(), Mockito.any());
    Mockito.verify(pki).updateRevokedCertificate(Mockito.any());
  }

  @Test
  public void testCertificateTypeSkipCRL() {
    PKI pki = new PKI();
    for (CertificateType t : CertificateType.values()) {
      boolean expected = false;
      if (t.equals(CertificateType.APP)) {
        expected = true;
      }
      Assert.assertEquals(expected, pki.shouldCertificateTypeSkipCRL(t));
    }
  }
}