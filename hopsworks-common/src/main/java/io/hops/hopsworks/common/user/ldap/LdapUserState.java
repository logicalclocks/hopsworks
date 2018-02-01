/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.user.ldap;

import io.hops.hopsworks.common.dao.user.ldap.LdapUser;
import io.hops.hopsworks.common.dao.user.ldap.LdapUserDTO;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LdapUserState {
  
  private boolean saved;
  private LdapUser ldapUser;
  private LdapUserDTO userDTO;

  public LdapUserState(LdapUser ldapUser) {
    this.ldapUser = ldapUser;
  }

  public LdapUserState(boolean saved, LdapUser ldapUser) {
    this.saved = saved;
    this.ldapUser = ldapUser;
  }

  public LdapUserState(boolean saved, LdapUser ldapUser, LdapUserDTO userDTO) {
    this.saved = saved;
    this.ldapUser = ldapUser;
    this.userDTO = userDTO;
  }

  public boolean isSaved() {
    return saved;
  }

  public void setSaved(boolean saved) {
    this.saved = saved;
  }

  public LdapUser getLdapUser() {
    return ldapUser;
  }

  public void setLdapUser(LdapUser ldapUser) {
    this.ldapUser = ldapUser;
  }

  public LdapUserDTO getUserDTO() {
    return userDTO;
  }

  public void setUserDTO(LdapUserDTO userDTO) {
    this.userDTO = userDTO;
  }
  
}
