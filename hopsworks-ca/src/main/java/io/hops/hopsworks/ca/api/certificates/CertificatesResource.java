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

import io.hops.hopsworks.ca.api.filter.Audience;
import io.hops.hopsworks.ca.api.filter.NoCacheResponse;
import io.hops.hopsworks.ca.controllers.CAException;
import io.hops.hopsworks.ca.controllers.PKI;
import io.hops.hopsworks.ca.controllers.PKIUtils;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.pki.PKICertificate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

@Path("/certificate")
@RequestScoped
@Api(value = "Certificate Management", description = "Certificate Management API")
public class CertificatesResource {

  @Inject
  private HostCertsResource hostCertsResource;
  @Inject
  private AppCertsResource appCertsResource;
  @Inject
  private KubeCertsResource kubeCertsResource;
  @Inject
  private ProjectCertsResource projectCertsResource;
  @Inject
  private CRLResource crlResource;
  @EJB
  private PKI pki;
  @EJB
  private PKIUtils pkiUtils;
  @EJB
  private NoCacheResponse noCacheResponse;

  @Path("/host")
  public HostCertsResource getHostCertsResource() {
    return hostCertsResource;
  }

  @Path("/app")
  public AppCertsResource getAppCertsResource() {
    return appCertsResource;
  }

  @Path("/kube")
  public KubeCertsResource getKubeCertsResource() {
    return kubeCertsResource;
  }

  @Path("/project")
  public ProjectCertsResource getProjectCertsResource() {
    return projectCertsResource;
  }

  @Path("/crl")
  public CRLResource getCrlResource() {
    return crlResource;
  }

  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get x509 certificate in pem format", response = CSRView.class)
  @JWTRequired(acceptedTokens={Audience.SERVICES, Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
  public Response getCertificate(
      @ApiParam(value = "X.500 name of the Certificate", required = true)
      @PathParam("name") String name,
      @ApiParam(value = "Status of certificate", required = true, allowableValues = "VALID, REVOKED, EXPIRED")
      @QueryParam("status") PKICertificate.Status status
  ) throws CAException, IOException {
    try {
      X509Certificate certificate = pki.loadCertificate(name, status);
      String pemified = pkiUtils.convertToPEM(certificate);
      CSRView cert = new CSRView(pemified, null, null);
      GenericEntity<CSRView> csrViewGenericEntity = new GenericEntity<CSRView>(cert) { };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(csrViewGenericEntity).build();
    } catch (GeneralSecurityException ex) {
      throw pkiUtils.certificateLoadingExceptionConvertToCAException(ex);
    }
  }
}
