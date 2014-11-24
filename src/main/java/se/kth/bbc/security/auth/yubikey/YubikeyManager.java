/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.auth.yubikey;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import se.kth.bbc.security.ua.model.Yubikey;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Stateless
public class YubikeyManager {

    private static final Logger logger = Logger.getLogger(YubikeyManager.class.getName());
    @PersistenceContext
    private EntityManager em;

    boolean updateLastSeen(int id, int counter, int high, int low, int sessionUse) {

        Yubikey yubikey = (Yubikey) em.find(Yubikey.class, id);
        yubikey.setCounter(counter);
        yubikey.setHigh(high);
        yubikey.setLow(low);
        yubikey.setSessionUse(sessionUse);
        em.merge(yubikey);
        return true;
    }

    boolean deactivate(int id) {

        Yubikey yubikey = (Yubikey) em.find(Yubikey.class, id);
        yubikey.setActive(Boolean.FALSE);
        em.merge(yubikey);
        return true;
    }

}
