/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
public enum PeopleAccountStatus {

  // Status of new Yubikey users requests
// Status of new Yubikey users requests
// Status of new Yubikey users requests
// Status of new Yubikey users requests
  YUBIKEY_ACCOUNT_INACTIVE(1),

  // Status of new Mobile users requests
  MOBILE_ACCOUNT_INACTIVE(2),

    // Upon successful reset of passwords status of blocked will be changed to pending 
  // User will be able to reset the remporary password using the status
  // When user changed the password, pending state changes to active
  ACCOUNT_PENDING(3),

  // When user is approved to use the platform
  ACCOUNT_ACTIVE(4),

    // Blocked users can not user the resources. This can be due to suspicious behaviour of users. 
  // For example. multiple false logings
  ACCOUNT_BLOCKED(5),

  // For scenrios where mobile device is compromised/lost
  MOBILE_LOST(6),

  // For scenarios where Yubikey device is compromised/lost
  YUBIKEY_LOST(7),

    // Users that are no longer granted to access the platform.
  // Users with this state, can not login, change password even as guest users
  ACCOUNT_DEACTIVATED(8),

  // For new account requestes where users should validate their account request
  ACCOUNT_VERIFICATION(9),

  // To mark an account as spam
  SPAM_ACCOUNT(10),

  YUBIKEY_USER(11),

  MOBILE_USER(12);

  private final int value;

  private PeopleAccountStatus(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
