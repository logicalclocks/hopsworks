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

package io.hops.hopsworks.common.constants.auth;

public class AccountStatusErrorMessages {

  public final static String INCCORCT_CREDENTIALS = "The email or password is incorrect.";

  public final static String INTERNAL_ERROR = "Internal servor error.";
  
  public final static String USER_NOT_FOUND = "No account found.";

  public final static String BLOCKED_ACCOUNT = "This account has been blocked.";

  public final static String INACTIVE_ACCOUNT = "This account has not been activated.";
  
  public final static String UNAPPROVED_ACCOUNT = "This account has not yet been approved.";

  public final static String INVALID_SEQ_ANSWER = "Inccorrect answer.";

  public final static String DEACTIVATED_ACCOUNT = "This account has been deactivated.";
  
  public final static String LOST_DEVICE = "This account has registered a lost device.";

  public final static String INCORRECT_PIN = "PIN must be 6 charachters.";

  public final static String PIN_REQUIERMENTS = "PIN must be numeric.";

  public final static String PASSWORD_REQUIREMNTS = "Password must contain at least 6 characters.";

  public final static String TOS_ERROR = "ToS not agreed";

  public final static String PASSWORD_ALPAHNUMERIC = "Password is not alphanumeric.";

  public final static String EMAIL_TAKEN = "Choose another username.";

  public final static String INVALID_EMAIL_FORMAT = "Invalid email format.";

  public final static String PASSWORD_MISMATCH = "Passwords do not match.";

  public final static String PASSWORD_EMPTY = "Enter a password.";

  public final static String INCORRECT_PASSWORD = "The password is incorrect.";

  public final static String INCORRECT_DEACTIVATION_LENGTH
          = "The message should have a length between 5 and 500 characters.";

  public final static String INCORRECT_TMP_PIN = "The temporary code was wrong.";

}
