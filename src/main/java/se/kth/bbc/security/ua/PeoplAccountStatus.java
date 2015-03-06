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

public enum PeoplAccountStatus{
    
    YUBIKEY_ACCOUNT_INACTIVE(1),
    MOBILE_ACCOUNT_INACTIVE(2),
    ACCOUNT_PENDING(3),
    ACCOUNT_ACTIVE(4),
    ACCOUNT_BLOCKED(5),
    MOBILE_LOST(6),
    YUBIKEY_LOST(7),
    ACCOUNT_DEACTIVATED(8);
    
    private final int value;

    private PeoplAccountStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}