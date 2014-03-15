/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
//@SessionScoped
@ManagedBean
@RequestScoped
public class UserMB {

    private Username user;
    @EJB
    private UserFacade userFacade;
    
    public Username getUser() {
        if (user == null) {
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            String userEmail = context.getUserPrincipal().getName();
            user = userFacade.findByEmail(userEmail);
        }
        return user;
    }

    public String getName() {
        return (getUser() != null) ? user.getName() : "null";
    }
    
    public boolean isUserAdmin() {
        return getRequest().isUserInRole("ADMIN");
    }

    public String logOut() {
        getRequest().getSession().invalidate();
        return "logout";
    }

    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
      
}
