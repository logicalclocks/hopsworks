package se.kth.kthfsdashboard.user;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
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
public class UserController {
    
    @EJB
    private UserFacade userFacade;

       
    private Username user;
    
     
    public UserController() {
    }
    
    public Username getUser(){
        if(user == null) {
            user = new Username();
        }
            return user;
    }
    
    public void setUser(Username user){
        this.user=user;
    }
    
    public List<Username> getAllUsers(){
        return userFacade.findAll();
    }
    
    public String addUser(){
        try{
            userFacade.persist(user);
        }catch(EJBException ejb){
            addErrorMessageToUserAction("Error: Add Operation failed.");
            return null;
        }
            addMessage("Add Operation Completed.");
            return "Success";
    }
    
    
    public String deleteUser(){
        try{
            userFacade.remove(user);
        }catch(EJBException ejb){
            addErrorMessageToUserAction("Error: Delete Operation failed.");
            return null;
        }
            addMessage("Delete Operation Completed.");
            return "Success";
    }
    
    
    public String updateUser(){
        try{
            userFacade.update(user);
        }catch(EJBException ejb){
            addErrorMessageToUserAction("Error: Update action failed.");
            return null;
        }
            addMessage("Update Completed.");
            return "Success";
    }
    
    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    
    public void addErrorMessageToUserAction(String message){
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }
    
    public void changePassword() {
        addMessage("Change Password not implemented!");
    }

    public void logout() {
        addMessage("Logout not implemented!");
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
