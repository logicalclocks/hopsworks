/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.auth.yubikey;

import com.yubico.base.Modhex;
import com.yubico.base.Pof;
import com.yubico.base.Token;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
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
    

    public String login() throws NoSuchAlgorithmException, InvalidKeyException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        if (req.getRemoteUser() != null) {
            return ("bbchome");
        }
        try {
            req.login(this.username, this.password);
        } catch (ServletException ex) {
            ctx.addMessage(null, new FacesMessage("Incorrect usernme/password"));
            return ("login_error");
        }

        user = mgr.getUser(username);
        int split = otpCode.length()- 32;
        final String pubid = new String(Modhex.decode(otpCode.substring(0, split)));

        if (user == null || !verifyPublicId(pubid)) {
            ctx.addMessage(null, new FacesMessage("Cannot find user."));
            return ("login_error");
        }
        try {
            /* verify the key */
            if (verifyOTP(otpCode.substring(split))) {
                ctx.addMessage(null, new FacesMessage("Failed login"));

                int val = user.getFalseLogin();
                user.setFalseLogin(val + 1);
                if (val > 5) {
                    umgr.deactivate(Integer.parseInt(yubikey.getYubidnum()));
                }
                return ("login_error");
            }
        } catch (NumberFormatException ex) {
            ctx.addMessage(null, new FacesMessage("Exception: " + ex.getMessage()));
            return ("login_error");
        }
        return ("bbchome");
    }

    public boolean verifyPublicId(String pubid) {

        String id ;
        Collection<Yubikey> yubi = mgr.getUser(username).getYubikeyCollection();
        for (Iterator<Yubikey> it = yubi.iterator(); it.hasNext();) {
            yubikey = it.next();
            if (yubikey.getActive()) {
                setYubikey(yubikey);
                id = yubikey.getPublicId();
                if(pubid.equals(id))
                    return true;
            }
        }
        return false;
    }

    public boolean verifyOTP(String otp) {

        byte[] key = yubikey.getAesSecret().getBytes();

        Token t;
        try {
            t = Pof.parse(otp, key);
        } catch (GeneralSecurityException e) {

            return false;
        }

        int sessionCounter = toInt(t.getSessionCounter());
        int seenSessionCounter = yubikey.getSessionUse();
        int scDiff = seenSessionCounter - sessionCounter;

        int sessionUse = t.getTimesUsed();
        int seenSessionUse = yubikey.getSessionUse();
        int suDiff = seenSessionUse - sessionUse;

        int hi = t.getTimestampHigh() & 0xff;
        int seenHi = yubikey.getHigh();
        int hiDiff = seenHi - hi;

        int lo = toInt(t.getTimestampLow());
        int seenLo = yubikey.getLow();
        int loDiff = seenLo - lo;
        if (scDiff > 0) {
            return false;
        }
        if (scDiff == 0 && suDiff > 0) {
            return false;
        }
        if (scDiff == 0 && suDiff == 0 && hiDiff > 0) {
            return false;	
        }
        if (scDiff == 0 && suDiff == 0 && hiDiff == 0 && loDiff > 0) {
            return false;
        }
        if (scDiff == 0 && suDiff == 0 && hiDiff == 0 && loDiff == 0) {
            return false;
        }

        umgr.updateLastSeen(Integer.parseInt(yubikey.getYubidnum()), sessionCounter, hi, lo, sessionUse);

        return true;
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

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public UserManager getMgr() {
        return mgr;
    }

    public void setMgr(UserManager mgr) {
        this.mgr = mgr;
    }

    public Yubikey getYubikey() {
        return yubikey;
    }

    public void setYubikey(Yubikey yubikey) {
        this.yubikey = yubikey;
    }

    private int toInt(byte[] arr) {
        int low = arr[0] & 0xff;
        int high = arr[1] & 0xff;
        return (int) (high << 8 | low);
    }

}