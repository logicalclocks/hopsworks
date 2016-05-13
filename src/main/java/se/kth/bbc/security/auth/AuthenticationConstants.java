package se.kth.bbc.security.auth;


public final class AuthenticationConstants {
  
    // Issuer of the QrCode
  public static final String ISSUER = "hops.io";

  // To distinguish Yubikey users
  public static final String YUBIKEY_USER_MARKER = "YUBIKEY_USER_MARKER";

  // For disabled OTP auth mode: 44 chars
  public static final String YUBIKEY_OTP_PADDING
          = "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@";

  // For padding when password field is empty: 6 chars
  public static final String MOBILE_OTP_PADDING = "@@@@@@";


  // when user is loged in 1 otherwise 0  
  public static final int IS_ONLINE = 1;
  public static final int IS_OFFLINE = 0;

  
  public static final int ALLOWED_FALSE_LOGINS = 20;

  //hopsworks user prefix username prefix
  public static final String USERNAME_PREFIX = "meb";

  // POSIX compliant usernake length
  public static final int USERNAME_LENGTH = 8;

  
  // Strating user id from 1000 to create a POSIX compliant username: meb1000
  public static int STARTING_USER = 1000;
  
  public static int PASSWORD_MIN_LENGTH = 6;
  public static int PASSWORD_MAX_LENGTH = 128;

  // POSIX compliant usernake length
  public static final int ACCOUNT_VALIDATION_TRIES = 5;
  
}
