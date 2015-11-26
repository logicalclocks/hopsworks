
package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.net.SocketException;
import java.util.List;
import javax.annotation.PostConstruct;
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
import se.kth.bbc.security.audit.AuditUtil;
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

  /*
   * public User getUser() {
   * if (user == null) {
   * try {
   * user = userManager.findByEmail(getLoginName());
   * address = user.getAddress();
   * organization = user.getOrganization();
   * login = auditManager.getLastUserLogin(user.getUid());
   * } catch (IOException ex) {
   * Logger.getLogger(ProfileManager.class.getName()).log(Level.SEVERE, null,
   * ex);
   *
   * return null;
   * }
   * }
   *
   * return user;
   * }
   */
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

  public void updateUserInfo() throws SocketException {


    if (userManager.updatePeople(user)) {
      MessagesController.addInfoMessage("Success",
              "Profile updated successfully.");
      auditManager.registerAccountChange(getUser(),
              AccountsAuditActions.PROFILEUPDATE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "SUCCESS", "UPDATE INFO");
    } else {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);
      auditManager.registerAccountChange(getUser(),
              AccountsAuditActions.PROFILEUPDATE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "UPDATE INFO");
      return;
    }
  }

  /**
   *
   * @throws java.net.SocketException
   */
  public void updateUserOrg() throws SocketException {

    if (userManager.updateOrganization(organization)) {
      MessagesController.addInfoMessage("Success",
              "Profile updated successfully.");
      auditManager.registerAccountChange(getUser(),
              AccountsAuditActions.PROFILEUPDATE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "SUCCESS", "UPDATE ORGANIZATION");
    } else {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);

      auditManager.registerAccountChange(getUser(),
              AccountsAuditActions.PROFILEUPDATE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "UPDATE ORGANIZATION");
      return;
    }
  }

  /**
   * Update the user address in the profile and register the audit logs.
   * <p>
   * @throws SocketException
   */
  public void updateAddress() throws SocketException {

    if (userManager.updateAddress(address)) {
      MessagesController.addInfoMessage("Success",
              "Address updated successfully.");
      auditManager.registerAccountChange(getUser(),
              AccountsAuditActions.PROFILEUPDATE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "SUCCESS", "UPDATE ADDRESS");
    } else {
      MessagesController.addSecurityErrorMessage("Update failed.");
      auditManager.registerAccountChange(getUser(),
              AccountsAuditActions.PROFILEUPDATE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "UPDATE ADDRESS");

      return;
    }
  }

}
