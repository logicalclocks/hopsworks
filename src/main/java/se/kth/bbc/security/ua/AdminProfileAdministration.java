package se.kth.bbc.security.ua;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.AccountsAuditActions;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.audit.RolesAuditActions;
import se.kth.bbc.security.audit.UserAuditActions;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.audit.model.Userlogins;
import se.kth.hopsworks.user.model.Users;


@ManagedBean
@ViewScoped
public class AdminProfileAdministration implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private UserManager userManager;

  @EJB
  private AuditManager am;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;
 
  private Users user;
  // for modifying user roles and status
  private Users editingUser;

  // to remove an existing group
  private String selectedGroup;

  // to assign a new stauts
  private String selectedStatus;

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

  public List<String> getUserRole(Users p) {
    List<String> list = userManager.findGroups(p.getUid());
    return list;
  }

  public String getChangedStatus(Users p) {
    return PeopleAccountStatus.values()[userManager.findByEmail(p.getEmail()).
            getStatus() - 1].name();
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
    List<String> list = userManager.findGroups(editingUser.getUid());
    return list;
  }

  public void setCurrentGroups(List<String> currentGroups) {
    this.currentGroups = currentGroups;
  }

  public List<String> getNewGroups() {
    List<String> list = userManager.findGroups(editingUser.getUid());
    List<String> tmp = new ArrayList<>();

    for (BBCGroup b : BBCGroup.values()) {

      if (!list.contains(b.name())) {
        tmp.add(b.name());
      }
    }
    return tmp;
  }

  public String getEditStatus() {

    int status = userManager.getUserByEmail(this.editingUser.getEmail()).
            getStatus();
    this.editStatus = PeopleAccountStatus.values()[status - 1].name();
    return this.editStatus;
  }

  @PostConstruct
  public void init() {

    groups = new ArrayList<>();
    status = new ArrayList<>();

    for (BBCGroup value : BBCGroup.values()) {
      groups.add(value.name());
    }

    editingUser = (Users) FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().get("editinguser");
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
    status.remove(PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.name());
    status.remove(PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.name());

    return status;
  }

  public void setStatus(List<String> status) {
    this.status = status;
  }

  public List<Users> getUsersNameList() {
    return userManager.findAllUsers();
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
    HttpServletRequest request = (HttpServletRequest) context.
            getExternalContext().getRequest();

    Principal principal = request.getUserPrincipal();

    try {
      Users p = userManager.findByEmail(principal.getName());

      if (p != null) {
        return p.getFname() + " " + p.getLname();
      } else {
        return principal.getName();
      }
    } catch (Exception ex) {
      ExternalContext extContext = FacesContext.getCurrentInstance().
              getExternalContext();
      extContext.redirect(extContext.getRequestContextPath());
      return null;
    }
  }

  /**
   * Update user roles from profile by admin.
   */
  public void updateStatusByAdmin() {
    // Update status
    if (!"#".equals(selectedStatus)) {
      try {
      editingUser.setStatus(PeopleAccountStatus.valueOf(selectedStatus).
              getValue());
      userManager.updateStatus(editingUser, PeopleAccountStatus.valueOf(
              selectedStatus).getValue());
        am.registerAccountChange(sessionState.getLoggedInUser(),
                AccountsAuditActions.CHANGEDSTATUS.name(), UserAuditActions.SUCCESS.
                        name(), selectedStatus, editingUser);
      } catch (SocketException ex) {
   
        try {
          am.registerAccountChange(sessionState.getLoggedInUser(),
                  AccountsAuditActions.CHANGEDSTATUS.name(), UserAuditActions.FAILED.
                          name(), selectedStatus, editingUser);
        } catch (SocketException ex1) {
          Logger.getLogger(AdminProfileAdministration.class.getName()).
                  log(Level.SEVERE, null, ex1);
        }
   
        Logger.getLogger(AdminProfileAdministration.class.getName()).
                log(Level.SEVERE, null, ex);
      }
      MessagesController.addInfoMessage("Success",
              "Status updated successfully.");

    } else {
      MessagesController.addErrorMessage("Error", "No selection made!");

    }

  }

  public void addRoleByAdmin() {

    // Register a new group
    if (!"#".equals(newGroup)) {
      userManager.registerGroup(editingUser, BBCGroup.valueOf(newGroup).
              getValue());
      try {
        am.registerRoleChange(sessionState.getLoggedInUser(),
                RolesAuditActions.ADDROLE.name(), RolesAuditActions.SUCCESS.
                        name(),BBCGroup.valueOf(newGroup).name(), editingUser);
      } catch (SocketException ex) {
        
        try {
          am.registerRoleChange(sessionState.getLoggedInUser(),
                  RolesAuditActions.ADDROLE.name(), RolesAuditActions.FAILED.
                          name(),BBCGroup.valueOf(newGroup).name(), editingUser);
        } catch (SocketException ex1) {
          Logger.getLogger(AdminProfileAdministration.class.getName()).
                  log(Level.SEVERE, null, ex1);
        }
        
        Logger.getLogger(AdminProfileAdministration.class.getName()).
                log(Level.SEVERE, null, ex);
        
      }
      
      MessagesController.addInfoMessage("Success", "Role updated successfully.");

    } else {
      MessagesController.addErrorMessage("Error", "No selection made!!");

    }

  }

  public void removeRoleByAdmin() {

    // Remove a group
    if (!"#".equals(selectedGroup)) {
      if (selectedGroup.equals(BBCGroup.BBC_GUEST.toString())) {
        MessagesController.addErrorMessage("Error", BBCGroup.BBC_GUEST.
                toString() + " can not be removed.");
      } else {
        
        try {
        userManager.removeGroup(editingUser, BBCGroup.valueOf(selectedGroup).
                getValue());

          am.registerRoleChange(sessionState.getLoggedInUser(),
                  RolesAuditActions.REMOVEROLE.name(), RolesAuditActions.SUCCESS.
                          name(),BBCGroup.valueOf(selectedGroup).name(), editingUser);
        } catch (SocketException ex) {

          try {
            am.registerRoleChange(sessionState.getLoggedInUser(),
                    RolesAuditActions.REMOVEROLE.name(), RolesAuditActions.FAILED.
                            name(),BBCGroup.valueOf(selectedGroup).name(), editingUser);
          } catch (SocketException ex1) {
            Logger.getLogger(AdminProfileAdministration.class.getName()).
                    log(Level.SEVERE, null, ex1);
          }
          Logger.getLogger(AdminProfileAdministration.class.getName()).
                  log(Level.SEVERE, null, ex);
        }
        
        MessagesController.addInfoMessage("Success",
                "User updated successfully.");
      }
    }

    if ("#".equals(selectedGroup)) {

      if (("#".equals(selectedStatus))
              || "#".equals(newGroup)) {
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

}
