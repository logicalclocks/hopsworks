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

package io.hops.hopsworks.common.user;

import io.hops.hopsworks.common.constants.message.ResponseMessages;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.exception.AppException;

@Stateless
public class UserValidator {

  @EJB
  private UserFacade userBean;

  public static final int PASSWORD_MIN_LENGTH = 6;
  public static final int PASSWORD_MAX_LENGTH = 255;
  private static final String PASSWORD_PATTERN
          = "(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).*$";
  private static final String EMAIL_PATTERN
          = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)"
          + "*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]"
          + "*[a-z0-9])?";

  public boolean isValidEmail(String email) throws AppException {
    if (email == null || email.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.EMAIL_EMPTY);
    }
    if (!isValid(email, EMAIL_PATTERN)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.EMAIL_INVALID);
    }

    return true;
  }

  public boolean isValidPassword(String password, String confirmedPassword)
          throws AppException {
    if (password.length() == 0) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_EMPTY);
    }
    if (password.length() < PASSWORD_MIN_LENGTH) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_TOO_SHORT);
    }
    if (password.length() > PASSWORD_MAX_LENGTH) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_TOO_LONG);
    }
    if (!isValid(password, PASSWORD_PATTERN)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_PATTERN_NOT_CORRECT);
    }
    if (!password.equals(confirmedPassword)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_MISS_MATCH);
    }

    return true;
  }

  public boolean isValidsecurityQA(String question, String answer) throws
          AppException {

    if (question == null || question.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.SEC_Q_EMPTY);
    } else if (SecurityQuestion.getQuestion(question) == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.SEC_Q_NOT_IN_LIST);
    }
    if (answer == null || answer.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.SEC_A_EMPTY);
    }

    return true;
  }

  public boolean isValidNewUser(UserDTO newUser) throws AppException {
    isValidEmail(newUser.getEmail());
    isValidPassword(newUser.getChosenPassword(), newUser.getRepeatedPassword());
    isValidsecurityQA(newUser.getSecurityQuestion(), newUser.getSecurityAnswer());
    if (newUser.getToS()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.TOS_NOT_AGREED);
    }
    if (userBean.findByEmail(newUser.getEmail()) != null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.USER_EXIST);
    }
    return true;
  }

  private boolean isValid(String u, String inPattern) {
    Pattern pattern = Pattern.compile(inPattern);
    Matcher matcher = pattern.matcher(u);
    return matcher.matches();
  }
}
