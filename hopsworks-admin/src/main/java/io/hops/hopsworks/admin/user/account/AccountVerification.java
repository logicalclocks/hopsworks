package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.dao.user.UserFacade;
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
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountType;
import io.hops.hopsworks.common.user.UsersController;

@ManagedBean
@RequestScoped
public class AccountVerification {

  @EJB
  private UserFacade userFacade;
  @EJB
  protected UsersController usersController;

  @EJB
  private AccountAuditFacade am;

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
      user = userFacade.findByUsername(username);
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

    if ((!user.getStatus().equals(PeopleAccountStatus.NEW_MOBILE_ACCOUNT)
            && user.getMode().equals(PeopleAccountType.M_ACCOUNT_TYPE))
            || (!user.getStatus().equals(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT)
            && user.getMode().equals(PeopleAccountType.Y_ACCOUNT_TYPE))) {
      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.FAILED.name(),
              "Could not verify the account due to wrong status.", user);

      if (user.getStatus().equals(PeopleAccountStatus.ACTIVATED_ACCOUNT)) {
        this.alreadyRegistered = true;
      }
      if (user.getStatus().equals(PeopleAccountStatus.VERIFIED_ACCOUNT)) {
        this.alreadyValidated = true;
      }

      return false;
    }

    if (key.equals(user.getValidationKey())) {
      usersController.changeAccountStatus(user.getUid(), "",
              PeopleAccountStatus.VERIFIED_ACCOUNT);
      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.SUCCESS.name(),
              "Verified account email address.", user);
      usersController.resetKey(user.getUid());
      return true;
    }

    int val = user.getFalseLogin();
    usersController.increaseLockNum(user.getUid(), val + 1);

    // if more than 5 times false logins set as spam
    if (val > AuthenticationConstants.ACCOUNT_VALIDATION_TRIES) {
      usersController.changeAccountStatus(user.getUid(), PeopleAccountStatus.SPAM_ACCOUNT.
              toString(),
              PeopleAccountStatus.SPAM_ACCOUNT);
      usersController.resetKey(user.getUid());
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
