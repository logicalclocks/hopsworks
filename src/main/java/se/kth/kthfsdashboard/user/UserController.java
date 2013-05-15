package se.kth.kthfsdashboard.user;

import java.security.Principal;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class UserController {


    public UserController() {

    }

    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void changePassword() {
        addMessage("Change Password not implemented!");
    }

    public void logout() {
        addMessage("Logout not implemented!");
    }    
    
    
    public String getLoginName() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        Principal principal = request.getUserPrincipal();

        return  principal.getName();
    }
    
}
