/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import se.kth.bbc.lims.MessagesController;

/**
 * Provides the controller for the Profile page view; interface to the model.
 * @author Stig
 */
@ManagedBean
@SessionScoped
public class ProfileController implements Serializable {

    private static final Logger logger = Logger.getLogger(UserController.class.getName());
    public static final String DEFAULT_GRAVATAR = "http://www.freeimages.com/assets/183003/1830029572/daisy-1445925-s.jpg";

    private static final long serialVersionUID = 1L;
    @EJB
    private UserFacade userFacade;
    private Username user;

    private String name;
    private String email;
    private String phoneNo;

    private String currentPwd;
    private String newPwd;

    public ProfileController() {

    }

    public Username getUser() {
        if (user == null) {
            try {
                user = userFacade.findByEmail(getLoginName());
            } catch (IOException ex) {
                //TODO: fix
                Logger.getLogger(ProfileController.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

        return user;
    }

    public void setUser(Username user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getCurrentPwd() {
        return currentPwd;
    }

    public void setCurrentPwd(String currentPwd) {
        this.currentPwd = currentPwd;
    }

    public String getNewPwd() {
        return newPwd;
    }

    public void setNewPwd(String newPwd) {
        this.newPwd = newPwd;
    }

    public String getLoginName() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        Principal principal = request.getUserPrincipal();

        try {
            return principal.getName();
        } catch (Exception ex) {
//            throw new RuntimeException("Not logged in");
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
            userFacade.update(user);
        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error: Update action failed.");
            return;
        }
        MessagesController.addInfoMessage("Success", "Profile updated successfully.");
    }

    public void changePasswordForm() {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("modal", true);
        options.put("draggable", false);
        options.put("resizable", false);
        options.put("contentHeight", 190);
        RequestContext.getCurrentInstance().openDialog("dialogs/changePassword", options, null);
    }

    public void changePassword() {
        user = getUser();
        String oldPwd = user.getPassword();
        String currentEncrypted = DatatypeConverter.printHexBinary(currentPwd.getBytes());
        logger.log(Level.INFO, currentPwd);
        
        if(oldPwd.compareTo(currentEncrypted)!= 0){
            //Wrong password            
            MessagesController.addErrorMessage("wrongPassword","Error","Wrong password.");
            return;
        }
        
        user.setPassword(newPwd);
        user.encodePassword();
        FacesMessage message;
        try {
            userFacade.update(user);
            message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Password updated.");

        } catch (EJBException ejb) {

            RequestContext.getCurrentInstance().closeDialog(null);
            //TODO: make more meaningful
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed update", "Password update failed.");
            logger.log(Level.SEVERE, "Failed password update");
        }
        RequestContext.getCurrentInstance().closeDialog(message);

    }

    public void cancelPwdChange() {
        RequestContext.getCurrentInstance().closeDialog(null);
    }

    public void onPwdChanged(SelectEvent event) {
        FacesMessage mess = (FacesMessage) (event.getObject());
        MessagesController.addMessage(mess);
    }

    public boolean isCorrectPwd(String pwd) {
        return user.getPassword().compareTo(pwd) == 0;
    }

}
