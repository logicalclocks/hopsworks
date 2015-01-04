package se.kth.bbc.security.auth;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.primefaces.context.RequestContext;
import se.kth.bbc.security.ua.AccountStatus;
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.People;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class CustomAuthentication implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String ISSUER = "BiobankCloud";
    private final String YUBIKEY_USER_MARKER = "YUBIKEY_USER_MARKER";

    @EJB
    private UserManager mgr;

    
    @EJB
    private EmailBean emailBean;

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
        
        // return if username is wrong
        if (user == null) {
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid username", null));
            return ("");
        }

       // retrun if user is not Mobile user     
        if (user.getYubikeyUser()==1) {
             RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not valid Mobile user", null));
            return ("");
        
        }
    
        // return if user not activated
        if (user.getStatus() == AccountStatus.MOBILE_ACCOUNT_INACTIVE) {
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not activated", null));
            return ("");
        }

        // return if used is bloked
        if (user.getStatus()== AccountStatus.ACCOUNT_BLOCKED) {
            // inform the use about the blocked account
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Account is blocked", null));
            return ("");
        }

        // return if used is bloked
        if (user.getStatus()== AccountStatus.ACCOUNT_DEACTIVATED) {
            // inform the use about the blocked account
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Account is deactivaed", null));
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
                try {
                    emailBean.sendEmail(user.getEmail(), "Account blocked", accountBlockedMessage());
                } catch (MessagingException ex1) {
                    Logger.getLogger(CustomAuthentication.class.getName()).log(Level.SEVERE, null, ex1);
                }
                
            }

            // inform the use about invalid credentials
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid password", null));
            return ("");
        }
        
        // reset the password after first login
        if (user.getStatus()== AccountStatus.ACCOUNT_PENDING) {
            return ("reset");
        }

        
        // go to welcome page
        return ("indexPage");
    }

     public String yubikeyLogin()
            throws NoSuchAlgorithmException, InvalidKeyException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        /* Log  out from the existing logged in user*/
        if (req.getRemoteUser() != null) {
            return logout();

        }

        user = mgr.getUser(username);
        
        // return if username is wrong
        if (user == null) {
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid username", null));
            return ("");
        }

        // retrun if user is not Yubikey user     
        if (user.getYubikeyUser()!=1) {
             RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not valid Yubikey user", null));
            return ("");
        
        }
        
        // return if user not activated
        if (user.getStatus() == AccountStatus.YUBIKEY_ACCOUNT_INACTIVE) {
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not activated", null));
            return ("");
        }

        // return if used is bloked
        if (user.getStatus()== AccountStatus.ACCOUNT_BLOCKED) {
            // inform the use about the blocked account
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Account is blocked", null));
            return ("");
        }
        
        // return if used is bloked
        if (user.getStatus()== AccountStatus.ACCOUNT_DEACTIVATED) {
            // inform the use about the blocked account
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Account is deactivaed", null));
            return ("");
        }

        
        userid = user.getUid();

        try {
            // concatenate the static password with the otp due to limitations of passing two passwords to glassfish
            req.login(this.username, this.password + this.otpCode + this.YUBIKEY_USER_MARKER);
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
                try {
                    emailBean.sendEmail(user.getEmail(), "Account blocked", accountBlockedMessage());
                } catch (MessagingException ex1) {
                    Logger.getLogger(CustomAuthentication.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }

            // inform the use about invalid credentials
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid password", null));
            return ("");
        }
        
        // reset the password after first login
        if (user.getStatus()== AccountStatus.ACCOUNT_PENDING) {
            return ("reset");
        }
       
        // go to welcome page
        return ("indexPage");
    }

     /**
      * 
      * @return
      * @throws NoSuchAlgorithmException
      * @throws InvalidKeyException 
      */
     
     public String certificateLogin()
            throws NoSuchAlgorithmException, InvalidKeyException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        /* Log  out from the existing logged in user*/
        if (req.getRemoteUser() != null) {
            return logout();

        }

        user = mgr.getUser(username);
        
        // return if username is wrong
        if (user == null) {
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid username", null));
            return ("");
        }

        // retrun if user is not Yubikey user     
        if (user.getYubikeyUser()!=1) {
             RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not valid Yubikey user", null));
            return ("");
        
        }
        
        // return if user not activated
        if (user.getStatus() == AccountStatus.YUBIKEY_ACCOUNT_INACTIVE) {
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not activated", null));
            return ("");
        }

        // return if used is bloked
        if (user.getStatus()== AccountStatus.ACCOUNT_BLOCKED) {
            // inform the use about the blocked account
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Blocked account", null));
            return ("");
        }
        
        userid = user.getUid();

        try {
            // concatenate the static password with the otp due to limitations of passing two passwords to glassfish
            req.login(this.username, this.password + this.otpCode + this.YUBIKEY_USER_MARKER);
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
                try {
                    emailBean.sendEmail(user.getEmail(), "BBC Account", accountBlockedMessage());
                } catch (MessagingException ex1) {
                    Logger.getLogger(CustomAuthentication.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }

            // inform the use about invalid credentials
            RequestContext.getCurrentInstance().update("growl");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid password", null));
            return ("");
        }
        
        // reset the password after first login
        if (user.getStatus()== AccountStatus.ACCOUNT_PENDING) {
            return ("reset");
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

    
    private String  accountBlockedMessage() {
        String l1 = "Greetings!\n\n.Your account in the Biobankcloud is blocked due to frequent false login attempts.\n\n";
        String l2 = "If you have any questions please contact support@biobankcloud.com";
        return l1 + l2 ;
    }
}
