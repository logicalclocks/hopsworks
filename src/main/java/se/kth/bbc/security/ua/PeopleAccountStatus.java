package se.kth.bbc.security.ua;
 
public enum PeopleAccountStatus {

// Status of new Mobile users requests
  NEW_MOBILE_ACCOUNT(1),

  // Status of new Yubikey users requests
  NEW_YUBIKEY_ACCOUNT(2),

  // For new account requestes where users should validate their account request
  VERIFIED_ACCOUNT(3),

  // When user is approved to use the platform
  ACTIVATED_ACCOUNT(4),

  // Users that are no longer granted to access the platform.
  // Users with this state, can not login, change password even as guest users
  DEACTIVATED_ACCOUNT(5),

  // Blocked users can not user the resources. This can be due to suspicious behaviour of users. 
  // For example. multiple false logings
  BLOCKED_ACCOUNT(6),
  
  // For scenrios where mobile device is compromised/lost
  LOST_MOBILE(7),

  // For scenarios where Yubikey device is compromised/lost
  LOST_YUBIKEY(8),

  // Mark account as SPAM

  SPAM_ACCOUNT(9),
  
  // For mobile account types classification
  M_ACCOUNT_TYPE(10),

  // For Yubikey account types classification
  Y_ACCOUNT_TYPE(11);

  private final int value;

  private PeopleAccountStatus(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
