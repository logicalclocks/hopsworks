/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.api.zeppelin.rest;

import io.hops.hopsworks.api.zeppelin.server.JsonResponse;
import io.hops.hopsworks.api.zeppelin.util.SecurityUtils;
import io.hops.hopsworks.api.zeppelin.util.TicketContainer;
import java.util.ArrayList;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

/**
 * Zeppelin security rest api endpoint.
 * <p>
 */
@Path("/zeppelin/{projectID}/security")
@Stateless
@Produces("application/json")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
public class SecurityRestApi {

  private static final Logger LOG = LoggerFactory.getLogger(
          SecurityRestApi.class);

  /**
   * Required by Swagger.
   */
  public SecurityRestApi() {
    //super();
  }

  /**
   * Get ticket
   * Returns username & ticket
   * for anonymous access, username is always anonymous.
   * After getting this ticket, access through websockets become safe
   *
   * @param httpReq
   * @return 200 response
   */
  @GET
  @Path("ticket")
  public Response ticket(@Context HttpServletRequest httpReq) {
    ZeppelinConfiguration conf = ZeppelinConfiguration.create();
    String principal = httpReq.getRemoteUser();//SecurityUtils.getPrincipal();
    HashSet<String> roles = SecurityUtils.getRoles();//returns all roles
    JsonResponse response;
    // ticket set to anonymous for anonymous user. Simplify testing.
    String ticket;
    if (principal == null || "anonymous".equals(principal)) {
      ticket = "anonymous";
    } else {
      ticket = TicketContainer.instance.getTicket(principal);
    }

    Map<String, String> data = new HashMap<>();
    data.put("principal", principal);
    data.put("roles", roles.toString());
    data.put("ticket", ticket);

    response = new JsonResponse(Response.Status.OK, "", data);
    LOG.warn(response.toString());
    return response.build();
  }

  /**
   * Get userlist
   * Returns list of all user from available realms
   *
   * @param searchText
   * @return 200 response
   */
  @GET
  @Path("userlist/{searchText}")
  public Response getUserList(@PathParam("searchText") final String searchText) {

    List<String> autoSuggestList = new ArrayList<>();

    return new JsonResponse<>(Response.Status.OK, "", autoSuggestList).build();
  }

}
