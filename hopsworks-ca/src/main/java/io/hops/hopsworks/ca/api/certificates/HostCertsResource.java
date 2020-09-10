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
import io.hops.hopsworks.ca.controllers.OpensslOperations;
import io.hops.hopsworks.ca.controllers.PKI;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.regex.Pattern;

import static io.hops.hopsworks.ca.controllers.CertificateType.HOST;

@RequestScoped
@Api(value = "Host certificate service", description = "Manage host certificates")
public class HostCertsResource {

  private static final String REVOKE_CERTIFICATES_PATTERN = "^%s__.*__[0-9]+.*";

  @EJB
  private OpensslOperations opensslOperations;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private PKI pki;

  public HostCertsResource() { }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Sing Host CSR with IntermediateHopsCA", response = CSRView.class)
  @JWTRequired(acceptedTokens={Audience.SERVICES}, allowedUserRoles={"AGENT"})
  public Response signCSR(CSRView csrView) throws IOException, CAException {
    if (csrView == null || csrView.getCsr() == null || csrView.getCsr().isEmpty()) {
      throw new IllegalArgumentException("Empty CSR");
    }

    String signedCert = opensslOperations.signCertificateRequest(csrView.getCsr(), HOST);
    Pair<String, String> chainOfTrust = pki.getChainOfTrust(pki.getResponsibileCA(HOST));

    CSRView signedCsr = new CSRView(signedCert, chainOfTrust.getValue0(), chainOfTrust.getValue1());
    GenericEntity<CSRView> csrViewGenericEntity = new GenericEntity<CSRView>(signedCsr) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(csrViewGenericEntity).build();
  }

  @DELETE
  @ApiOperation(value = "Revoke Host certificate")
  @JWTRequired(acceptedTokens={Audience.SERVICES}, allowedUserRoles={"AGENT"})
  public Response revokeCertificate(
          @ApiParam(value = "Identifier of the Certificate to revoke", required = true)
          @QueryParam("certId") String certId)
          throws IOException, CAException {
    if (certId == null || certId.isEmpty()) {
      throw new IllegalArgumentException("Empty certificate identifier");
    }

    opensslOperations.revokeCertificate(certId, HOST);
    return Response.ok().build();
  }

  @Path("all")
  @DELETE
  @ApiOperation(value = "Revoke all Host certificates")
  @JWTRequired(acceptedTokens={Audience.SERVICES}, allowedUserRoles={"AGENT"})
  public Response revokeCertificateGlob(
          @ApiParam(value = "Hostname of the node to revoke certificates for", required = true)
          @QueryParam("hostname")
          String hostname) throws IOException, CAException {
    if (Strings.isNullOrEmpty(hostname)) {
      throw new IllegalArgumentException("Empty hostname to revoke");
    }
    opensslOperations.revokeCertificateGlob(Pattern.compile(String.format(REVOKE_CERTIFICATES_PATTERN,
            Pattern.quote(hostname))), HOST);
    return Response.ok().build();
  }
}
