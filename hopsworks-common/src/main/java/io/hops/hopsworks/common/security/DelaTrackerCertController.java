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

package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCert;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCertFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import static io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode.AGENTIDNOTFOUND;
import static io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode.BADREQUEST;
import static io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode.CN;
import static io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode.CNNOTFOUND;
import static io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode.EMAIL;
import static io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode.NOTFOUND;
import static io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode.O;
import static io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode.OU;
import static io.hops.hopsworks.common.exception.RESTCodes.DelaCSRErrorCode.SERIALNUMBER;


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
      throw new DelaCSRCheckException(BADREQUEST, Level.FINE);
    }

    //subject=/C=se/CN=bbc.sics.se/ST=stockholm/L=kista/O=hopsworks/OU=hs/emailAddress=dela1@kth.se
    String subject = opensslOperations.getSubjectFromCSR(csr);
    HashMap<String, String> keyVal = pki.getKeyValuesFromSubject(subject);
    String email = keyVal.get("emailAddress");
    String commonName = keyVal.get("CN");
    String organizationName = keyVal.get("O");
    String organizationalUnitName = keyVal.get("OU");
    if (email == null || email.isEmpty() || !email.equals(user.getEmail())) {
      throw new DelaCSRCheckException(EMAIL, Level.FINE);
    }
    if (commonName == null || commonName.isEmpty()) {
      throw new DelaCSRCheckException(CN, Level.FINE);
    }
    if (organizationName == null || organizationName.isEmpty()) {
      throw new DelaCSRCheckException(O, Level.FINE);
    }
    if (organizationalUnitName == null || organizationalUnitName.isEmpty()) {
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

  private String getSerialNumFromCert(String pubAgentCert) throws IOException {
    String serialNum = opensslOperations.getSerialNumberFromCert(pubAgentCert);
    String[] parts = serialNum.split("=");
    if (parts.length < 2) {
      throw new IOException("Failed to get serial number from cert.");
    }
    return parts[1];
  }
}

