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

package io.hops.hopsworks.ca.apiV2.certificates;

import io.hops.hopsworks.ca.api.certs.NoCacheResponse;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.security.CAException;
import io.hops.hopsworks.common.security.DelaCSRCheckException;
import io.hops.hopsworks.common.security.DelaTrackerCertController;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.logging.Level;

import static io.hops.hopsworks.common.security.CertificateType.DELA;

@Stateless
@RolesAllowed({"CLUSTER_AGENT"})
@Api(value = "Dela Tracker certificate service", description = "Manage Dela tracker certificates")
public class DelaTrackerCertsResource {

  @EJB
  private DelaTrackerCertController delaTrackerCertController;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private PKI pki;
  @EJB
  private OpensslOperations opensslOperations;

  @ApiOperation(value = "Sign Dela certificate with IntermediateHopsCA", response = CSRView.class)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response signCSR(@Context SecurityContext sc, CSRView csrView)
      throws IOException, CAException, DelaCSRCheckException {

    if (csrView == null || csrView.getCsr() == null || csrView.getCsr().isEmpty()) {
      throw new CAException(RESTCodes.CAErrorCode.BADSIGNREQUEST, Level.FINE, DELA);
    }

    String signedCert = delaTrackerCertController.signCsr(sc.getUserPrincipal().getName(), csrView.getCsr());
    Pair<String, String> chainOfTrust = pki.getChainOfTrust(pki.getResponsibileCA(DELA));
    CSRView signedCsr = new CSRView(signedCert, chainOfTrust.getValue0(), chainOfTrust.getValue1());
    GenericEntity<CSRView> csrViewGenericEntity = new GenericEntity<CSRView>(signedCsr) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(csrViewGenericEntity).build();
  }

  @ApiOperation(value = "Revoke Dela certificate")
  @DELETE
  public Response revokeCertificate(
      @ApiParam(value = "Identifier of the certificate to revoke", required = true) @QueryParam("certId") String certId)
      throws IOException, CAException {
    if (certId == null || certId.isEmpty()) {
      throw new CAException(RESTCodes.CAErrorCode.BADREVOKATIONREQUEST, Level.FINE, DELA);
    }

    opensslOperations.revokeCertificate(certId, DELA, true, true);
    return Response.ok().build();
  }
}
