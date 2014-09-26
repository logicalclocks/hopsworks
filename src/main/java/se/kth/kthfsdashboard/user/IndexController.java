package se.kth.kthfsdashboard.user;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Controller class for monitoring user requests and warning administrator of
 * new requests.
 *
 * @author stig
 */
@ManagedBean
@RequestScoped
public class IndexController implements Serializable {

    @EJB
    private UserFacade userFacade;

    /*
     Checks if there are new request and whether this should show in a dialog box (i.e. if the user is an admin).
     */
    public boolean checkForRequests() {
        if (getRequest().isUserInRole("ADMIN") || getRequest().isUserInRole("BBC_ADMIN")) {
            //return false if no requests
            return !(userFacade.findAllByStatus(Username.STATUS_REQUEST).isEmpty());
        }
        return false;
    }

    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
}
