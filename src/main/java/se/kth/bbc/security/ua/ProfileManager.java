package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.net.SocketException;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.AccountsAuditActions;
import se.kth.bbc.security.audit.UserAuditActions;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.Organization;
import se.kth.bbc.security.audit.model.Userlogins;
import se.kth.hopsworks.user.model.Users;

@ManagedBean
@ViewScoped
public class ProfileManager implements Serializable {

  public static final String DEFAULT_GRAVATAR
          = "resources/images/icons/default-icon.jpg";

  private static final long serialVersionUID = 1L;
  @EJB
  private UserManager userManager;

  @EJB
  private AuditManager auditManager;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  private Users user;
  private Address address;
  private Userlogins login;
  private Organization organization;

  private boolean editable;

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public Userlogins getLogin() {
    return login;
  }

  public void setLogin(Userlogins login) {
    this.login = login;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public Address getAddress() {
    return address;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public Users getUser() {
    if (user == null) {
      user = userManager.findByEmail(sessionState.getLoggedInUsername());
      address = user.getAddress();
      organization = user.getOrganization();
      login = auditManager.getLastUserLogin(user.getUid());
    }

    return user;
  }

  public List<String> getCurrentGroups() {
    List<String> list = userManager.findGroups(user.getUid());
    return list;
  }

  public void updateUserInfo() {

    if (userManager.updatePeople(user)) {
      MessagesController.addInfoMessage("Success",
              "Profile updated successfully.");
      auditManager.registerAccountChange(sessionState.getLoggedInUser(),
              AccountsAuditActions.PROFILEUPDATE.name(),
              UserAuditActions.SUCCESS.name(), "", getUser());

    } else {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);
      auditManager.registerAccountChange(sessionState.getLoggedInUser(),
              AccountsAuditActions.PROFILEUPDATE.name(),
              UserAuditActions.FAILED.name(), "", getUser());
      return;
    }
  }

  /**
   * Update organization info.
   */
  public void updateUserOrg(){

    if (userManager.updateOrganization(organization)) {
      MessagesController.addInfoMessage("Success",
              "Profile updated successfully.");
      auditManager.registerAccountChange(sessionState.getLoggedInUser(),
              AccountsAuditActions.PROFILEUPDATE.name(),
              UserAuditActions.SUCCESS.name(), "Update Organization", getUser());
    } else {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);

      auditManager.registerAccountChange(sessionState.getLoggedInUser(),
              AccountsAuditActions.PROFILEUPDATE.name(),
              UserAuditActions.FAILED.name(), "Update Organization", getUser());
      return;
    }
  }

  /**
   * Update the user address in the profile and register the audit logs.
   */
  public void updateAddress() {

    if (userManager.updateAddress(address)) {
      MessagesController.addInfoMessage("Success",
              "Address updated successfully.");
      auditManager.registerAccountChange(sessionState.getLoggedInUser(),
              AccountsAuditActions.PROFILEUPDATE.name(),
              UserAuditActions.SUCCESS.name(), "Update Address", getUser());
    } else {
      MessagesController.addSecurityErrorMessage("Update failed.");
      auditManager.registerAccountChange(sessionState.getLoggedInUser(),
              AccountsAuditActions.PROFILEUPDATE.name(),
              UserAuditActions.FAILED.name(), "Update Address", getUser());

      return;
    }
  }

}
