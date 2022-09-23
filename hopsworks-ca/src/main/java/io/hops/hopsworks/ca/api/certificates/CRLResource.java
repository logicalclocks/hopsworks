/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.ca.api.filter.NoCacheResponse;
import io.hops.hopsworks.ca.controllers.CAException;
import io.hops.hopsworks.ca.controllers.CAInitializationException;
import io.hops.hopsworks.ca.controllers.CertificateType;
import io.hops.hopsworks.ca.controllers.PKI;
import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;

@ApplicationScoped
@Api(value = "Resource to download HopsCA Certificate Revocation Lists")
public class CRLResource {

  @EJB
  private PKI pki;
  @EJB
  private NoCacheResponse noCacheResponse;
  
  @javax.ws.rs.Path("/intermediate")
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @ApiOperation(value = "Endpoint to download HopsCA intermediate CA Certificate Revocation List")
  public Response fetchCRL() throws CAException {
    try {
      String crl = pki.getCertificateRevocationListPEM(CAType.INTERMEDIATE);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(crl).build();
    } catch (GeneralSecurityException | IOException | CAInitializationException ex) {
      throw new CAException(RESTCodes.CAErrorCode.CERTIFICATE_REVOCATION_LIST_READ, Level.SEVERE,
          CertificateType.APP, "Failed to read CRL", ex.getMessage(), ex);
    }
  }
}
