package se.kth.bbc.security.auth;


public final class AuthenticationConstants {
  
    // Issuer of the QrCode
  public static final String ISSUER = "BiobankCloud";

  // To distinguish Yubikey users
  public static final String YUBIKEY_USER_MARKER = "YUBIKEY_USER_MARKER";

  // For disabled OTP auth mode: 44 chars
  public static final String YUBIKEY_OTP_PADDING
          = "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@";

  // For padding when password field is empty: 6 chars
  public static final String MOBILE_OTP_PADDING = "@@@@@@";

    /*
   * offline: -1
   * online: 1
   */

  public static final int IS_ONLINE = 1;
  public static final int IS_OFFLINE = -1;

  public static final int ALLOWED_FALSE_LOGINS = 20;

  //hopsworks user prefix username prefix
  public static final String USERNAME_PREFIX = "meb";

  
  // Strating user id from 1000 to create a POSIX compliant username: meb1000
  public static int STARTING_USER = 1000;
  
  public static int PASSWORD_LENGTH = 6;
  
}
