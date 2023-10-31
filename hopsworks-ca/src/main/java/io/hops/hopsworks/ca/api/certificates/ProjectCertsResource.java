/*
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
 */

package io.hops.hopsworks.ca.api.certificates;

import com.google.common.base.Strings;
import io.hops.hopsworks.ca.api.filter.Audience;
import io.hops.hopsworks.ca.api.filter.NoCacheResponse;
import io.hops.hopsworks.ca.controllers.CAException;
import io.hops.hopsworks.ca.controllers.CAInitializationException;
import io.hops.hopsworks.ca.controllers.PKI;
import io.hops.hopsworks.ca.controllers.PKIUtils;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.operator.OperatorCreationException;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.naming.InvalidNameException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import static io.hops.hopsworks.ca.controllers.CertificateType.PROJECT;

@RequestScoped
@Api(value = "Project certificate service", description = "Manage Project certificates")
public class ProjectCertsResource {

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private PKIUtils pkiUtils;
  @EJB
  private PKI pki;

  @ApiOperation(value = "Sign Project certificate with IntermediateHopsCA", response = CSRView.class)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens={Audience.SERVICES}, allowedUserRoles={"AGENT"})
  public Response signCSR(CSRView csrView) throws IOException, CAException {
    if (csrView == null || Strings.isNullOrEmpty(csrView.getCsr())) {
      throw new IllegalArgumentException("Empty CSR");
    }

    try {
      X509Certificate signedCert = pki.signCertificateSigningRequest(csrView.getCsr(), PROJECT, null);
      String stringifiedCert = pkiUtils.convertToPEM(signedCert);
      Pair<String, String> chainOfTrust = pki.getChainOfTrust(pkiUtils.getResponsibleCA(PROJECT));
      CSRView signedCsr = new CSRView(stringifiedCert, chainOfTrust.getLeft(), chainOfTrust.getRight());
      GenericEntity<CSRView> csrViewGenericEntity = new GenericEntity<CSRView>(signedCsr) { };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(csrViewGenericEntity).build();
    } catch (IOException | GeneralSecurityException | OperatorCreationException | CAInitializationException ex) {
      throw pkiUtils.csrSigningExceptionConvertToCAException(ex, PROJECT);
    }
  }

  @ApiOperation(value = "Revoke Project certificate")
  @DELETE
  @JWTRequired(acceptedTokens={Audience.SERVICES}, allowedUserRoles={"AGENT"})
  public Response revokeCertificate(
      @ApiParam(value = "Identifier of the Certificate to revoke", required = true) @QueryParam("certId") String certId)
    throws IOException, CAException {

    if (Strings.isNullOrEmpty(certId)) {
      throw new IllegalArgumentException("Empty certificate identifier");
    }

    try {
      pki.revokeCertificate(certId, PROJECT);
      return Response.ok().build();
    } catch (InvalidNameException | GeneralSecurityException | CAInitializationException ex) {
      throw pkiUtils.certificateRevocationExceptionConvertToCAException(ex, PROJECT);
    }
  }
}
