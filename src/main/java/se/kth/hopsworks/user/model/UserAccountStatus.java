/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.user.model;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
public enum UserAccountStatus {

    /**
     * Status of a new users requests that needs approval. 
     */
    ACCOUNT_INACTIVE(2),
    /**
     * When user is approved to use the platform
     */
    ACCOUNT_ACTIVE(4),
    /**
     * Blocked users can not user the resources. This can be due to suspicious
     * behavior of users. For example. multiple false logings.
     */
    ACCOUNT_BLOCKED(5),
    /**
     * Users that are no longer granted to access the platform. Users with this
     * state, can not login, change password even as guest users
     */
    ACCOUNT_DEACTIVATED(8),
    /**
     * For new account requests where users should validate their account
     * request
     */
    ACCOUNT_VERIFICATION(9);

    private final int value;

    private UserAccountStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
