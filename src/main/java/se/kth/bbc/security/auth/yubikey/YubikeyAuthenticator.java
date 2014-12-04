package se.kth.bbc.security.auth.yubikey;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.People;
import se.kth.bbc.security.ua.model.Yubikey;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class YubikeyAuthenticator implements Serializable {

    private static final Logger logger = Logger.getLogger(YubikeyAuthenticator.class.getName());
    
    private final String YUBIKEY_USER_MARKER = "YUBIKEY_USER_MARKER";
    private static final long serialVersionUID = 1L;
        
    @EJB
    private UserManager mgr;

    @EJB
    private YubikeyManager umgr;

    private String username;
    private String password;
    private String otpCode;
    private Yubikey yubikey;
    private People user;
    
    
    /**
     * Login users based on the Yubico custom Realm.
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException 
     */

    public String login() throws NoSuchAlgorithmException, InvalidKeyException {
 
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        if (req.getRemoteUser() != null) {
            return ("bbchome");
        }
        try {
            // Put a marker for yubikey users
            // TODO: login should be done using yubico realm 
            // Send the password as password+otp+yubikey marker to glassfish custom realm
            req.login(this.username, this.password + this.otpCode + YUBIKEY_USER_MARKER);
        } catch (ServletException ex) {
            ctx.addMessage(null, new FacesMessage("Incorrect usernme/password"));
            return ("login_error");
        }

        return ("indexPage");
    }

    
    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpSession sess = (HttpSession) ctx.getExternalContext().getSession(false);

        if (null != sess) {
            sess.invalidate();
        }
     return ("logout");
    }

    public UserManager getMgr() {
        return mgr;
    }

    public void setMgr(UserManager mgr) {
        this.mgr = mgr;
    }

    public YubikeyManager getUmgr() {
        return umgr;
    }

    public void setUmgr(YubikeyManager umgr) {
        this.umgr = umgr;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public People getUser() {
        return user;
    }

    public void setUser(People user) {
        this.user = user;
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

    public Yubikey getYubikey() {
        return yubikey;
    }

    public void setYubikey(Yubikey yubikey) {
        this.yubikey = yubikey;
    }
}