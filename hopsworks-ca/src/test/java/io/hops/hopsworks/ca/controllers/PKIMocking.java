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

import io.hops.hopsworks.ca.configuration.CAConf;
import io.hops.hopsworks.ca.persistence.CRLFacade;
import io.hops.hopsworks.ca.persistence.KeyFacade;
import io.hops.hopsworks.ca.persistence.PKICertificateFacade;
import io.hops.hopsworks.ca.persistence.SerialNumberFacade;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.persistence.entity.pki.PKICertificate;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PKIMocking {

  protected PKI realPKI;
  protected PKI pki;
  protected SerialNumberFacade serialNumberFacade;
  protected KeyFacade keyFacade;
  protected PKICertificateFacade pkiCertificateFacade;
  protected CRLFacade crlFacade;
  protected PKIUtils pkiUtils;

  protected static final Map<CAType, Long> CA_SERIAL_NUMBERS = new HashMap<>(3);

  protected void setupBasicPKI() throws Exception {
    CA_SERIAL_NUMBERS.put(CAType.ROOT, 0L);
    CA_SERIAL_NUMBERS.put(CAType.INTERMEDIATE, 0L);
    CA_SERIAL_NUMBERS.put(CAType.KUBECA, 0L);
    realPKI = new PKI();
    pki = Mockito.spy(realPKI);

    Mockito.doReturn(false).when(pki).loadFromFile();

    serialNumberFacade = Mockito.mock(SerialNumberFacade.class);
    Mockito.when(serialNumberFacade.nextSerialNumber(Mockito.any()))
        .thenAnswer(new Answer<Long>() {
          @Override
          public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
            CAType caType = (CAType) invocationOnMock.getArgument(0);
            Long current = CA_SERIAL_NUMBERS.get(caType);
            CA_SERIAL_NUMBERS.put(caType, ++current);
            return current;
          }
        });
    Mockito.when(serialNumberFacade.isInitialized(Mockito.any())).thenReturn(true);
    pki.setSerialNumberFacade(serialNumberFacade);

    keyFacade = Mockito.mock(KeyFacade.class);
    Mockito.doNothing().when(keyFacade).saveKey(Mockito.any());
    Mockito.when(keyFacade.getEncodedKey(Mockito.any(), Mockito.any())).thenReturn(null);
    pki.setKeyFacade(keyFacade);

    pkiCertificateFacade = Mockito.mock(PKICertificateFacade.class);
    Mockito.doNothing().when(pkiCertificateFacade).saveCertificate(Mockito.any());
    Mockito.when(pkiCertificateFacade.findBySubjectAndStatus(Mockito.any(), Mockito.eq(PKICertificate.Status.VALID)))
        .thenReturn(Optional.empty());
    pki.setPkiCertificateFacade(pkiCertificateFacade);

    crlFacade = Mockito.mock(CRLFacade.class);
    Mockito.doNothing().when(crlFacade).init(Mockito.any());
    pki.setCRLFacade(crlFacade);

    pkiUtils = new PKIUtils();
    pki.setPKIUtils(pkiUtils);

    CAConf conf = Mockito.mock(CAConf.class);
    Mockito.when(conf.getBoolean(CAConf.CAConfKeys.KUBERNETES)).thenReturn(true);
    Mockito.when(conf.getString(CAConf.CAConfKeys.KUBERNETES_TYPE)).thenReturn("local");
    pki.setCaConf(conf);
  }

  protected String stringifyCSR(PKCS10CertificationRequest csr) throws IOException {
    try (StringWriter sw = new StringWriter()) {
      PemWriter pw = new JcaPEMWriter(sw);
      PemObjectGenerator pog = new JcaMiscPEMGenerator(csr);
      pw.writeObject(pog.generate());
      pw.flush();
      sw.flush();
      pw.close();
      return sw.toString();
    }
  }
}
