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
package io.hops.hopsworks.admin.user.administration;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import org.primefaces.model.LazyDataModel;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class SpamUserAdministrationBean implements Serializable {
  
  private static final Logger LOGGER = Logger.getLogger(SpamUserAdministrationBean.class.getName());
  
  @EJB
  private UserFacade userFacade;
  @EJB
  protected UserAdministrationController userAdministrationController;

  private LazyDataModel<Users> lazyUsers;
  private List<String> groups;
  
  @PostConstruct
  public void init() {
    lazyUsers = new SpamUsersAdminLazyDataModel(userFacade);
    groups = userAdministrationController.getAllGroupsNames();
  }
  
  public LazyDataModel<Users> getLazyUsers() {
    return lazyUsers;
  }
  
  public List<String> getGroups() {
    return groups;
  }
  
  public void setGroups(List<String> groups) {
    this.groups = groups;
  }
  
  public UserAccountType[] getAccountTypes() {
    return UserAccountType.values();
  }
  
  public String getAccountType(UserAccountType type) {
    return userAdministrationController.getAccountType(type);
  }
  
  public void deleteUser(Users user) {
    userAdministrationController.deleteSpamUser(user);
    LOGGER.log(Level.FINE, "Deleted spam user: {0}", user.getEmail());
  }
  
  public void removeFromSpam(Users user) {
    userAdministrationController.removeFromSpam(user);
    LOGGER.log(Level.FINE, "Removed from spam user: {0}", user.getEmail());
  }
}
