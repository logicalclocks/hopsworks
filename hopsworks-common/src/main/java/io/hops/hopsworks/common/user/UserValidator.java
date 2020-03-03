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

package io.hops.hopsworks.common.user;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.UserException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class UserValidator {

  @EJB
  private UserFacade userBean;

  public static final int PASSWORD_MIN_LENGTH = 6;
  public static final int TEMP_PASSWORD_LENGTH = 8;
  public static final int PASSWORD_MAX_LENGTH = 255;
  private static final String PASSWORD_PATTERN
          = "(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).*$";
  private static final String EMAIL_PATTERN
          = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)"
          + "*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]"
          + "*[a-z0-9])?";

  public boolean isValidEmail(String email) throws UserException {
    if (Strings.isNullOrEmpty(email)) {
      throw new IllegalArgumentException("Email was not provided");
    }
    if (!isValid(email, EMAIL_PATTERN)) {
      throw new UserException(RESTCodes.UserErrorCode.INVALID_EMAIL, Level.FINE);
    }

    return true;
  }

  public boolean isValidPassword(String password, String confirmedPassword) throws UserException {
    if (password.length() == 0) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_EMPTY, Level.FINE);
    }
    if (password.length() < PASSWORD_MIN_LENGTH) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_TOO_SHORT, Level.FINE);
    }
    if (password.length() > PASSWORD_MAX_LENGTH) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_TOO_LONG, Level.FINE);
    }
    if (!isValid(password, PASSWORD_PATTERN)) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_PATTERN_NOT_CORRECT, Level.FINE);
    }
    if (!password.equals(confirmedPassword)) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_MISS_MATCH, Level.FINE);
    }

    return true;
  }

  public boolean isValidsecurityQA(String question, String answer) throws UserException {

    if (question == null || question.isEmpty()) {
      throw new IllegalArgumentException(RESTCodes.UserErrorCode.SEC_Q_EMPTY.getMessage());
    } else if (SecurityQuestion.getQuestion(question) == null) {
      throw new UserException(RESTCodes.UserErrorCode.SEC_Q_NOT_IN_LIST, Level.FINE);
    }
    if (answer == null || answer.isEmpty()) {
      throw new IllegalArgumentException(RESTCodes.UserErrorCode.SEC_A_EMPTY.getMessage());
    }

    return true;
  }

  public boolean isValidNewUser(UserDTO newUser) throws UserException {
    isValidEmail(newUser.getEmail());
    isValidPassword(newUser.getChosenPassword(), newUser.getRepeatedPassword());
    isValidsecurityQA(newUser.getSecurityQuestion(), newUser.getSecurityAnswer());
    if (userBean.findByEmail(newUser.getEmail()) != null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_EXISTS, Level.FINE);
    }
    if (!newUser.getTos()) {
      throw new UserException(RESTCodes.UserErrorCode.TOS_NOT_AGREED, Level.FINE);
    }
    return true;
  }

  private boolean isValid(String u, String inPattern) {
    Pattern pattern = Pattern.compile(inPattern);
    Matcher matcher = pattern.matcher(u);
    return matcher.matches();
  }
}
