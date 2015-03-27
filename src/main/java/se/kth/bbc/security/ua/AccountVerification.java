/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.enterprise.context.RequestScoped;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */

@ManagedBean
@RequestScoped
public class AccountVerification {

    @EJB
    private UserManager mgr;
    
    @EJB
    private EmailBean emailBean;

    @ManagedProperty(value="#{param.key}")
    private String key;
    
    private String username;
    private boolean valid;

    @PostConstruct
    public void init() {
       
        username = key.substring(0, 8);
     
        // get the 8 char username
        String secret = key.substring(8, key.length());
             valid = check(secret);
    }

    private boolean check(String key) {
        User user  = mgr.getUserByUsernmae(username);
        
        if(user.getStatus()!= PeopleAccountStatus.ACCOUNT_VARIFICATION.getValue())
            return false;
        
        if (key.equals(user.getValidationKey())){
            if (user.getYubikeyUser()==1){
            
                mgr.restrictAccount(user.getUid(), "", PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue());

            } else if(user.getYubikeyUser()== PeopleAccountStatus.MOBILE_USER.getValue()) {
             
                mgr.restrictAccount(user.getUid(), "", PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue());
            }
            mgr.resetKey(user.getUid());
            return true;
        }
        
        int val = user.getFalseLogin();
        mgr.increaseLockNum(user.getUid(), val + 1);
        
        if (val > 5) {
            mgr.restrictAccount(user.getUid(),"SPAM Acccount",PeopleAccountStatus.SPAM_ACCOUNTS.getValue());
            mgr.resetKey(user.getUid());
            mgr.resetKey(user.getUid());
        }
  
        return false;
   }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
}
