/*
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
 *
 */

package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import io.hops.hopsworks.common.exception.AppException;
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

    try {
      if (usersController.isUsernameTaken(uname)) {
        FacesMessage facesMsg = new FacesMessage(AccountStatusErrorMessages.EMAIL_TAKEN);
        facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
        throw new ValidatorException(facesMsg);
      }
    } catch (AppException ex) {
      FacesMessage facesMsg = new FacesMessage("Could not get user account from DB: " + uname);
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
