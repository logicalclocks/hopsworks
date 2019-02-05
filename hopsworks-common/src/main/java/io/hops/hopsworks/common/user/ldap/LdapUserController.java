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

package io.hops.hopsworks.common.user.ldap;

import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.ldap.LdapUserDTO;
import io.hops.hopsworks.common.dao.user.ldap.LdapUserFacade;
import io.hops.hopsworks.common.dao.user.ldap.LdapUser;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LdapUserController {

  private final static Logger LOGGER = Logger.getLogger(LdapUserController.class.getName());
  @EJB
  private LdapRealm ldapRealm;
  @EJB
  private LdapUserFacade ldapUserFacade;
  @EJB
  private UsersController userController;
  @EJB
  private BbcGroupFacade groupFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private Settings settings;

  /**
   * Try to login ldap user. 
   * @param username
   * @param password
   * @param consent
   * @param chosenEmail
   * @return
   * @throws LoginException 
   */
  public LdapUserState login(String username, String password, boolean consent, String chosenEmail) throws
      LoginException {
    LdapUserDTO userDTO = null;
    try {
      userDTO = ldapRealm.findAndBind(username, password);// login user
    } catch (EJBException | NamingException ee) {
      throw new LoginException("Could not reach LDAP server.");
    }
    if (userDTO == null) {
      throw new LoginException("User not found.");
    }
    LdapUser ladpUser = ldapUserFacade.findByLdapUid(userDTO.getEntryUUID());
    LdapUserState ldapUserState;
    if (ladpUser == null) {
      if (consent) {
        ladpUser = createNewLdapUser(userDTO, chosenEmail);
        persistLdapUser(ladpUser);
      }
      ldapUserState = new LdapUserState(consent, ladpUser, userDTO);
      return ldapUserState;
    }
    ldapUserState = new LdapUserState(true, ladpUser, userDTO);
    if (ldapUserUpdated(userDTO, ladpUser.getUid())) {
      ladpUser = updateLdapUser(userDTO, ladpUser);//do we need to ask again?
      ldapUserState.setLdapUser(ladpUser);
      return ldapUserState;
    }
    return ldapUserState;
  }

  private LdapUser createNewLdapUser(LdapUserDTO userDTO, String chosenEmail) throws LoginException {
    LOGGER.log(Level.INFO, "Creating new ldap user.");
    if (userDTO.getEmail().size() != 1 && (chosenEmail == null || chosenEmail.isEmpty())) {
      throw new LoginException("Could not register user. Email not chosen.");
    }
    if (!userDTO.getEmail().contains(chosenEmail)) {
      throw new LoginException("Could not register user. Chosen email not in ldap user email list.");
    }
    String email = userDTO.getEmail().size() == 1 ? userDTO.getEmail().get(0) : chosenEmail;
    Users u = userFacade.findByEmail(email);
    if (u != null) {
      throw new LoginException("Failed to login. User with the chosen email already exist in the system.");
    }
    String authKey = SecurityUtils.getRandomPassword(16);
    Users user = userController.createNewLdapUser(email, userDTO.getGivenName(), userDTO.getSn(), authKey,
        UserAccountStatus.fromValue(settings.getLdapAccountStatus()));
    List<String> groups = new ArrayList<>();
    try {
      groups = ldapRealm.getUserGroups(userDTO.getUid());
    } catch (NamingException ex) {
      throw new LoginException("Could not reach LDAP server.");
    }
    BbcGroup group;
    for (String grp : groups) {
      group = groupFacade.findByGroupName(grp);
      if (group != null) {
        user.getBbcGroupCollection().add(group);
      }
    }
    return new LdapUser(userDTO.getEntryUUID(), user, authKey);
  }

  private boolean ldapUserUpdated(LdapUserDTO user, Users uid) {
    if (user == null || uid == null) {
      return false;
    }
    return !uid.getFname().equals(user.getGivenName()) || !uid.getLname().equals(user.getSn());
  }

  private LdapUser updateLdapUser(LdapUserDTO user, LdapUser ldapUser) {
    if (!ldapUser.getUid().getFname().equals(user.getGivenName())) {
      ldapUser.getUid().setFname(user.getGivenName());
    }
    if (!ldapUser.getUid().getLname().equals(user.getSn())) {
      ldapUser.getUid().setLname(user.getSn());
    }
    return ldapUserFacade.update(ldapUser);
  }

  private void persistLdapUser(LdapUser ladpUser) {
    ldapUserFacade.save(ladpUser);
  }

}
