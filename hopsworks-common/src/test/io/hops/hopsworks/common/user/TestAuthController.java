/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.user;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class TestAuthController {

  private UserFacade userFacade;
  private EmailBean emailBean;

  @Before
  public void init() {
    userFacade = Mockito.mock(UserFacade.class);
    Mockito.when(userFacade.update(Mockito.any())).thenReturn(null);

    emailBean = Mockito.mock(EmailBean.class);
  }

  @Test
  public void testRegisterFalseLoginUser() {
    AuthController authController = Mockito.spy(new AuthController());
    Mockito.when(authController.isUserInRole(Mockito.any(), Mockito.any())).thenReturn(false);
    authController.setUserFacade(userFacade);

    Users users = new Users();
    users.setFalseLogin(0);

    authController.registerFalseLogin(users);
    Assert.assertEquals(1, users.getFalseLogin());
  }

  @Test
  public void testRegisterFalseLoginUserBlock() {
    AuthController authController = Mockito.spy(new AuthController());
    Mockito.when(authController.isUserInRole(Mockito.any(), Mockito.any())).thenReturn(false);
    authController.setUserFacade(userFacade);
    authController.setEmailBean(emailBean);

    Users users = new Users();
    users.setFalseLogin(Settings.ALLOWED_FALSE_LOGINS);

    authController.registerFalseLogin(users);
    Assert.assertEquals(UserAccountStatus.BLOCKED_ACCOUNT, users.getStatus());
  }

  @Test
  public void testRegisterFalseLoginAgentNoFailedLogin() {
    AuthController authController = Mockito.spy(new AuthController());
    Mockito.when(authController.isUserInRole(Mockito.any(), Mockito.any())).thenReturn(true);
    authController.setUserFacade(userFacade);

    Users users = new Users();
    users.setFalseLogin(0);

    authController.registerFalseLogin(users);
    Assert.assertEquals(0, users.getFalseLogin());
  }

  @Test
  public void testRegisterFalseLoginAgentNoBlock() {
    AuthController authController = Mockito.spy(new AuthController());
    Mockito.when(authController.isUserInRole(Mockito.any(), Mockito.any())).thenReturn(true);
    authController.setUserFacade(userFacade);
    authController.setEmailBean(emailBean);

    Users users = new Users();
    users.setFalseLogin(Settings.ALLOWED_FALSE_LOGINS);
    users.setStatus(UserAccountStatus.ACTIVATED_ACCOUNT);

    authController.registerFalseLogin(users);
    Assert.assertEquals(UserAccountStatus.ACTIVATED_ACCOUNT, users.getStatus());
  }
}