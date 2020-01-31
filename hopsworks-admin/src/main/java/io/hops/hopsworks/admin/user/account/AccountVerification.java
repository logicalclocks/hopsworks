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

package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@RequestScoped
public class AccountVerification {

  @EJB
  protected AuditedUserAccountAction auditedUserAccountAction;

  @ManagedProperty("#{param.key}")
  private String key;

  private String username;
  private boolean valid = false;
  private boolean alreadyRegistered = false;
  private boolean alreadyValidated = false;
  private boolean dbDown = false;
  private boolean userNotFound = false;

  @PostConstruct
  public void init() {
    valid = validate(key);
  }
  
  private boolean validate(String key) {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();
    try {
      auditedUserAccountAction.validateKey(key, req);
      return true;
    } catch (PersistenceException ex) {
      dbDown = true;
    } catch (EJBException | IllegalArgumentException e) {
      return false;
    } catch (UserException ue) {
      if (RESTCodes.UserErrorCode.ACCOUNT_INACTIVE.equals(ue.getErrorCode())) {
        this.alreadyValidated = true;
      } else if (RESTCodes.UserErrorCode.ACCOUNT_ALREADY_VERIFIED.equals(ue.getErrorCode())) {
        this.alreadyRegistered = true;
      } else {
        this.userNotFound = true;
      }
    }
    return false;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public void setDbDown(boolean dbDown) {
    this.dbDown = dbDown;
  }

  public boolean isDbDown() {
    return dbDown;
  }

  public boolean isUserNotFound() {
    return userNotFound;
  }

  public void setUserNotFound(boolean userNotFound) {
    this.userNotFound = userNotFound;
  }

  public boolean isAlreadyValidated() {
    return alreadyValidated;
  }

  public void setAlreadyValidated(boolean alreadyValidated) {
    this.alreadyValidated = alreadyValidated;
  }

  public String setLogin() {
    return ("welcome");
  }

  public boolean isAlreadyRegistered() {
    return alreadyRegistered;
  }

  public void setAlreadyRegistered(boolean alreadyRegistered) {
    this.alreadyRegistered = alreadyRegistered;
  }

}
