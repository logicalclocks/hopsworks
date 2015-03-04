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
    YUBIKEY_ACCOUNT_INACTIVE(-2),
    MOBILE_ACCOUNT_INACTIVE(-1),
    ACCOUNT_PENDING(0),
    ACCOUNT_ACTIVE(1),
    ACCOUNT_BLOCKED(2),
    MOBILE_LOST(3),
    YUBIKEY_LOST(4),
    ACCOUNT_DEACTIVATED(5);
    
    private final int value;

    private PeoplAccountStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}