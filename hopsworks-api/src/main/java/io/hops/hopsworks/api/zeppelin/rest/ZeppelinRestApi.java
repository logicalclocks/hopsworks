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

package io.hops.hopsworks.api.zeppelin.rest;

import io.hops.hopsworks.api.zeppelin.server.JsonResponse;
import io.swagger.annotations.Api;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.apache.zeppelin.util.Util;

/**
 */
@Path("/zeppelin/{projectID}")
@Stateless
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Zeppelin",
        description = "Zeppelin Api")
public class ZeppelinRestApi {

  public ZeppelinRestApi() {
  }

  /**
   * Get the root endpoint Return always 200.
   * <p/>
   * @return 200 response
   */
  @GET
  public Response getRoot() {
    return Response.ok().build();
  }

  @GET
  @Path("version")
  public Response getVersion() {
    return new JsonResponse<>(Response.Status.OK, "Zeppelin version", Util.
            getVersion()).build();
  }
}
