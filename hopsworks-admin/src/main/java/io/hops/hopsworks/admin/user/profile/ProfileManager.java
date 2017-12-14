package io.hops.hopsworks.admin.user.profile;

import io.hops.hopsworks.admin.lims.ClientSessionState;
import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.dao.user.UserFacade;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import io.hops.hopsworks.common.dao.user.security.Address;
import io.hops.hopsworks.common.dao.user.security.Organization;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.Userlogins;
import io.hops.hopsworks.common.user.UsersController;

@ManagedBean
@ViewScoped
public class ProfileManager implements Serializable {

  public static final String DEFAULT_GRAVATAR = "resources/images/icons/default-icon.jpg";

  private static final long serialVersionUID = 1L;
  @EJB
  private UserFacade userFacade;
  @EJB
  protected UsersController usersController;

  @EJB
  private AccountAuditFacade auditManager;

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
      user = userFacade.findByEmail(sessionState.getLoggedInUsername());
      address = user.getAddress();
      organization = user.getOrganization();
      login = auditManager.getLastUserLogin(user.getUid());
    }

    return user;
  }

  public List<String> getCurrentGroups() {
    List<String> list = usersController.getUserRoles(user);
    return list;
  }

  public void updateUserInfo() {

    try {
      userFacade.update(user);
      MessagesController.addInfoMessage("Success", "Profile updated successfully.");
      auditManager.registerAccountChange(sessionState.getLoggedInUser(), AccountsAuditActions.PROFILEUPDATE.name(),
              UserAuditActions.SUCCESS.name(), "", getUser());
    } catch (RuntimeException ex) {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);
      auditManager.registerAccountChange(sessionState.getLoggedInUser(), AccountsAuditActions.PROFILEUPDATE.name(),
              UserAuditActions.FAILED.name(), "", getUser());
      return;
    }
  }

  /**
   * Update organization info.
   */
  public void updateUserOrg() {

    try {
      user.setOrganization(organization);
      userFacade.update(user);
      MessagesController.addInfoMessage("Success", "Profile updated successfully.");
      auditManager.registerAccountChange(sessionState.getLoggedInUser(), AccountsAuditActions.PROFILEUPDATE.name(),
          UserAuditActions.SUCCESS.name(), "Update Organization", getUser());
    } catch (RuntimeException ex) {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);
      auditManager.registerAccountChange(sessionState.getLoggedInUser(), AccountsAuditActions.PROFILEUPDATE.name(),
          UserAuditActions.FAILED.name(), "Update Organization", getUser());
    }
  }

  /**
   * Update the user address in the profile and register the audit logs.
   */
  public void updateAddress() {

    try {
      user.setAddress(address);
      userFacade.update(user);
      MessagesController.addInfoMessage("Success", "Address updated successfully.");
      auditManager.registerAccountChange(sessionState.getLoggedInUser(), AccountsAuditActions.PROFILEUPDATE.name(),
          UserAuditActions.SUCCESS.name(), "Update Address", getUser());
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Update failed.");
      auditManager.registerAccountChange(sessionState.getLoggedInUser(), AccountsAuditActions.PROFILEUPDATE.name(),
          UserAuditActions.FAILED.name(), "Update Address", getUser());
    }
  }

}
