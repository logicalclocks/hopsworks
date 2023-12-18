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

package io.hops.hopsworks.ca.api.certificates;

import com.google.common.base.Strings;
import io.hops.hopsworks.ca.api.filter.Audience;
import io.hops.hopsworks.ca.api.filter.NoCacheResponse;
import io.hops.hopsworks.ca.controllers.CAException;
import io.hops.hopsworks.ca.controllers.CAInitializationException;
import io.hops.hopsworks.ca.controllers.CertificateNotFoundException;
import io.hops.hopsworks.ca.controllers.PKI;
import io.hops.hopsworks.ca.controllers.PKIUtils;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.OperatorCreationException;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.naming.InvalidNameException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static io.hops.hopsworks.ca.controllers.CertificateType.KUBE;

@RequestScoped
@Api(value = "Kubernetes certificate service", description = "Manage Kubernetes certificates")
public class KubeCertsResource{

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private PKIUtils pkiUtils;
  @EJB
  private PKI pki;

  @ApiOperation(value = "Sign Kubernetes certificate with KubeCA", response = CSRView.class)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens={Audience.SERVICES}, allowedUserRoles={"AGENT"})
  public Response signCSR(CSRView csrView) throws CAException {
    if (csrView == null || csrView.getCsr() == null || csrView.getCsr().isEmpty()) {
      throw new IllegalArgumentException("Empty CSR");
    }

    try {
      X509Certificate signedCert = pki.signCertificateSigningRequest(csrView.getCsr(), KUBE, csrView.getRegion());
      String stringifiedCert = pkiUtils.convertToPEM(signedCert);
      Pair<String, String> chainOfTrust = pki.getChainOfTrust(pkiUtils.getResponsibleCA(KUBE));
      CSRView signedCsr = new CSRView(stringifiedCert, chainOfTrust.getLeft(), chainOfTrust.getRight());
      GenericEntity<CSRView> csrViewGenericEntity = new GenericEntity<CSRView>(signedCsr) {
      };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(csrViewGenericEntity).build();
    } catch (IOException | GeneralSecurityException | OperatorCreationException | CAInitializationException ex) {
      throw pkiUtils.csrSigningExceptionConvertToCAException(ex, KUBE);
    }
  }

  @ApiOperation(value = "Revoke KubeCA certificates")
  @DELETE
  @JWTRequired(acceptedTokens={Audience.SERVICES}, allowedUserRoles={"AGENT"})
  public Response revokeCertificate(
      @ApiParam(value = "Identifier of the Certificate to revoke", required = true)
      @QueryParam("certId") String certId,
      @ApiParam(value = "Flag whether certId is a full RFC4514 Distinguished Name string")
      @QueryParam("exact") Boolean exact) throws CAException {
    if (Strings.isNullOrEmpty(certId)) {
      throw new IllegalArgumentException("Empty certificate identifier");
    }

    final List<X500Name> subjectsToRevoke = new ArrayList<>();
    try {
      if (!Boolean.TRUE.equals(exact)) {
        X500Name certificateName = pkiUtils.parseCertificateSubjectName(certId, KUBE);
        List<String> subjects = pkiUtils.findAllValidSubjectsWithPartialMatch(certificateName.toString());
        subjects.forEach(s -> subjectsToRevoke.add(new X500Name(s)));
      } else {
        subjectsToRevoke.add(new X500Name(certId));
      }
      if (subjectsToRevoke.isEmpty()) {
        throw new CertificateNotFoundException("Could not find a VALID certificate with ID: " + certId + " Is " +
            "exact X509 Name: " + exact);
      }
      for (X500Name n : subjectsToRevoke) {
        pki.revokeCertificate(n, KUBE);
      }
      return Response.ok().build();
    } catch (InvalidNameException | GeneralSecurityException | CAInitializationException ex) {
      throw pkiUtils.certificateRevocationExceptionConvertToCAException(ex, KUBE);
    }
  }

  @ApiOperation(value = "Get KubeCA certificate", response = CSRView.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens={Audience.SERVICES}, allowedUserRoles={"AGENT"})
  public Response getCACert() throws CAException {
    try {
      Pair<String, String> chainOfTrust = pki.getChainOfTrust(pkiUtils.getResponsibleCA(KUBE));
      CSRView csrView = new CSRView(chainOfTrust.getLeft(), chainOfTrust.getRight());
      GenericEntity<CSRView> csrViewGenericEntity = new GenericEntity<CSRView>(csrView) {
      };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(csrViewGenericEntity).build();
    } catch (CAInitializationException | GeneralSecurityException | IOException ex) {
      throw new CAException(RESTCodes.CAErrorCode.CA_INITIALIZATION_ERROR, Level.SEVERE, KUBE,
          "Failed to get chain of trust", "Failed to get chain of trust for KUBE certificates", ex);
    }
  }
}