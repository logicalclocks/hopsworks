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

import io.hops.hopsworks.common.dao.user.security.apiKey.ApiScope;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
   * Subject of password recovery
   */
  public final static String ACCOUNT_MOBILE_RECOVERY_SUBJECT
    = "You have requested to recover a lost device";

  /*
   * Subject of password rest
   */
  public final static String ACCOUNT_PASSWORD_RESET = "Your password has been reset";
  public final static String ACCOUNT_QR_RESET = "Your QR code has been reset";

  /*
   * Subject of rejected accounts
   */
  public final static String ACCOUNT_REJECT
          = "Your Hopsworks account request has been rejected";
  public final static String ACCOUNT_STATUS_CHANGED = "Your Hopsworks account status was changed";
  /*
   * Default accpount acitvation period
   */
  public final static int ACCOUNT_ACTIVATION_PERIOD = 48;

  public final static String GREETINGS_HEADER = "Hello";

  /*
   * Account deactivation
   */
  public final static String ACCOUNT_DEACTIVATED = "Your Hopsworks account has expired";
  
  public final static String API_KEY_CREATED_SUBJECT = "Api key created";
  public final static String API_KEY_DELETED_SUBJECT = "Api key deleted";

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
            + ACCOUNT_ACTIVATION_PERIOD
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
  public static String buildMobileRequestMessageRest(String path, String key, long validFor) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n" + "We received an account request for Hopsworks on your behalf.\n\n";
    String l2 = "Please click on the following link to verify your email address. We will activate your account within "
            + ACCOUNT_ACTIVATION_PERIOD + " hours after validating your email address.\n\n\n";

    String url = path + "?key=" + key;

    String l3 = "To confirm your email click " + url + " \n\n";
    String l4 = "If you did not request an account, please ignore this email. This link is only valid for"
      + formatTime(validFor) + ". \n\n";
    String l5 = "If you have any questions please contact " + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4 + l5;

    return message;
  }

  /**
   * Build an email message for mobile users upon registration.
   *
   * @param path
   * @param key
   * @return
   */
  public static String buildPasswordRecoveryMessage(String path, String key, long validFor) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n We received a password recovery request for Hopsworks on your behalf.\n\n";
    String l2 = "Please click on the following link to recover your password: \n";

    String url = path + "/hopsworks/#!/passwordRecovery?key=" + key;

    String l3 = url + " \n\n";
    String l4 = "If you did not request a password reset, please ignore this email. This password reset link is only " +
      "valid for " + formatTime(validFor) + ". \n\n";
    String l5 = "If you have any questions please contact " + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4 + l5;

    return message;
  }
  
  public static String buildQRRecoveryMessage(String path, String key, long validFor) {
    
    String message;
    
    String l1 = GREETINGS_HEADER + ",\n\n We received a lost mobile recovery request for Hopsworks on your behalf.\n\n";
    String l2 = "Please click on the following link to recover your QR code: \n";
    
    String url = path + "/hopsworks/#!/qrRecovery?key=" + key;
    
    String l3 = url + " \n\n";
    String l4 = "If you did not request a QR code reset, please ignore this email. This QR code reset link is only " +
      "valid for " + formatTime(validFor) + ". \n\n";
    String l5 = "If you have any questions please contact " + HOPSWORKS_SUPPORT_EMAIL;
  
    message = l1 + l2 + l3 + l4 + l5;
    
    return message;
  }
  
  public static String formatTime(long validForMs) {
    long hh = TimeUnit.MILLISECONDS.toHours(validForMs);
    long mm = TimeUnit.MILLISECONDS.toMinutes(validForMs) % 60;
    StringBuilder validFor = new StringBuilder();
    if (hh > 0) {
      validFor.append(hh).append(hh == 1? " hour" : " hours");
    }
    if (hh > 0 && mm > 0) {
      validFor.append(" and ").append(mm).append(mm == 1? " minute" : " minutes");
    } else if (mm > 0) {
      validFor.append(mm).append(mm == 1? " minute" : " minutes");
    }
    return validFor.toString();
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
            + "We received an account deactivation request and your Hopsworks "
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
  
  /**
   * Construct message for admin password reset
   *
   * @return
   */
  public static String buildResetByAdminMessage(String initiator) {
    String message;
    String l1 =
      GREETINGS_HEADER + ",\n\n" + "Your password was reset by a platform administrator (" + initiator + ").\n\n";
    String l2 = "Your password has been changed successfully.\n\n\n";
    String l3 = "If you have any questions please contact " + HOPSWORKS_SUPPORT_EMAIL;
    message = l1 + l2 + l3;
    return message;
  }
  
  public static String buildQRResetMessage() {
    String message;
    String l1 = GREETINGS_HEADER + ",\n\n A lost device has been reported on Hopsworks.\n\n";
    String l2 = "Your QR code has been changed successfully.\n\n\n";
    String l3 = "If you have any questions please contact " + HOPSWORKS_SUPPORT_EMAIL;
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
  
  public static String accountStatusChangeMessage(String status) {
    String message;
    
    String l1 = GREETINGS_HEADER + ",\n\n"
      + "Your Hopsworks account status was changed to " + status + ".\n\n";
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
            + ACCOUNT_ACTIVATION_PERIOD
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
            + ACCOUNT_ACTIVATION_PERIOD
            + " hours of getting this email.\n\n\n";

    String url = path + "/hopsworks-cluster/api/cluster/unregister/confirm/" + key;

    String l3 = "To confirm this request click " + url + " \n\n";
    String l4 = "If you have any questions please contact " + HOPSWORKS_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;

    return message;
  }
  
  public static String buildApiKeyCreatedMessage(String keyName, Date createdOn, String email, Set<ApiScope> scopes) {
    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
      + "You have successfully created an api key for your Hopsworks Account " + email + " named \"" + keyName +
      "\" on " + createdOn + ".\n" +
      "This api key will allow you to access your Hopsworks account from a device or application that can not login " +
      "with a username and password. Attaching this api key on a request authentication header will allow you to " +
      "access any hopsworks service in the scope: " + scopes + ".\n";
    String l2 = "Don't recognize this activity? please contact " + HOPSWORKS_SUPPORT_EMAIL;
  
    message = l1 + l2;
    return message;
  }
  
  public static String buildApiKeyDeletedMessage(String keyName, Date deletedOn, String email) {
    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
      + "You have deleted an api key created for your Hopsworks Account " + email + " named \"" + keyName +
      "\" on " + deletedOn + ".\n";
      
    String l2 = "Don't recognize this activity? please contact " + HOPSWORKS_SUPPORT_EMAIL;
  
    message = l1 + l2;
    return message;
  }
  
  public static String buildApiKeyDeletedAllMessage(Date deletedOn, String email) {
    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
      + "You have deleted all api keys created for your Hopsworks Account " + email + " on " + deletedOn + ".\n";
    
    String l2 = "Don't recognize this activity? please contact " + HOPSWORKS_SUPPORT_EMAIL;
    
    message = l1 + l2;
    return message;
  }

}
