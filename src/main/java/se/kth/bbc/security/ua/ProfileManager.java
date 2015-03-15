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
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.security.ua.model.Userlogins;
import se.kth.kthfsdashboard.user.Gravatar;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class ProfileManager implements Serializable {

    public static final String DEFAULT_GRAVATAR = "resources/images/icons/default-icon.jpg";

    private static final long serialVersionUID = 1L;
    @EJB
    private UserManager userManager;

    private User user;
    private Address address;
    private Userlogins login;
    
    public User getUser() {
        if (user == null) {
            try {
                user = userManager.findByEmail(getLoginName());
                address = userManager.findAddress(user.getUid());
                login = userManager.getLastUserLoing(user.getUid());
            } catch (IOException ex) {
                Logger.getLogger(ProfileManager.class.getName()).log(Level.SEVERE, null, ex);

                return null;
            }
        }

        return user;
    }

    public Userlogins getLogin() {
        return login;
    }

    public void setLogin(Userlogins login) {
        this.login = login;
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
            Logger.getLogger(ProfileManager.class.getName()).log(Level.SEVERE, null, ex);
            return DEFAULT_GRAVATAR;
        }
        String url = Gravatar.getUrl(email, 60);

        return url;
    }

       public List<String> getCurrentGroups() {
        List<String> list = userManager.findGroups(user.getUid());
        return list;
    }

    
    public void updateUserInfo(){

        if (userManager.updatePeople(user)) {
            MessagesController.addInfoMessage("Success", "Profile updated successfully.");
        } else {
            MessagesController.addSecurityErrorMessage("Update failed.");
            return;
        }
    
            userManager.updateAddress(address);
    }

      public void updateAddress(){

        if (userManager.updateAddress(address)) {
            MessagesController.addInfoMessage("Success", "Address updated successfully.");
        } else {
            MessagesController.addSecurityErrorMessage("Update failed.");
            return;
        }
    }

      
}
