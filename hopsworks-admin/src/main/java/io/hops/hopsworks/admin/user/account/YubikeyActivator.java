package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.admin.lims.ClientSessionState;
import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.util.EmailBean;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import io.hops.hopsworks.common.dao.user.security.Address;
import io.hops.hopsworks.common.dao.user.security.Yubikey;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;

@ManagedBean
@ViewScoped
public class YubikeyActivator implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(YubikeyActivator.class.
          getName());

  @EJB
  private UserManager userManager;

  @EJB
  private AuditManager auditManager;

  @EJB
  private BbcGroupFacade bbcGroupFacade;

  @EJB
  private EmailBean emailBean;

  @Resource
  private UserTransaction userTransaction;

  private Users user;

  private Address address;

  // Yubikey public id. 12 chars: vviehlefjvcb
  private String pubid;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  // e.g. f1bda8c978766d50c25d48d72ed516e0
  private String secret;
  // to remove an existing group
  private String sgroup;

  // for yubikey administration page
  private Users selectedYubikyUser;

  public String getSgroup() {
    return sgroup;
  }

  public void setSgroup(String sgroup) {
    this.sgroup = sgroup;
  }

  public Users getSelectedYubikyUser() {
    return selectedYubikyUser;
  }

  public void setSelectedYubikyUser(Users selectedYubikyUser) {
    this.selectedYubikyUser = selectedYubikyUser;
  }

  @PostConstruct
  public void init() {

    this.selectedYubikyUser = (Users) FacesContext.getCurrentInstance().
            getExternalContext().getSessionMap().get("yUser");
    this.address = this.selectedYubikyUser.getAddress();

    actGroups = new ArrayList<>();

    for (BbcGroup value : bbcGroupFacade.findAll()) {
      actGroups.add(value.getGroupName());
    }
  }

  // user activation groups to exclude guest and bbcuser
  List<String> actGroups;

  public List<String> getActGroups() {

    return actGroups;
  }

  public void setActGroups(List<String> actGroups) {
    this.actGroups = actGroups;
  }

  public String activateYubikey() throws MessagingException {

    try {
      // parse the creds  1486433,vviehlefjvcb,01ec8ce3dea6,f1bda8c978766d50c25d48d72ed516e0,,2014-12-14T23:16:09,

      if (this.selectedYubikyUser.getMode()
              != PeopleAccountStatus.Y_ACCOUNT_TYPE.getValue()) {
        MessagesController.addSecurityErrorMessage(user.getEmail()
                + " is not a Yubikey user");
        return "";
      }

      Yubikey yubi = this.selectedYubikyUser.getYubikey();

      // Trim the input
      yubi.setPublicId(pubid.replaceAll("\\s", ""));
      yubi.setAesSecret(secret.replaceAll("\\s", ""));

      // Update the info
      yubi.setCreated(new Date());
      yubi.setAccessed(new Date());
      yubi.setCounter(0);
      yubi.setSessionUse(0);
      yubi.setHigh(0);
      yubi.setLow(0);

      userTransaction.begin();

      if (this.selectedYubikyUser.getStatus()
              == PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT.getValue()
              && this.selectedYubikyUser.getYubikey().getStatus()
              != PeopleAccountStatus.LOST_YUBIKEY.getValue()) {
        // Set status to active
        yubi.setStatus(PeopleAccountStatus.ACTIVATED_ACCOUNT.getValue());

        userManager.updateYubikey(yubi);

        auditManager.registerAccountChange(sessionState.getLoggedInUser(),
                PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
                UserAuditActions.SUCCESS.name(), "", yubi.getUid());

        if (!"#".equals(this.sgroup.trim()) && (this.sgroup != null
                || !this.sgroup.isEmpty())) {
          BbcGroup bbcGroup = bbcGroupFacade.findByGroupName(this.sgroup);
          userManager.registerGroup(this.selectedYubikyUser, bbcGroup.getGid());
          auditManager.registerRoleChange(sessionState.getLoggedInUser(),
                  RolesAuditActions.ADDROLE.name(),
                  RolesAuditActions.SUCCESS.name(), bbcGroup.getGroupName(),
                  this.selectedYubikyUser);
        } else {
          MessagesController.addSecurityErrorMessage(
                  " Role could not be granted.");
          return ("");
        }
      }

      // for lost yubikey devices there is no need for role assignment
      if (this.selectedYubikyUser.getStatus()
              == PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT.getValue()
              && this.selectedYubikyUser.getYubikey().getStatus()
              == PeopleAccountStatus.LOST_YUBIKEY.getValue()) {

        // Set status to active
        yubi.setStatus(PeopleAccountStatus.ACTIVATED_ACCOUNT.getValue());
        userManager.updateYubikey(yubi);

        auditManager.registerAccountChange(sessionState.getLoggedInUser(),
                AccountsAuditActions.RECOVERY.name(),
                AccountsAuditActions.SUCCESS.name(), "", yubi.getUid());

      }

      userManager.updateStatus(this.selectedYubikyUser,
              PeopleAccountStatus.ACTIVATED_ACCOUNT.getValue());
      userTransaction.commit();

      auditManager.registerAccountChange(sessionState.getLoggedInUser(),
              PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
              UserAuditActions.SUCCESS.name(), "", yubi.getUid());

      emailBean.sendEmail(this.selectedYubikyUser.getEmail(), RecipientType.TO,
              UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
              UserAccountsEmailMessages.
              yubikeyAccountActivatedMessage(this.selectedYubikyUser.getEmail()));

      // Update the user management GUI
      // yRequests.remove(this.selectedYubikyUser);
    } catch (NotSupportedException | SystemException | RollbackException |
            HeuristicMixedException | HeuristicRollbackException |
            SecurityException | IllegalStateException ex) {
      MessagesController.addSecurityErrorMessage("Technical Error!");
      return ("");
    }

    return "print_address";
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getPubid() {
    return pubid;
  }

  public void setPubid(String pubid) {
    this.pubid = pubid;
  }

  public String getsgroup() {
    return sgroup;
  }

  public void setsgroup(String sgroup) {
    this.sgroup = sgroup;
  }

  public ClientSessionState getSessionState() {
    return sessionState;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

}
