
package se.kth.bbc.security.ua;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.audit.model.Userlogins;
import se.kth.bbc.security.ua.model.Yubikey;
import se.kth.hopsworks.user.model.Users;
 
@ManagedBean
@SessionScoped
public class PeopleAdministration implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private UserManager userManager;

  @EJB
  private AuditManager auditManager;

  @EJB
  private EmailBean emailBean;

  @Resource
  private UserTransaction userTransaction;

  private Users user;

  // for yubikey administration page
  private Users selectedYubikyUser;

  private Address address;

  private String secAnswer;

  private List<Users> filteredUsers;
  private List<Users> selectedUsers;

  // All verified users
  private List<Users> allUsers;

  // Accounts waiting to be validated by the email owner
  private List<Users> spamUsers;

  // for modifying user roles and status
  private Users editingUser;

  // for mobile users activation
  private List<Users> requests;

  // for user activation
  private List<Users> yRequests;

  // to remove an existing group
  private String sgroup;

  public String getSgroup() {
    return sgroup;
  }

  public void setSgroup(String sgroup) {
    this.sgroup = sgroup;
  }

  // to assign a new stauts
  private String selectedStatus;

  // to assign a new group
  private String nGroup;

  List<String> status;

  // all groups
  List<String> groups;

  // all existing groups belong tp
  List<String> cGroups;

  // all possible new groups user doesnt belong to
  List<String> nGroups;

  // Yubikey public id. 12 chars: vviehlefjvcb
  private String pubid;

  // e.g. f1bda8c978766d50c25d48d72ed516e0
  private String secret;

  // current status of the editing user
  private String eStatus;

  public String geteStatus() {
    this.eStatus
            = PeopleAccountStatus.values()[this.editingUser.getStatus() - 1].
            name();
    return this.eStatus;
  }

  public void seteStatus(String eStatus) {
    this.eStatus = eStatus;
  }

  public String getnGroup() {
    return nGroup;
  }

  public void setnGroup(String nGroup) {
    this.nGroup = nGroup;
  }

  public Users getEditingUser() {
    return editingUser;
  }

  public void setEditingUser(Users editingUser) {
    this.editingUser = editingUser;
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

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public List<Users> getyRequests() {
    return yRequests;
  }

  public void setyRequests(List<Users> yRequests) {
    this.yRequests = yRequests;
  }

  public List<String> getUserRole(Users p) {
    List<String> list = userManager.findGroups(p.getUid());
    return list;
  }

  public String getChanged_Status(Users p) {
    return PeopleAccountStatus.values()[userManager.findByEmail(p.getEmail()).
            getStatus() - 1].name();
  }

  /*
   * public String getUserStatus(People p) {
   * return Integer.toString(userManager.findByEmail(p.getEmail()).getStatus());
   * }
   */
  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  /**
   * Filter the current groups
   *
   * @return
   */
  public List<String> getcGroups() {
    List<String> list = userManager.findGroups(editingUser.getUid());
    return list;
  }

  public void setcGroups(List<String> cGroups) {
    this.cGroups = cGroups;
  }

  public List<String> getnGroups() {
    List<String> list = userManager.findGroups(editingUser.getUid());
    List<String> tmp = new ArrayList<>();

    for (BBCGroup b : BBCGroup.values()) {

      if (!list.contains(b.name())) {
        tmp.add(b.name());
      }
    }
    return tmp;
  }

  public void setnGroups(List<String> nGroups) {
    this.nGroups = nGroups;
  }

  public String getSelectedStatus() {
    return selectedStatus;
  }

  public void setSelectedStatus(String selectedStatus) {
    this.selectedStatus = selectedStatus;
  }

  @PostConstruct
  public void initGroups() {
    groups = new ArrayList<>();
    status = new ArrayList<>();
// dont include BBCADMIN and BBCUSER roles for approving accounts as they are perstudy
    for (BBCGroup value : BBCGroup.values()) {
      if (value != BBCGroup.BBC_ADMIN) {
        groups.add(value.name());
      }
    }
  }

  public List<String> getStatus() {

    status = new ArrayList<>();

    int st = editingUser.getStatus();

    for (PeopleAccountStatus p : PeopleAccountStatus.values()) {
      status.add(p.name());
    }

    // remove the inactive users
    status.remove(PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.name());
    status.remove(PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.name());
    return status;
  }

  public void setStatus(List<String> status) {
    this.status = status;
  }

  public void setFilteredUsers(List<Users> filteredUsers) {
    this.filteredUsers = filteredUsers;
  }

  public List<Users> getFilteredUsers() {
    return filteredUsers;
  }

  /*
   * Find all registered users
   */
  public List<Users> getAllUsers() {
    if (allUsers == null) {
      allUsers = userManager.findAllUsers();
    }
    return allUsers;
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

  /**
   * Reject users that are not validated.
   *
   * @param user1
   */
  public void rejectUser(Users user1) {

    if (user1 == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    try {
      boolean removeByEmail = userManager.removeByEmail(user1.getEmail());

      // update the user request table
      if (removeByEmail) {

        if (user1.getStatus() == PeopleAccountStatus.ACCOUNT_VERIFICATION.
                getValue()) {
          spamUsers.remove(user1);
        } else if (user1.getMode() == PeopleAccountStatus.YUBIKEY_USER.
                getValue()) {
          yRequests.remove(user1);
        } else if (user1.getMode() == PeopleAccountStatus.MOBILE_USER.
                getValue()) {
          requests.remove(user1);
        }
      } else {
        MessagesController.addSecurityErrorMessage("Could not delete the user!");
      }
      emailBean.sendEmail(user1.getEmail(),
              UserAccountsEmailMessages.ACCOUNT_REJECT,
              UserAccountsEmailMessages.accountRejectedMessage());
      MessagesController.addInfoMessage(user1.getEmail() + " was rejected.");

    } catch (EJBException ejb) {
      MessagesController.addSecurityErrorMessage("Rejection failed");
    } catch (MessagingException  ex) {
      Logger.getLogger(PeopleAdministration.class.getName()).log(Level.SEVERE,
              "Could not reject user.", ex);
    }

  }

  public void confirmMessage(ActionEvent actionEvent) {

    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Deletion Successful!", null);
    FacesContext.getCurrentInstance().addMessage(null, message);
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
      System.err.println(extContext.getRequestContextPath());
      extContext.redirect(extContext.getRequestContextPath());
      return null;
    }
  }

  /**
   * Get all open user requests.
   *
   * @return
   */
  public List<Users> getAllRequests() {
    if (requests == null) {
      requests = userManager.findAllByStatus(
              PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue());
    }
    return requests;
  }

  /**
   * Get all Yubikey requests
   *
   * @return
   */
  public List<Users > getAllYubikeyRequests() {
    if (yRequests == null) {
      yRequests = userManager.findAllByStatus(
              PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue());
    }
    return yRequests;
  }

  public List<Users> getSelectedUsers() {
    return selectedUsers;
  }

  public void setSelectedUsers(List<Users> users) {
    this.selectedUsers = users;
  }

  public void activateUser(Users user1) {
    if (sgroup == null || sgroup.isEmpty()) {
      MessagesController.addSecurityErrorMessage("Select a role.");
      return;
    }
    try {

      userTransaction.begin();

      if (!"#".equals(sgroup) && (!sgroup.equals(BBCGroup.BBC_GUEST.name()))) {
        userManager.registerGroup(user1, BBCGroup.valueOf(sgroup).getValue());
      }else {
      
        MessagesController.addSecurityErrorMessage(sgroup +" already is granted.");
      }
      
      

      userManager.updateStatus(user1, PeopleAccountStatus.ACCOUNT_ACTIVE.
              getValue());
      userTransaction.commit();

      emailBean.sendEmail(user1.getEmail(),
              UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
              UserAccountsEmailMessages.
              accountActivatedMessage(user1.getEmail()));

    } catch (NotSupportedException | SystemException | MessagingException |
            RollbackException | HeuristicMixedException |
            HeuristicRollbackException | SecurityException |
            IllegalStateException e) {
      return;
    }
    requests.remove(user1);
  }

  public void blockUser(Users user1) {
    try {
      userTransaction.begin();
      userManager.updateStatus(user1, PeopleAccountStatus.ACCOUNT_BLOCKED.
              getValue());
      userTransaction.commit();

      emailBean.sendEmail(user1.getEmail(),
              UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
              UserAccountsEmailMessages.accountBlockedMessage());
    } catch (NotSupportedException | SystemException | MessagingException |
            RollbackException | HeuristicMixedException |
            HeuristicRollbackException | SecurityException |
            IllegalStateException ex) {
      Logger.getLogger(PeopleAdministration.class.getName()).log(Level.SEVERE,
              null, ex);
    }
    requests.remove(user1);
  }

  public String modifyUser(Users user1) {
    // Get the latest status
    Users newStatus = userManager.getUserByEmail(user1.getEmail());
    FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().put("editinguser", newStatus);

    Userlogins login = auditManager.getLastUserLogin(user1.getUid());

    FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().put("editinguser_logins", login);

    return "admin_profile";
  }

  public List<Users> getSpamUsers() {

    if (spamUsers == null) {
      //spamUsers = userManager.findAllSPAMAccounts();
    }
    return spamUsers;
  }

  public void setSpamUsers(List<Users> spamUsers) {
    this.spamUsers = spamUsers;
  }

  public String getSecAnswer() {
    return secAnswer;
  }

  public Users getSelectedYubikyUser() {
    return selectedYubikyUser;
  }

  public void setSelectedYubikyUser(Users selectedYubikyUser) {
    this.selectedYubikyUser = selectedYubikyUser;
  }

  public void setSecAnswer(String secAnswer) {
    this.secAnswer = secAnswer;
  }

  public SecurityQuestion[] getQuestions() {
    return SecurityQuestion.values();
  }

  public String activateYubikeyUser(Users user1) {
    this.selectedYubikyUser = user1;
    this.address = this.selectedYubikyUser.getAddress();
    return "activate_yubikey";
  }

  public String activateYubikey() throws MessagingException {
    try {
      // parse the creds  1486433,vviehlefjvcb,01ec8ce3dea6,f1bda8c978766d50c25d48d72ed516e0,,2014-12-14T23:16:09,

      if (this.selectedYubikyUser.getMode()
              != PeopleAccountStatus.YUBIKEY_USER.getValue()) {
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

      if (this.selectedYubikyUser.getYubikey().getStatus()
              == PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue()) {
        // Set stauts to active
        yubi.setStatus(PeopleAccountStatus.ACCOUNT_ACTIVE.getValue());

        userManager.updateYubikey(yubi);
        if (!"#".equals(sgroup) || (!sgroup.equals(BBCGroup.BBC_GUEST.name()))) {

          userManager.registerGroup(this.selectedYubikyUser, BBCGroup.valueOf(
                  sgroup).getValue());
        }else{
          MessagesController.addSecurityErrorMessage(sgroup +" already is granted.");
        }
      }

      userManager.updateStatus(this.selectedYubikyUser,
              PeopleAccountStatus.ACCOUNT_ACTIVE.getValue());
      userTransaction.commit();

      
      emailBean.sendEmail(this.selectedYubikyUser.getEmail(),
              UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
              UserAccountsEmailMessages.
              yubikeyAccountActivatedMessage(this.selectedYubikyUser.getEmail()));
      
      // Update the user management GUI
      yRequests.remove(this.selectedYubikyUser);

    } catch (NotSupportedException | SystemException | RollbackException |
            HeuristicMixedException | HeuristicRollbackException |
            SecurityException | IllegalStateException ex) {
      MessagesController.addSecurityErrorMessage("Technical Error!");
      return ("");
    }

    return "print_address";
  }


  public List<String> parseCredentials(String creds) {

    List<String> list = new ArrayList<>();

    StringTokenizer st = new StringTokenizer(creds, ",");
    while (st.hasMoreTokens()) {
      list.add(st.nextToken());
    }

    return list;

  }

  public Date getTimeStamp(String time) throws ParseException {
    SimpleDateFormat sdf
            = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Date tmp = new Date();
    tmp = sdf.parse(time);
    return tmp;
  }

  /**
   * Update user roles from profile by admin.
   */
  public void updateUserByAdmin() {
    try {
      // update status
      if (!"#".equals(selectedStatus)) {
        editingUser.setStatus(PeopleAccountStatus.valueOf(selectedStatus).
                getValue());
        userManager.updateStatus(editingUser, PeopleAccountStatus.valueOf(
                selectedStatus).getValue());
        MessagesController.addInfoMessage("Success",
                "Status updated successfully.");

      }

      // register a new group
      if (!"#".equals(nGroup)) {
        userManager.registerGroup(editingUser, BBCGroup.valueOf(nGroup).
                getValue());
        MessagesController.addInfoMessage("Success",
                "Role updated successfully.");

      }

      // remove a group
      if (!"#".equals(sgroup)) {
        if (sgroup.equals(BBCGroup.BBC_GUEST.name())) {
          MessagesController.addSecurityErrorMessage(BBCGroup.BBC_GUEST.name()
                  + " can not be removed.");
        } else {
          userManager.removeGroup(editingUser, BBCGroup.valueOf(sgroup).
                  getValue());
          MessagesController.addInfoMessage("Success",
                  "User updated successfully.");
        }
      }

      if ("#".equals(sgroup)) {

        if (("#".equals(selectedStatus))
                || "#".equals(nGroup)) {
          MessagesController.addSecurityErrorMessage("No selection made!");
        }
      }

    } catch (EJBException ejb) {
      MessagesController.addSecurityErrorMessage("Update failed.");
    }
  }
}
