/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.security.ua.model.People;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class PeopleStatusBean {

    @EJB
    private UserManager userName;

    private People user;

    public People getUser() {
        if (user == null) {
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            String userEmail = context.getUserPrincipal().getName();
            user = userName.findByEmail(userEmail);
        }
        return user;
    }

 
    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    public boolean isAnyAdminUser() {
        return getRequest().isUserInRole("BBC_ADMIN") || getRequest().isUserInRole("BBC_RESEARCHER");
    }
    
    public boolean isBBCAdmin() {
        return getRequest().isUserInRole("BBC_ADMIN");
    }

    public String logOut() {
        getRequest().getSession().invalidate();
        return "welcome";
    }

    
}
