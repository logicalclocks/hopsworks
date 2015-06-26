package se.kth.bbc.security.ua;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.Organization;
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.security.ua.model.Userlogins;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@ViewScoped
public class ProfileManager implements Serializable {

  public static final String DEFAULT_GRAVATAR
          = "resources/images/icons/default-icon.jpg";

  private static final long serialVersionUID = 1L;
  @EJB
  private UserManager userManager;

  private User user;
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

  public User getUser() {
    if (user == null) {
      try {
        user = userManager.findByEmail(getLoginName());
        address = user.getAddress();
        organization = user.getOrganization();
        login = userManager.getLastUserLogin(user.getUid());
      } catch (IOException ex) {
        Logger.getLogger(ProfileManager.class.getName()).log(Level.SEVERE, null,
                ex);

        return null;
      }
    }

    return user;
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

  public void setUser(User user) {
    this.user = user;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public Address getAddress() {
    return this.address;
  }

  public String getLoginName() throws IOException {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.
            getExternalContext().getRequest();

    Principal principal = request.getUserPrincipal();

    try {
      return principal.getName();
    } catch (Exception ex) {
      ExternalContext extContext = FacesContext.getCurrentInstance().
              getExternalContext();
      extContext.redirect(extContext.getRequestContextPath());
      return null;
    }
  }

  public List<String> getCurrentGroups() {
    List<String> list = userManager.findGroups(user.getUid());
    return list;
  }

  public void updateUserInfo() {

    if (userManager.updatePeople(user)) {
      MessagesController.addInfoMessage("Success",
              "Profile updated successfully.");
    } else {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);

      return;
    }
  }

  public void updateUserOrg() {

    if (userManager.updateOrganization(organization)) {
      MessagesController.addInfoMessage("Success",
              "Profile updated successfully.");
    } else {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Failed to update", null);
      FacesContext.getCurrentInstance().addMessage(null, msg);

      return;
    }
  }

  public void updateAddress() {

    if (userManager.updateAddress(address)) {
      MessagesController.addInfoMessage("Success",
              "Address updated successfully.");
    } else {
      MessagesController.addSecurityErrorMessage("Update failed.");
      return;
    }
  }

}
