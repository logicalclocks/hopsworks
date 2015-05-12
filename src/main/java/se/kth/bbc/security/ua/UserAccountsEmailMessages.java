/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.swing.text.AbstractDocument;

/**
 * This class contain the email messages that are sent to the users for account
 * management and registration.
 *
 * @author Ali Gholmai <gholami@pdc.kth.se>
 */
public class UserAccountsEmailMessages {

  /*
   * Subject of account request
   */
  public final static String ACCOUNT_REQUEST_SUBJECT = "Account Request";

  /*
   * Subject of account confirmation
   */
  public final static String ACCOUNT_CONFIRMATION_SUBJECT
          = "Account Confirmation";

  /*
   * Subject of password reset
   */
  public final static String ACCOUNT_PASSWORD_SUBJECT = "Password Reset";

  /*
   * Subject of device lost
   */
  public final static String DEVICE_LOST_SUBJECT = "Login Issue";

  /*
   * Subject of blocked acouunt
   */
  public final static String ACCOUNT_BLOCKED__SUBJECT = "Account Locked";

  /*
   * Subject of blocked acouunt
   */
  public final static String BIOBANKCLOUD_SUPPORT_EMAIL
          = "support@biobankcloud.com";

  /*
   * Subject of profile update
   */
  public final static String ACCOUNT_PROFILE_UPDATE = "Profile Update";

  /*
   * Subject of password rest
   */
  public final static String ACCOUNT_PASSWORD_RESET = "Password Reset";

  /*
   * Subject of rejected accounts
   */
  public final static String ACCOUNT_REJECT = "Account Rejected";

  /*
   * Default accpount acitvation period
   */
  public final static int ACCOUNT_ACITVATION_PERIOD = 48;

  public final static String GREETINGS_HEADER = "Hello";

  /*
   * Account deactivation
   */
  public final static String ACCOUNT_DEACTIVATED = "Account Deactivated";

  /**
   * Build an email message for Yubikey users upon registration.
   *
   * @param username
   * @param key
   * @return
   */
  public static String buildYubikeyRequestMessage(String path, String key) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We receieved your yubikey account request for the BiobankCloud.\n\n";
    String l2
            = "You will receive a Yubikey device within 48 hours at your address.\n\n\n";

    String url = path + "/security/validate_account.xhtml?key=" + key;

    String l3 = "To confirm your email click " + url + " \n\n";

    String l4 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;

    return message;
  }

  /**
   * Build an email message for mobile users upon registration.
   *
   * @param username
   * @param key
   * @return
   */
  public static String buildMobileRequestMessage(String path, String key) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We received your mobile account request for the BiobankCloud.\n\n";
    String l2 = "Your account will be activated within "
            + ACCOUNT_ACITVATION_PERIOD
            + " hours after validating your email address.\n\n\n";

    String url = path + "/security/validate_account.xhtml?key=" + key;

    String l3 = "To confirm your email click " + url + " \n\n";
    String l4 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;
    return message;
  }

  public static String accountBlockedMessage() {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your account in the Biobankcloud has been blocked.\n\n";
    String l2
            = "If you have any questions please contact support@biobankcloud.com";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

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
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + tmp_pass + l3;
    return message;
  }

  public static String buildSecResetMessage() {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
            + "A security question change has been requested on your behalf.\n\n";
    String l2 = "Your security question has been changed successfully.\n\n\n";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2 + l3;
    return message;
  }

  public static String buildDeactMessage() {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We receieved an account deactivation request and your BiobankCloud account has been deactivated.\n\n";
    String l2 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

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
            + BIOBANKCLOUD_SUPPORT_EMAIL;
    message = l1 + l2 + l3;

    return message;
  }

  public static String accountActivatedMessage(String username) {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your account request to access the BiobankCloud has been approved.\n\n";
    String l2 = "You can login with your username: " + username + "\n\n\n";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;
    message = l1 + l2 + l3;

    return message;
  }

  public static String accountRejectedMessage() {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your Biobankcloud account request has been rejected.\n\n";
    String l2 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;
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
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + tmp_pass + l2;
    return message;
  }

  public static String buildYubikeyResetMessage() {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We received your mobile account request for the BiobankCloud.\n\n";
    String l2 = "Your account will be activated within "
            + ACCOUNT_ACITVATION_PERIOD + " hours.\n\n\n";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2 + l3;

    return message;
  }
}
