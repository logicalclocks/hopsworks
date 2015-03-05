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
public final class AccountStatus {
    public static int YUBIKEY_ACCOUNT_INACTIVE = 1;
    public static int MOBILE_ACCOUNT_INACTIVE = 2;
    public static int ACCOUNT_PENDING = 3;
    public static int ACCOUNT_ACTIVE = 4;
    public static int ACCOUNT_BLOCKED = 5;
    
    // lost mobile device
    public static int MOBILE_LOST = 6;

    // lost yubikey device
    public static int YUBIKEY_LOST = 7;

    // account deactivated
    public static int ACCOUNT_DEACTIVATED = 8;
 
}
