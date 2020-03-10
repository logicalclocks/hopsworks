/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.cluster.controller;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCertFacade;
import io.hops.hopsworks.common.security.CSR;
import io.hops.hopsworks.common.security.CertificatesController;
import io.hops.hopsworks.exceptions.DelaCSRCheckException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.cluster.ClusterCert;
import io.hops.hopsworks.restutils.RESTCodes;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.logging.Level;

import static io.hops.hopsworks.restutils.RESTCodes.DelaCSRErrorCode.AGENTIDNOTFOUND;
import static io.hops.hopsworks.restutils.RESTCodes.DelaCSRErrorCode.CN;
import static io.hops.hopsworks.restutils.RESTCodes.DelaCSRErrorCode.CNNOTFOUND;
import static io.hops.hopsworks.restutils.RESTCodes.DelaCSRErrorCode.EMAIL;
import static io.hops.hopsworks.restutils.RESTCodes.DelaCSRErrorCode.NOTFOUND;
import static io.hops.hopsworks.restutils.RESTCodes.DelaCSRErrorCode.O;
import static io.hops.hopsworks.restutils.RESTCodes.DelaCSRErrorCode.OU;
import static io.hops.hopsworks.restutils.RESTCodes.DelaCSRErrorCode.SERIALNUMBER;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaTrackerCertController {

  @EJB
  private ClusterCertFacade clusterCertFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private CertificatesController certificatesController;

  public CSR signCsr(String userEmail, CSR csr)
      throws IOException, HopsSecurityException, GenericException, DelaCSRCheckException {
    ClusterCert clusterCert = checkCSR(userEmail, csr);

    CSR signedCert = certificatesController.signDelaClusterCertificate(csr);
    String certSerialNumber;
    try {
      certSerialNumber = String.valueOf(
          certificatesController.extractSerialNumberFromCert(signedCert.getSignedCert()));
    } catch (CertificateException e) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CREATION_ERROR, Level.WARNING, null, null, e);
    }
    clusterCert.setSerialNumber(certSerialNumber);
    clusterCertFacade.update(clusterCert);

    return signedCert;
  }

  private ClusterCert checkCSR(String userEmail, CSR csrDTO) throws IOException, DelaCSRCheckException {
    Users user = userFacade.findByEmail(userEmail);
    String csr = csrDTO.getCsr();
    if (user == null || user.getEmail() == null || csr == null || csr.isEmpty()) {
      throw new IllegalArgumentException("User or CSR is empty");
    }

    //subject=/C=se/CN=bbc.sics.se/ST=stockholm/L=kista/O=hopsworks/OU=hs/emailAddress=dela1@kth.se
    X500Name subject = certificatesController.extractSubjectFromCSR(csrDTO.getCsr());
    String email = IETFUtils.valueToString(subject.getRDNs(BCStyle.EmailAddress)[0].getFirst().getValue());
    String commonName = IETFUtils.valueToString(subject.getRDNs(BCStyle.CN)[0].getFirst().getValue());
    String organizationName = IETFUtils.valueToString(subject.getRDNs(BCStyle.O)[0].getFirst().getValue());
    String organizationalUnitName = IETFUtils.valueToString(subject.getRDNs(BCStyle.OU)[0].getFirst().getValue());
    if (email.isEmpty() || !email.equals(user.getEmail())) {
      throw new DelaCSRCheckException(EMAIL, Level.FINE);
    }
    if (commonName.isEmpty()) {
      throw new DelaCSRCheckException(CN, Level.FINE);
    }
    if (organizationName.isEmpty()) {
      throw new DelaCSRCheckException(O, Level.FINE);
    }
    if (organizationalUnitName.isEmpty()) {
      throw new DelaCSRCheckException(OU, Level.FINE);
    }

    ClusterCert clusterCert = clusterCertFacade.getByOrgUnitNameAndOrgName(organizationName, organizationalUnitName);
    if (clusterCert == null) {
      throw new DelaCSRCheckException(NOTFOUND, Level.FINE);
    }
    if (clusterCert.getSerialNumber() != null && !clusterCert.getSerialNumber().isEmpty()) {
      throw new DelaCSRCheckException(SERIALNUMBER, Level.FINE);
    }
    if (!clusterCert.getCommonName().equals(commonName)) {
      throw new DelaCSRCheckException(CNNOTFOUND, Level.FINE);
    }
    if (!clusterCert.getAgentId().equals(user)) {
      throw new DelaCSRCheckException(AGENTIDNOTFOUND, Level.FINE);
    }
    return clusterCert;
  }
}

