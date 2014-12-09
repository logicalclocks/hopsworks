package se.kth.bbc.security.auth.totp;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.primefaces.context.RequestContext;
import se.kth.bbc.security.ua.AccountStatusIF;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.People;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class Gauth implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String ISSUER = "BiobankCloud";

    @EJB
    private UserManager mgr;

    private String username;
    private String password;
    private String otpCode;
    private People user;
    private int userid;

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

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    /**
     * Authenticate the users using two factor mobile authentication.
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public String login()
            throws NoSuchAlgorithmException, InvalidKeyException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        /* Log  out from the existing logged in user*/
        if (req.getRemoteUser() != null) {
            return logout();

        }

        user = mgr.getUser(username);
        if (user == null) {
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid username", null));
            return ("");
        }

        userid = user.getUid();

        try {
            // concatenate the static password with the otp due to limitations of passing two passwords to glassfish
            req.login(this.username, this.password + this.otpCode);
            // Reset the lock for failed accounts
            mgr.resetLock(userid);
            // Set the onlne flag
            mgr.setOnline(userid, 1);

        } catch (ServletException ex) {
            // if more than five times block the account
            int val = user.getFalseLogin();
            mgr.increaseLockNum(userid, val + 1);
            if (val > 5) {
                mgr.deactivateUser(userid);
            }

            // inform the use about invalid credentials
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid password", null));
            return ("");
        }
        
        if (user.getStatus()== AccountStatusIF.ACCOUNT_PENDING) {
            return ("reset");
        }

                
        if (user.getStatus()== AccountStatusIF.ACCOUNT_BLOCKED) {

            // inform the use about the blocked account
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Blocked account", null));
            return ("");
        }

        // go to welcome page
        return ("indexPage");
    }

    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpSession sess = (HttpSession) ctx.getExternalContext().getSession(false);

        if (null != sess) {
            sess.invalidate();
        }
        mgr.setOnline(userid, -1);
        return ("welcome");
    }

}
