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

package io.hops.hopsworks.ca.apiV2.certificates;

import io.hops.hopsworks.ca.api.certs.NoCacheResponse;
import io.hops.hopsworks.common.security.CAException;
import io.hops.hopsworks.common.security.OpensslOperations;
import io.hops.hopsworks.common.security.PKI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.javatuples.Pair;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static io.hops.hopsworks.common.security.CAException.CAExceptionErrors.BADREVOKATIONREQUEST;
import static io.hops.hopsworks.common.security.CAException.CAExceptionErrors.BADSIGNREQUEST;
import static io.hops.hopsworks.common.security.CertificateType.APP;

@Stateless
@RolesAllowed({"AGENT"})
@Api(value = "App certificate service", description = "Manage App certificates")
public class AppCertsResource {

  @EJB
  private OpensslOperations opensslOperations;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private PKI pki;

  @ApiOperation(value = "Sign App certificate with IntermediateHopsCA", response = CSRView.class)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response signCSR(CSRView csrView) throws IOException, CAException {
    if (csrView == null || csrView.getCsr() == null || csrView.getCsr().isEmpty()) {
      throw new CAException(BADSIGNREQUEST, APP);
    }

    String signedCert = opensslOperations.signCertificateRequest(csrView.getCsr(), APP);

    Pair<String, String> chainOfTrust = pki.getChainOfTrust(pki.getResponsibileCA(APP));
    CSRView signedCsr = new CSRView(signedCert, chainOfTrust.getValue0(), chainOfTrust.getValue1());
    GenericEntity<CSRView> csrViewGenericEntity = new GenericEntity<CSRView>(signedCsr) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(csrViewGenericEntity).build();
  }

  @ApiOperation(value = "Revoke App certificate")
  @DELETE
  public Response revokeCertificate(
      @ApiParam(value = "Identifier of the Certificate to revoke", required = true) @QueryParam("certId") String certId)
      throws CAException, IOException {

    if (certId == null || certId.isEmpty()) {
      throw new CAException(BADREVOKATIONREQUEST, APP);
    }

    opensslOperations.revokeCertificate(certId, APP, true, true);
    return Response.ok().build();
  }
}
