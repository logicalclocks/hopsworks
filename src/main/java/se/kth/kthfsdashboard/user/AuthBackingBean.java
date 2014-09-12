/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import se.kth.bbc.activity.UserGroupsController;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@ManagedBean
@RequestScoped
public class AuthBackingBean {

    private static Logger log = Logger.getLogger(AuthBackingBean.class.getName());
    private String username;
    private String password;
    @ManagedProperty(value="#{bbcViewController}")
    private BbcViewController views;

    
    @EJB
    private UserGroupsController userGroupsController;

    
    public AuthBackingBean() {
    }


    public String login() {

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        if (username.isEmpty()) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "Enter your username.");
            context.addMessage(null, msg);
            return null;
        }

        if (password.isEmpty()) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "Enter your password.");
            context.addMessage(null, msg);
            return null;
        }

        try {
            if (request.getRemoteUser() != null) {
                request.logout();
            }
            request.login(username, password);
        } catch (ServletException e) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "The username or password is incorrect.");
            context.addMessage(null, msg);
            return null;
        }

        //you can fetch user from database for authenticated principal and do some action  
        Principal principal = request.getUserPrincipal();
        log.log(Level.INFO, "Logging IN Authenticated user: {0}", principal.getName());

        // delete from USERS_GROUPS where USER like principal.getName();
        // Remove the user from all groups.
        userGroupsController.clearGroups(principal.getName());
                
        
        if (request.isUserInRole("BBC_ADMIN") || request.isUserInRole("BBC_RESEARCHER") || request.isUserInRole("ADMIN")) {
            return "/bbc/lims/index.xml?faces-redirect=true";            
//        } else if (request.isUserInRole("ADMIN")) {
//            return "/sauron/clusters.xml?faces-redirect=true";
        }
            return "/view.xml";
    }

    public String logout() {
//      TODO does not work correctly
        String result = "logout";
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getExternalContext().getRequest() == null) {
            return "/loginError.xml";
        }
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        Principal principal = request.getUserPrincipal();
        HttpSession s = request.getSession(false);
        log.log(Level.INFO, "Logging OUT Authenticated user: {0}", principal.getName());

        userGroupsController.clearGroups(principal.getName());
        if (s != null) {
            try {
                s.invalidate();
                request.logout();
            } catch (ServletException e) {
                log.log(Level.SEVERE, "Failed to logout user!", e);
                result = "/loginError.xml";
            } catch (Throwable e) {
                log.log(Level.SEVERE, "Throwable Exception when calling logout user! ", e.toString());
            }
        }
        return result;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BbcViewController getViews() {
        return views;
    }

    public void setViews(BbcViewController views) {
        this.views = views;
    }
    
}
