/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.persistence.PersistenceException;
import javax.persistence.QueryTimeoutException;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.security.audit.AccountsAuditActions;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.auth.AuthenticationConstants;
import se.kth.hopsworks.user.model.Users;

@ManagedBean
@RequestScoped
public class AccountVerification {

    @EJB
    private UserManager mgr;

    @EJB
    private AuditManager am;

//  @ManagedProperty(value = "#{param.key}")
    @ManagedProperty("#{param.key}")
    private String key;

    private String username;
    private boolean valid;
    private boolean dbDown = false;
    private boolean userNotFound = false;

    @PostConstruct
    public void init() {
        username = key.substring(0, 8);
        // get the 8 char username
        String secret = key.substring(8, key.length());
        valid = validateKey(secret);
    }

    private boolean validateKey(String key) {

        // If user loged in invalidate session first  
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        Users user = null;

        try {
            mgr.getUserByUsername(username);
        } catch (QueryTimeoutException ex) {
            dbDown = true;
            return false;
        } catch (PersistenceException ex) {
            dbDown = true;
            return false;
        }

        if (user == null) {
            userNotFound = true;
            return false;
        }

        if (user.getStatus() != PeopleAccountStatus.ACCOUNT_VERIFICATION.getValue()) {
            am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
                AccountsAuditActions.FAILED.name(), "Could not verify the account due to wrong status.", user);

            return false;
        }

        if (key.equals(user.getValidationKey())) {
            if (user.getMode() == PeopleAccountStatus.YUBIKEY_USER.getValue()) {

                mgr.changeAccountStatus(user.getUid(), "",
                    PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue());

            } else if (user.getMode() == PeopleAccountStatus.MOBILE_USER.
                getValue()) {

                mgr.changeAccountStatus(user.getUid(), "",
                    PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue());
            }

            am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
                AccountsAuditActions.SUCCESS.name(), "Verified account email address.", user);

            mgr.resetKey(user.getUid());
            return true;
        }

        int val = user.getFalseLogin();
        mgr.increaseLockNum(user.getUid(), val + 1);

        if (val > AuthenticationConstants.ALLOWED_FALSE_LOGINS) {
            mgr.changeAccountStatus(user.getUid(), "SPAM Acccount",
                PeopleAccountStatus.SPAM_ACCOUNT.getValue());
            mgr.resetKey(user.getUid());
            mgr.resetKey(user.getUid());
            am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
                AccountsAuditActions.FAILED.name(), "Too many false activation attemps.", user);

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

    public void setDbDown(boolean dbDown) {
        this.dbDown = dbDown;
    }

    public boolean isDbDown() {
        return dbDown;
    }

    public boolean isUserNotFound() {
        return userNotFound;
    }

    public void setUserNotFound(boolean userNotFound) {
        this.userNotFound = userNotFound;
    }

}
