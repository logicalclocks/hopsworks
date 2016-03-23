package se.kth.bbc.security.ua;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.security.audit.AccountsAuditActions;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.hopsworks.user.model.Users;


@ManagedBean
@RequestScoped
public class AccountVerification {

  @EJB
  private UserManager mgr;

    @EJB
  private AuditManager am;


  @ManagedProperty(value = "#{param.key}")
  private String key;

  private String username;
  private boolean valid;

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
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
            getRequest();

    Users user = mgr.getUserByUsername(username);

    if ((user.getStatus() != PeopleAccountStatus.NEW_MOBILE_ACCOUNT.getValue() 
            && user.getMode()== PeopleAccountStatus.M_ACCOUNT_TYPE.getValue()) || 
            (user.getStatus() != PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT.getValue() 
            && user.getMode()== PeopleAccountStatus.Y_ACCOUNT_TYPE.getValue())
            ) {
       am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.FAILED.name(), "Could not verify the account due to wrong status.", user);

      return false;
    }

    if (key.equals(user.getValidationKey())) {

        mgr.changeAccountStatus(user.getUid(), "",
                PeopleAccountStatus.VERIFIED_ACCOUNT.getValue());
        am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.SUCCESS.name(), "Verified account email address.", user);
        mgr.resetKey(user.getUid());
      return true;
    }

    int val = user.getFalseLogin();
    mgr.increaseLockNum(user.getUid(), val + 1);

    // if more than 5 times false logins set as spam
    if (val > 5) {

      mgr.changeAccountStatus(user.getUid(), PeopleAccountStatus.SPAM_ACCOUNT.toString(),
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

}