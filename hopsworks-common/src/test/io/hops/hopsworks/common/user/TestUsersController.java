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
import io.hops.hopsworks.common.dao.user.UsersDTO;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountType;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class TestUsersController {

  @InjectMocks
  private UsersController usersController = new UsersController();

  @Mock
  private SecurityUtils securityUtils;

  @Mock
  private UserFacade userFacade;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    Mockito.when(securityUtils.generateSecret(Mockito.any())).thenCallRealMethod();
  }

  @Test
  public void testCreateUserEmailLowercase() {
    String email = "newEmail@Email.com";
    UsersDTO userDTO = new UsersDTO();
    userDTO.setEmail(email);
    userDTO.setFirstName("first");
    userDTO.setLastName("last");
    userDTO.setTwoFactor(true);
    userDTO.setChosenPassword("lolololololo");
    userDTO.setMaxNumProjects(1);
    Users user =
        usersController.createNewUser(userDTO, UserAccountStatus.VERIFIED_ACCOUNT, UserAccountType.M_ACCOUNT_TYPE);
    Assert.assertEquals(email.toLowerCase(), user.getEmail());
  }

}
