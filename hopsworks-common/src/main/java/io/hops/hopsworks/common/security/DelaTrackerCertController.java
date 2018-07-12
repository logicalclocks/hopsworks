/*
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
 *
 */

package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCert;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCertFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.util.HashMap;

import static io.hops.hopsworks.common.security.DelaCSRCheckException.DelaCSRCheckErrors.AGENTIDNOTFOUND;
import static io.hops.hopsworks.common.security.DelaCSRCheckException.DelaCSRCheckErrors.BADREQUEST;
import static io.hops.hopsworks.common.security.DelaCSRCheckException.DelaCSRCheckErrors.CN;
import static io.hops.hopsworks.common.security.DelaCSRCheckException.DelaCSRCheckErrors.CNNOTFOUND;
import static io.hops.hopsworks.common.security.DelaCSRCheckException.DelaCSRCheckErrors.EMAIL;
import static io.hops.hopsworks.common.security.DelaCSRCheckException.DelaCSRCheckErrors.NOTFOUND;
import static io.hops.hopsworks.common.security.DelaCSRCheckException.DelaCSRCheckErrors.O;
import static io.hops.hopsworks.common.security.DelaCSRCheckException.DelaCSRCheckErrors.OU;
import static io.hops.hopsworks.common.security.DelaCSRCheckException.DelaCSRCheckErrors.SERIALNUMBER;


@Stateless
public class DelaTrackerCertController {

  @EJB
  private ClusterCertFacade clusterCertFacade;
  @EJB
  private OpensslOperations opensslOperations;
  @EJB
  private UserFacade userFacade;
  @EJB
  private PKI pki;

  public String signCsr(String userEmail, String csr) throws IOException, DelaCSRCheckException {
    // TODO this is really dangerous to do within a single transaction.
    ClusterCert clusterCert = checkCSR(userEmail, csr);
    String signedCsr = opensslOperations.signCertificateRequest(csr, CertificateType.DELA);
    clusterCert.setSerialNumber(getSerialNumFromCert(signedCsr));
    clusterCertFacade.update(clusterCert);

    return signedCsr;
  }

  private ClusterCert checkCSR(String userEmail, String csr) throws IOException, DelaCSRCheckException{
    Users user = userFacade.findByEmail(userEmail);
    if (user == null || user.getEmail() == null || csr == null || csr.isEmpty()) {
      throw new DelaCSRCheckException(BADREQUEST);
    }

    //subject=/C=se/CN=bbc.sics.se/ST=stockholm/L=kista/O=hopsworks/OU=hs/emailAddress=dela1@kth.se
    String subject = opensslOperations.getSubjectFromCSR(csr);
    HashMap<String, String> keyVal = pki.getKeyValuesFromSubject(subject);
    String email = keyVal.get("emailAddress");
    String commonName = keyVal.get("CN");
    String organizationName = keyVal.get("O");
    String organizationalUnitName = keyVal.get("OU");
    if (email == null || email.isEmpty() || !email.equals(user.getEmail())) {
      throw new DelaCSRCheckException(EMAIL);
    }
    if (commonName == null || commonName.isEmpty()) {
      throw new DelaCSRCheckException(CN);
    }
    if (organizationName == null || organizationName.isEmpty()) {
      throw new DelaCSRCheckException(O);
    }
    if (organizationalUnitName == null || organizationalUnitName.isEmpty()) {
      throw new DelaCSRCheckException(OU);
    }

    ClusterCert clusterCert = clusterCertFacade.getByOrgUnitNameAndOrgName(organizationName, organizationalUnitName);
    if (clusterCert == null) {
      throw new DelaCSRCheckException(NOTFOUND);
    }
    if (clusterCert.getSerialNumber() != null && !clusterCert.getSerialNumber().isEmpty()) {
      throw new DelaCSRCheckException(SERIALNUMBER);
    }
    if (!clusterCert.getCommonName().equals(commonName)) {
      throw new DelaCSRCheckException(CNNOTFOUND);
    }
    if (!clusterCert.getAgentId().equals(user)) {
      throw new DelaCSRCheckException(AGENTIDNOTFOUND);
    }
    return clusterCert;
  }

  private String getSerialNumFromCert(String pubAgentCert) throws IOException {
    String serialNum = opensslOperations.getSerialNumberFromCert(pubAgentCert);
    String[] parts = serialNum.split("=");
    if (parts.length < 2) {
      throw new IOException("Failed to get serial number from cert.");
    }
    return parts[1];
  }
}

