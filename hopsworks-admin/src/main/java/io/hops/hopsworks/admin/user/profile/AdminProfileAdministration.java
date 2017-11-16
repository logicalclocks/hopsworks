package io.hops.hopsworks.admin.user.profile;

import io.hops.hopsworks.admin.lims.ClientSessionState;
import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.util.EmailBean;
import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import io.hops.hopsworks.common.dao.user.security.Address;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.Userlogins;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.AuditUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class AdminProfileAdministration implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private UserFacade userFacade;
  @EJB
  protected UsersController usersController;

  @EJB
  private AccountAuditFacade am;

  @EJB
  private BbcGroupFacade bbcGroupFacade;

  @EJB
  private EmailBean emailBean;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  private Users user;
  // for modifying user roles and status
  private Users editingUser;

  // to remove an existing group
  private String selectedGroup;

  // to assign a new stauts
  private String selectedStatus;

  // maxNumProjs
//  private String maxNumProjs;
  // to assign a new group
  private String newGroup;

  // all groups
  List<String> groups;

  // all existing groups belong tp
  List<String> currentGroups;

  // all possible new groups user doesnt belong to
  List<String> newGroups;

  // current status of the editing user
  private String editStatus;

  List<String> status;

  private Userlogins login;

  private Address address;

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public Userlogins getLogin() {
    return login;
  }

  public void setLogin(Userlogins login) {
    this.login = login;
  }

  public void setEditStatus(String editStatus) {
    this.editStatus = editStatus;
  }

  public String getNew_group() {
    return newGroup;
  }

  public void setNew_group(String new_group) {
    this.newGroup = new_group;
  }

  public Users getEditingUser() {
    return editingUser;
  }

  public void setEditingUser(Users editingUser) {
    this.editingUser = editingUser;
  }

  public boolean mobileAccount() {
    return this.editingUser.getMode().equals(PeopleAccountType.M_ACCOUNT_TYPE);
  }

  public List<String> getUserRole(Users p) {
    List<String> list = usersController.getUserRoles(p);
    return list;
  }

  public String getChangedStatus(Users p) {
    return userFacade.findByEmail(p.getEmail()).getStatus().name();
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  public void setNewGroups(List<String> newGroups) {
    this.newGroups = newGroups;
  }

  public String getSelectedStatus() {
    return selectedStatus;
  }

  public void setSelectedStatus(String selectedStatus) {
    this.selectedStatus = selectedStatus;
  }

  public String getSelectedGroup() {
    return selectedGroup;
  }

  public void setSelectedGroup(String selectedGroup) {
    this.selectedGroup = selectedGroup;
  }

  /**
   * Filter the current groups of the user.
   *
   * @return
   */
  public List<String> getCurrentGroups() {
    List<String> list = usersController.getUserRoles(editingUser);
    return list;
  }

  public void setCurrentGroups(List<String> currentGroups) {
    this.currentGroups = currentGroups;
  }

  public List<String> getNewGroups() {
    List<String> list = usersController.getUserRoles(editingUser);
    List<String> tmp = new ArrayList<>();

    for (BbcGroup b : bbcGroupFacade.findAll()) {

      if (!list.contains(b.getGroupName())) {
        tmp.add(b.getGroupName());
      }
    }
    return tmp;
  }

  public String getEditStatus() {

    this.editStatus = userFacade.findByEmail(this.editingUser.getEmail()).getStatus().name();
    return this.editStatus;
  }

  @PostConstruct
  public void init() {

    groups = new ArrayList<>();
    status = new ArrayList<>();

    for (BbcGroup value : bbcGroupFacade.findAll()) {
      groups.add(value.getGroupName());
    }

    editingUser = (Users) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("editinguser");
    address = editingUser.getAddress();

    login = (Userlogins) FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().get("editinguser_logins");

  }

  public List<String> getStatus() {

    status = new ArrayList<>();

    for (PeopleAccountStatus p : PeopleAccountStatus.values()) {
      status.add(p.name());
    }

    // Remove the inactive users
    status.remove(PeopleAccountStatus.NEW_MOBILE_ACCOUNT.name());
    status.remove(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT.name());

    return status;
  }

  public void setStatus(List<String> status) {
    this.status = status;
  }

  public List<Users> getUsersNameList() {
    return userFacade.findAllUsers();
  }

  public List<String> getGroups() {
    return groups;
  }

  public Users getSelectedUser() {
    return user;
  }

  public void setSelectedUser(Users user) {
    this.user = user;
  }

  public String getLoginName() throws IOException {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

    Principal principal = request.getUserPrincipal();

    try {
      Users p = userFacade.findByEmail(principal.getName());

      if (p != null) {
        return p.getFname() + " " + p.getLname();
      } else {
        return principal.getName();
      }
    } catch (Exception ex) {
      ExternalContext extContext = FacesContext.getCurrentInstance().getExternalContext();
      extContext.redirect(extContext.getRequestContextPath());
      return null;
    }
  }

  /**
   * Update user roles from profile by admin.
   */
  public void updateStatusByAdmin() {
    // Update status
    if (!"#!".equals(selectedStatus)) {
      editingUser.setStatus(PeopleAccountStatus.valueOf(selectedStatus));
      try {
        userFacade.updateStatus(editingUser.getEmail(), PeopleAccountStatus.valueOf(selectedStatus));
        am.registerAccountChange(sessionState.getLoggedInUser(), AccountsAuditActions.CHANGEDSTATUS.name(),
            UserAuditActions.SUCCESS.name(), selectedStatus, editingUser);
        MessagesController.addInfoMessage("Success", "Status updated successfully.");
      } catch (Exception ex) {
        MessagesController.addInfoMessage("Problem", "Could not update account status.");
        Logger.getLogger(AdminProfileAdministration.class.getName()).log(Level.SEVERE, null, ex);
      }
    } else {
      am.registerAccountChange(sessionState.getLoggedInUser(), AccountsAuditActions.CHANGEDSTATUS.name(),
          UserAuditActions.FAILED.name(), selectedStatus, editingUser);
      MessagesController.addErrorMessage("Error", "No selection made!");

    }

  }

  public void addRoleByAdmin() {
    BbcGroup bbcGroup = bbcGroupFacade.findByGroupName(newGroup);

    // Register a new group
    if (!"#!".equals(newGroup)) {
      usersController.registerGroup(editingUser, bbcGroup.getGid());
      am.registerRoleChange(sessionState.getLoggedInUser(), RolesAuditActions.ADDROLE.name(), RolesAuditActions.SUCCESS.
          name(), bbcGroup.getGroupName(), editingUser);
      MessagesController.addInfoMessage("Success", "Role updated successfully.");

    } else {
      am.registerRoleChange(sessionState.getLoggedInUser(), RolesAuditActions.ADDROLE.name(), RolesAuditActions.FAILED.
          name(), bbcGroup.getGroupName(), editingUser);
      MessagesController.addErrorMessage("Error", "No selection made!!");
    }

  }

  public void removeRoleByAdmin() {
    BbcGroup bbcGroup = bbcGroupFacade.findByGroupName(selectedGroup);

    // Remove a group
    if (!"#!".equals(selectedGroup)) {
      userFacade.removeGroup(editingUser.getEmail(), bbcGroup.getGid());

      am.registerRoleChange(sessionState.getLoggedInUser(),
              RolesAuditActions.REMOVEROLE.name(), RolesAuditActions.SUCCESS.
              name(), bbcGroup.getGroupName(), editingUser);
      MessagesController.addInfoMessage("Success", "User updated successfully.");
    }

    if ("#!".equals(selectedGroup)) {

      if (("#!".equals(selectedStatus))
              || "#!".equals(newGroup)) {
        am.registerRoleChange(sessionState.getLoggedInUser(),
                RolesAuditActions.REMOVEROLE.name(), RolesAuditActions.FAILED.
                name(), bbcGroup.getGroupName(), editingUser);
        MessagesController.addErrorMessage("Error", "No selection made!");
      }
    }

  }

  public ClientSessionState getSessionState() {
    return sessionState;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public String getMaxNumProjs() {
    return userFacade.findByEmail(editingUser.getEmail()).getMaxNumProjects().
            toString();
  }

  public void setMaxNumProjs(String maxNumProjs) {
    int num = Integer.parseInt(maxNumProjs);
    usersController.updateMaxNumProjs(editingUser, num);
  }

  public boolean notVerified() {

    if (editingUser.getBbcGroupCollection().isEmpty() == false) {
      return false;
    }
    if (editingUser.getStatus().equals(PeopleAccountStatus.VERIFIED_ACCOUNT)) {
      return false;
    }
    return true;
  }

  public void resendAccountVerificationEmail() throws MessagingException {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.
            getExternalContext().getRequest();

    String activationKey = SecurityUtils.getRandomPassword(64);
    emailBean.sendEmail(editingUser.getEmail(), RecipientType.TO,
            UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
            UserAccountsEmailMessages.buildMobileRequestMessage(
                    AuditUtil.getUserURL(request), user.getUsername()
                    + activationKey));
    editingUser.setValidationKey(activationKey);
    userFacade.persist(editingUser);

  }

}
