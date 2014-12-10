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
public interface AccountStatusIF {
    public static int ACCOUNT_INACTIVE = -1;
    public static int ACCOUNT_PENDING = 0;
    public static int ACCOUNT_ACTIVE = 1;
    public static int ACCOUNT_BLOCKED = 2;
    
}
