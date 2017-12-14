/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.apiV2.users;

import io.hops.hopsworks.apiV2.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

@Path("/users")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Users", description = "Users Resource")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersResource {
  @EJB
  private UserFacade userBean;
  
  @ApiOperation("Get a list of users in the cluster")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response findAll(@Context SecurityContext sc) {

    List<Users> users = userBean.findAllUsers();
    List<UserView> userViews = new ArrayList<>();
    
    for (Users user : users) {
      UserView userView = new UserView(user);
      userViews.add(userView);
    }
    GenericEntity<List<UserView>> result = new GenericEntity<List<UserView>>(userViews) {};
    return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
  }
}
