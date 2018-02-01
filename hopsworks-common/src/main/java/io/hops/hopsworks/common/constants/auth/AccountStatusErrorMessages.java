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
