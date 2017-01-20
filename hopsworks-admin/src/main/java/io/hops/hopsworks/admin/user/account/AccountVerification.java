package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.persistence.PersistenceException;
import javax.persistence.QueryTimeoutException;
import javax.servlet.http.HttpServletRequest;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;

@ManagedBean
@RequestScoped
public class AccountVerification {

  @EJB
  private UserManager mgr;

  @EJB
  private AuditManager am;

  @ManagedProperty("#{param.key}")
  private String key;

  private String username;
  private boolean valid = false;
  private boolean alreadyRegistered = false;
  private boolean alreadyValidated = false;
  private boolean dbDown = false;
  private boolean userNotFound = false;

  @PostConstruct
  public void init() {
    if (key != null) {
      username = key.substring(0, AuthenticationConstants.USERNAME_LENGTH);
      // get the 8 char username
      String secret = key.substring(AuthenticationConstants.USERNAME_LENGTH,
              key.length());
      valid = validateKey(secret);
    }
  }

  private boolean validateKey(String key) {

    // If user loged in invalidate session first  
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
            getRequest();

    Users user = null;

    try {
      user = mgr.getUserByUsername(username);
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

    if ((user.getStatus() != PeopleAccountStatus.NEW_MOBILE_ACCOUNT.getValue()
            && user.getMode() == PeopleAccountStatus.M_ACCOUNT_TYPE.getValue())
            || (user.getStatus() != PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT.
            getValue()
            && user.getMode() == PeopleAccountStatus.Y_ACCOUNT_TYPE.getValue())) {
      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.FAILED.name(),
              "Could not verify the account due to wrong status.", user);

      if (user.getStatus() == PeopleAccountStatus.ACTIVATED_ACCOUNT.getValue()) {
        this.alreadyRegistered = true;
      }
      if (user.getStatus() == PeopleAccountStatus.VERIFIED_ACCOUNT.getValue()) {
        this.alreadyValidated = true;
      }

      return false;
    }

    if (key.equals(user.getValidationKey())) {
      mgr.changeAccountStatus(user.getUid(), "",
              PeopleAccountStatus.VERIFIED_ACCOUNT.getValue());
      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.SUCCESS.name(),
              "Verified account email address.", user);
      mgr.resetKey(user.getUid());
      return true;
    }

    int val = user.getFalseLogin();
    mgr.increaseLockNum(user.getUid(), val + 1);

    // if more than 5 times false logins set as spam
    if (val > AuthenticationConstants.ACCOUNT_VALIDATION_TRIES) {
      mgr.changeAccountStatus(user.getUid(), PeopleAccountStatus.SPAM_ACCOUNT.
              toString(),
              PeopleAccountStatus.SPAM_ACCOUNT.getValue());
      mgr.resetKey(user.getUid());
      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.FAILED.name(),
              "Too many false activation attemps.", user);

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

  public boolean isAlreadyValidated() {
    return alreadyValidated;
  }

  public void setAlreadyValidated(boolean alreadyValidated) {
    this.alreadyValidated = alreadyValidated;
  }

  public String setLogin() {
    return ("welcome");
  }

  public boolean isAlreadyRegistered() {
    return alreadyRegistered;
  }

  public void setAlreadyRegistered(boolean alreadyRegistered) {
    this.alreadyRegistered = alreadyRegistered;
  }

}
