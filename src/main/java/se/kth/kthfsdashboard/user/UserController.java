package se.kth.kthfsdashboard.user;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;
import java.util.List;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class UserController implements Serializable {
    private static final Logger logger = Logger.getLogger(UserController.class.getName());
    
    private static final long serialVersionUID = 1L;
    @EJB
    private UserFacade userFacade;
    private Username user;

       
    public UserController() {
    }

    public Username getUser() {
        if (user == null) {
            user = new Username();
        }
        return user;
    }

    public void setUser(Username user) {
        this.user = user;
    }

    public List<Username> getAllUsers() {
        return userFacade.findAll();
    }
    
    public Group[] groupValues() {
        return Group.values();
  }

    public String addUser() {
        user.encodePassword();
        user.setRegisteredOn(new Date());
        try {
            userFacade.persist(user);
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Add Operation failed.");
            return null;
        }
        addMessage("Add Operation Completed.");
        return "Success";
    }

    public String deleteUser() {
        try {
            userFacade.remove(user);
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Delete Operation failed.");
            return null;
        }

        addMessage("Delete Operation Completed.");
        return "Success";
    }

    public String updateUser() {
        try {
            userFacade.update(user);
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Update action failed.");
            return null;
        }
        addMessage("Update Completed.");
        return "Success";
    }

//    public void create() {
//        Username u = new Username();
//        u.setEmail("roshan@kth.se");
//        u.setMobileNum("022");
//        u.setName("Roshan Sedar");
//        u.setPassword("roshan");
//        u.setRegisteredOn(new Date());
//        u.setSalt("011".getBytes());
//        u.setUsername("admin");
//        List<Group> g = new ArrayList<Group>();
//        g.add(Group.USER);
//        u.setGroups(g);
//        userFacade.persist(u);
//    }
    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void addErrorMessageToUserAction(String message) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }

    public void changePassword() {
        addMessage("Change Password not implemented!");
    }

    public void logout() {
        addMessage("Logout not implemented!");
    }
    
    public String userManagement(){
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        Principal principal = request.getUserPrincipal();
       
        if(request.isUserInRole("BBC_ADMIN")){
            return "bbc/lims/services.xhtml?faces-redirect=true";
        }else{
            addErrorMessageToUserAction("Operation is not allowed: " + principal.getName() + " is not a privileged user to perform this action.");
            return "Failed";
        }
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
}
