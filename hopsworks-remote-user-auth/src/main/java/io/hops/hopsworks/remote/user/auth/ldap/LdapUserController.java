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
package io.hops.hopsworks.remote.user.auth.ldap;

import io.hops.hopsworks.common.dao.remote.user.RemoteUser;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserType;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.RemoteUserStateDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.remote.user.auth.RemoteUserAuthController;

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
  private RemoteUserAuthController remoteUserAuthController;
  @EJB
  private RemoteUserFacade remoteUserFacade;
  @EJB
  private Settings settings;
  
  /**
   * Try to login ldap user.
   *
   * @param username
   * @param password
   * @param consent
   * @param chosenEmail
   * @return
   * @throws LoginException
   */
  public RemoteUserStateDTO login(String username, String password, boolean consent, String chosenEmail) throws
    LoginException {
    RemoteUserDTO userDTO = null;
    try {
      userDTO = ldapRealm.findAndBind(username, password);// login user
    } catch (EJBException | NamingException ee) {
      throw new LoginException("Could not reach LDAP server.");
    }
    return remoteUserAuthController.getRemoteUserStatus(userDTO, consent, chosenEmail, RemoteUserType.LDAP,
      UserAccountStatus.fromValue(settings.getLdapAccountStatus()));
  }
  
  /**
   * Get kerberos user from ldap
   *
   * @param principalName
   * @param consent
   * @param chosenEmail
   * @return
   * @throws LoginException
   */
  public RemoteUserStateDTO getKrbLdapUser(String principalName, boolean consent, String chosenEmail) throws
    LoginException {
    RemoteUserDTO userDTO = null;
    try {
      userDTO = ldapRealm.findKrbUser(principalName);
    } catch (EJBException | NamingException ee) {
      LOGGER.log(Level.WARNING, "Could not reach LDAP server. {0}", ee.getMessage());
      throw new LoginException("Could not reach LDAP server.");
    }
    return remoteUserAuthController.getRemoteUserStatus(userDTO, consent, chosenEmail, RemoteUserType.KRB,
      UserAccountStatus.fromValue(settings.getLdapAccountStatus()));
  }
  
  /**
   * @param user
   * @param password
   * @param consent
   * @param chosenEmail
   * @return
   * @throws LoginException
   */
  public RemoteUserStateDTO getLdapUser(Users user, String password, boolean consent, String chosenEmail)
    throws LoginException {
    RemoteUserDTO userDTO = null;
    RemoteUser remoteUser = remoteUserFacade.findByUsers(user);
    try {
      userDTO = ldapRealm.getLdapUser(remoteUser, password);
    } catch (EJBException | NamingException e) {
      LOGGER.log(Level.WARNING, "Could not reach LDAP server. {0}", e.getMessage());
      throw new LoginException("Could not reach LDAP server.");
    }
    return remoteUserAuthController.getRemoteUserStatus(userDTO, consent, chosenEmail, RemoteUserType.LDAP,
      UserAccountStatus.fromValue(settings.getLdapAccountStatus()));
  }
  
}
