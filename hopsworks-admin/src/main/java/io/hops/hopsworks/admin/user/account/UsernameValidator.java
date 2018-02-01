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

package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import io.hops.hopsworks.common.user.UsersController;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@ManagedBean
@RequestScoped
public class UsernameValidator implements Validator {

  @EJB
  protected UsersController usersController;

  // The pattern for email validation
  private static final String EMAIL_PATTERN
          = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@"
          + "((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";

  /**
   * Ensure the the username is available.
   *
   * @param context
   * @param component
   * @param value
   * @throws ValidatorException
   */
  @Override
  public void validate(FacesContext context, UIComponent component,
          Object value) throws ValidatorException {

    String uname = value.toString();

    if (!isValidEmail(uname)) {

      FacesMessage facesMsg = new FacesMessage(
              AccountStatusErrorMessages.INVALID_EMAIL_FORMAT);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);

    }

    if (usersController.isUsernameTaken(uname)) {
      FacesMessage facesMsg = new FacesMessage(AccountStatusErrorMessages.EMAIL_TAKEN);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);
    }

  }

  /**
   * Check if the email is a valid format.
   *
   * @param u
   * @return
   */
  public boolean isValidEmail(String u) {
    Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    Matcher matcher = pattern.matcher(u);
    return matcher.matches();
  }

}
