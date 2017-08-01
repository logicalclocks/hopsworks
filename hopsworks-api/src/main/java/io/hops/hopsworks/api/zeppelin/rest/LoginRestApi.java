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
import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.user.UsersController;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.zeppelin.notebook.NotebookAuthorization;

/**
 * Created for org.apache.zeppelin.rest.message on 17/03/16.
 */
@Path("/zeppelin/{projectID}/login")
@Stateless
@Produces("application/json")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
public class LoginRestApi {

  private static final Logger LOG = LoggerFactory.getLogger(LoginRestApi.class);

  @EJB
  private UserFacade userBean;
  @EJB
  private UsersController userController;
  @EJB
  private AuditManager am;

  /**
   * Required by Swagger.
   */
  public LoginRestApi() {
    super();
  }

  /**
   * Post Login
   * Returns userName & password
   * for anonymous access, username is always anonymous.
   * After getting this ticket, access through websockets become safe
   *
   * @param userName
   * @param password
   * @return 200 response
   */
  @POST
  public Response postLogin(@FormParam("userName") String userName,
          @FormParam("password") String password) {
    JsonResponse response = null;
    // ticket set to anonymous for anonymous user. Simplify testing.
    Subject currentUser = org.apache.shiro.SecurityUtils.getSubject();
    if (currentUser.isAuthenticated()) {
      currentUser.logout();
    }
    if (!currentUser.isAuthenticated()) {
      try {
        UsernamePasswordToken token = new UsernamePasswordToken(userName, password);
        //      token.setRememberMe(true);
        currentUser.login(token);
        HashSet<String> roles = SecurityUtils.getRoles();
        String principal = SecurityUtils.getPrincipal();
        String ticket;
        if ("anonymous".equals(principal)) {
          ticket = "anonymous";
        } else {
          ticket = TicketContainer.instance.getTicket(principal);
        }

        Map<String, String> data = new HashMap<>();
        data.put("principal", principal);
        data.put("roles", roles.toString());
        data.put("ticket", ticket);

        response = new JsonResponse(Response.Status.OK, "", data);
        //if no exception, that's it, we're done!
        
        //set roles for user in NotebookAuthorization module
        NotebookAuthorization.getInstance().setRoles(principal, roles);
      } catch (UnknownAccountException uae) {
        //username wasn't in the system, show them an error message?
        LOG.error("Exception in login: ", uae);
      } catch (IncorrectCredentialsException ice) {
        //password didn't match, try again?
        LOG.error("Exception in login: ", ice);
      } catch (LockedAccountException lae) {
        //account for that username is locked - can't login.  Show them a message?
        LOG.error("Exception in login: ", lae);
      } catch (AuthenticationException ae) {
        //unexpected condition - error?
        LOG.error("Exception in login: ", ae);
      }
    }

    if (response == null) {
      response = new JsonResponse(Response.Status.FORBIDDEN, "", "");
    }

    LOG.warn(response.toString());
    return response.build();
  }

  @POST
  @Path("logout")
  public Response logout(@Context HttpServletRequest req) {
    JsonResponse response;

    Map<String, String> data = new HashMap<>();
    data.put("principal", "anonymous");
    data.put("roles", "");
    data.put("ticket", "anonymous");
    Users user = userBean.findByEmail(req.getRemoteUser());
    try {
      req.logout();
      req.getSession().invalidate();
      if (user != null) {
        userController.setUserIsOnline(user, AuthenticationConstants.IS_OFFLINE);
        am.registerLoginInfo(user, UserAuditActions.LOGOUT.name(),
                UserAuditActions.SUCCESS.name(), req);
        TicketContainer.instance.invalidate(user.getEmail());
      }
    } catch (ServletException e) {
      am.registerLoginInfo(user, UserAuditActions.LOGOUT.name(),
              UserAuditActions.FAILED.name(), req);
    }

    response = new JsonResponse(Response.Status.OK, "", data);
    LOG.warn(response.toString());
    return response.build();
  }

}
