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

package io.hops.hopsworks.common.dao.user.security.ua;

public class UserAccountsEmailMessages {

  /*
   * Subject of account request
   */
  public final static String ACCOUNT_REQUEST_SUBJECT
          = "Your Hopsworks account needs verification";
  /*
   * Subject of cluster request
   */
  public final static String CLUSTER_REQUEST_SUBJECT = "Your Hopsworks cluster needs verification";

  /*
   * Subject of account activation email
   */
  public final static String ACCOUNT_CONFIRMATION_SUBJECT
          = "Welcome to Hopsworks!";

  public final static String ALERT_SERVICE_DOWN
          = "A Hopsworks service has stopped ";

  /*
   * Subject of device lost
   */
  public final static String DEVICE_LOST_SUBJECT = "Login issue";

  /*
   * Subject of blocked acouunt
   */
  public final static String ACCOUNT_BLOCKED__SUBJECT = "Your account is locked";

  /*
   * Subject of blocked acouunt
   */
  public static String HOPSWORKS_SUPPORT_EMAIL = "support@hops.io";

  /*
   * Subject of profile update
   */
  public final static String ACCOUNT_PROFILE_UPDATE
          = "Your profile has been updated";

  /*
   * Subject of password recovery
   */
  public final static String ACCOUNT_PASSWORD_RECOVERY_SUBJECT
          = "You have requested to recover your password";

  /*
   * Subject of password rest
   */
  public final static String ACCOUNT_PASSWORD_RESET
          = "Your password has been reset";

  /*
   * Subject of rejected accounts
   */
  public final static String ACCOUNT_REJECT
          = "Your Hopsworks account request has been rejected";

  /*
   * Default accpount acitvation period
   */
  public final static int ACCOUNT_ACITVATION_PERIOD = 48;

  public final static String GREETINGS_HEADER = "Hello";

  /*
   * Account deactivation
   */
  public final static String ACCOUNT_DEACTIVATED
          = "Your Hopsworks account has expired";

  /**
   * Build an email message for mobile users upon registration.
   *
   * @param path
   * @param key
   * @return
   */
  @Deprecated
  public static String buildMobileRequestMessage(String path, String key) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We received an account request for HopsWorks on your behalf.\n\n";
    String l2
            = "Please click on the following link to verify your email address. We"
            + " will activate your account within "
            + ACCOUNT_ACITVATION_PERIOD
            + " hours after validating your email address.\n\n\n";

    String url = path + "/hopsworks-admin/security/validate_account.xhtml?key=" + key;

    String l3 = "To confirm your email click " + url + " \n\n";
    String l4 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;

    return message;
  }
  
    /**
   * Build an email message for mobile users upon registration.
   *
   * @param path
   * @param key
   * @return
   */
  public static String buildMobileRequestMessageRest(String path, String key) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We received an account request for Hopsworks on your behalf.\n\n";
    String l2
            = "Please click on the following link to verify your email address. We"
            + " will activate your account within "
            + ACCOUNT_ACITVATION_PERIOD
            + " hours after validating your email address.\n\n\n";

    String url = path + "?key=" + key;

    String l3 = "To confirm your email click " + url + " \n\n";
    String l4 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;

    return message;
  }

  /**
   * Build an email message for mobile users upon registration.
   *
   * @param path
   * @param key
   * @return
   */
  public static String buildPasswordRecoveryMessage(String path, String key) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We received a password recovery request for Hopsworks on your behalf.\n\n";
    String l2
            = "Please click on the following link to recover your password: \n";

    String url = path + "?key=" + key;

    String l3 = url + " \n\n";
    String l4 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;

    return message;
  }
  
  public static String accountBlockedMessage() {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your Hopsworks account has been blocked.\n\n";
    String l2
            = "If you have any questions please visit www.hops.io or contact support@hops.io";
    String l3 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3;
    return message;
  }

  public static String buildPasswordResetMessage(String random_password) {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
            + "A password reset has been requested on your behalf.\n\nPlease use the temporary password"
            + " below. You will be required to change your passsword when you login first time.\n\n";

    String tmp_pass = "Password:" + random_password + "\n\n\n";
    String l3 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + tmp_pass + l3;
    return message;
  }

  public static String buildDeactivatedMessage() {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
        + "A password reset has been requested on your behalf.\n\n After too many fail at answering the "
        + "security question, your account has been deactivated\n\n";

    String l3 = "To reactivate you account please contact "
        + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l3;
    return message;
  }

  public static String buildWrongAnswerMessage() {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
        + "A password reset has been requested on your behalf.\n\n "
        + "You have provided the wrong answer to the security question. Please, try again.\n\n";

    String l3 = "If you have any questions please contact "
        + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l3;
    return message;
  }

  public static String buildSecResetMessage() {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
            + "A security question change has been requested on your behalf.\n\n";
    String l2 = "Your security question has been changed successfully.\n\n\n";
    String l3 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3;
    return message;
  }

  public static String buildDeactMessage() {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We receieved an account deactivation request and your Hopsworks "
            + "account has been deactivated.\n\n";
    String l2 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2;
    return message;
  }

  /**
   * Construct message for profile password change
   *
   * @return
   */
  public static String buildResetMessage() {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "A password reset has been requested on your behalf.\n\n";
    String l2 = "Your password has been changed successfully.\n\n\n";
    String l3 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;
    message = l1 + l2 + l3;

    return message;
  }

  public static String accountActivatedMessage(String username) {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your account request to access Hopsworks has been approved.\n\n";
    String l2 = "You can login with your username: " + username
            + " and other credentials you setup.\n\n\n";
    String l3 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;
    message = l1 + l2 + l3;

    return message;
  }

  public static String accountRejectedMessage() {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your Hopsworks account request has been rejected.\n\n";
    String l2 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;
    message = l1 + l2;

    return message;
  }

  public static String buildTempResetMessage(String random_password) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "A mobile device reset has been requested on your behalf.\n\n"
            + "Please use the temporary password below."
            + "You need to validate the code to get a new setup.\n\n";

    String tmp_pass = "Code:" + random_password + "\n\n\n";
    String l2 = "If you have any questions please contact "
            + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + tmp_pass + l2;

    return message;

  }
  
  public static String buildClusterRegisterRequestMessage(String path, String key) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We received an account request for Hopsworks on your behalf.\n\n";
    String l2 = "Please click on the following link to verify your email address. Within"
            + ACCOUNT_ACITVATION_PERIOD
            + " hours of getting this email.\n\n\n";

    String url = path + "/hopsworks-cluster/api/cluster/register/confirm/" + key;

    String l3 = "To confirm your email click " + url + " \n\n";
    String l4 = "If you have any questions please contact " + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;

    return message;
  }
  
  public static String buildClusterUnregisterRequestMessage(String path, String key) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We received a cluster remove request for Hops.site on your behalf.\n\n";
    String l2 = "Please click on the link below to verify your email address. Within"
            + ACCOUNT_ACITVATION_PERIOD
            + " hours of getting this email.\n\n\n";

    String url = path + "/hopsworks-cluster/api/cluster/unregister/confirm/" + key;

    String l3 = "To confirm your this request click " + url + " \n\n";
    String l4 = "If you have any questions please contact " + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;

    return message;
  }

}
