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

import io.hops.hopsworks.ca.controllers.PKI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.file.Path;

@ApplicationScoped
@Api(value = "Resource to download HopsCA Certificate Revocation Lists")
public class CRLResource {
  
  @EJB
  private PKI pki;
  
  @javax.ws.rs.Path("/intermediate")
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @ApiOperation(value = "Endpoint to download HopsCA intermediate CA Certificate Revocation List")
  public Response fetchCRL() {
    return returnCRL(pki.getCACRLPath(PKI.CAType.INTERMEDIATE));
  }
  
  private Response returnCRL(Path crl) {
    return Response.ok(crl.toFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + crl.getFileName().toString() + "\"")
        .build();
  }
}
