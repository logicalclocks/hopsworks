/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.People;
import se.kth.kthfsdashboard.user.Gravatar;
import se.kth.kthfsdashboard.user.UserController;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class ProfileManager implements Serializable {

    private static final Logger logger = Logger.getLogger(ProfileManager.class.getName());

    public static final String DEFAULT_GRAVATAR = "resources/images/icons/default-icon.jpg";

    private static final long serialVersionUID = 1L;
    @EJB
    private UserManager userManager;

    // for mobile users activation
    private List<People> requests;

    // for user activation
    private List<People> yubikey_requests;

    // for yubikey administration page
    private People selectedYubikyUser;

    private People user;
    private Address address;

    public People getUser() {
        if (user == null) {
            try {
                user = userManager.findByEmail(getLoginName());
                address = userManager.findAddress(user.getUid());
            } catch (IOException ex) {
                Logger.getLogger(ProfileManager.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

        return user;
    }

    public Address getAddress() {
        return this.address;
    }

    public String getLoginName() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        Principal principal = request.getUserPrincipal();

        try {
            return principal.getName();
        } catch (Exception ex) {
            ExternalContext extContext = FacesContext.getCurrentInstance().getExternalContext();
            System.err.println(extContext.getRequestContextPath());
            extContext.redirect(extContext.getRequestContextPath());
            return null;
        }
    }

    public String getGravatar() {
        String email;
        try {
            email = getLoginName();

        } catch (IOException ex) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
            return DEFAULT_GRAVATAR;
        }
        String url = Gravatar.getUrl(email, 60);

        return url;
    }

    public void updateUser() {
        try {
            userManager.updatePeople(user);
            userManager.updateAddress(address);

        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error: Update failed.");
            return;
        }
        MessagesController.addInfoMessage("Success", "Profile updated successfully.");
    }

  
    
}
