/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import com.google.zxing.WriterException;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.security.auth.CustomAuthentication;
import se.kth.bbc.security.auth.QRCodeGenerator;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class RecoverySelector implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @EJB
    private UserManager um;

    @EJB
    private EmailBean email;

    private User people;

    private String console;

    // Quick response code URL
    private String qrUrl = "Pass";
    private StreamedContent qrCode;

    //@ManagedProperty(value="#{recoverySelector.uname}")
    private String uname;
    private String tmpCode;
    private String passwd;

        public String getQrUrl() {
        return qrUrl;
    }

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    public StreamedContent getQrCode() {
        return qrCode;
    }

    public void setQrCode(StreamedContent qrCode) {
        this.qrCode = qrCode;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getTmpCode() {
        return tmpCode;
    }

    public void setTmpCode(String tmpCode) {
        this.tmpCode = tmpCode;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getConsole() {
        return console;
    }

    public void setConsole(String console) {
        this.console = console;
    }

    public String redirect() {

        if (console.equals("Password")) {
            return "sec_question";
        }

        if (console.equals("Mobile")) {
            return "mobile_recovery";
        }

        if (console.equals("Yubikey")) {
            return "yubikey_recovery";
        }

        return "";
    }


    public String sendQrCode() {

        people = um.getUser(this.uname);

        if (people == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not found!", "null"));
            return "";
        }

        
        if (people.getStatus() == PeoplAccountStatus.ACCOUNT_BLOCKED.getValue() ) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Account is blocked!", "null"));
            return "";
        }
        if (people.getYubikeyUser() == 1) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "No Mobile user found", "null"));
            return "";
        }

        try {

            if (people.getPassword().equals(SecurityUtils.converToSHA256(passwd))) {

                String random = SecurityUtils.getRandomString();
                um.updateSecret(people.getUid(), random);
                String message = buildTempResetMessage(random);
                email.sendEmail(people.getEmail(), "BBC Temporary Code", message);

                return "validate_code";
            } else {
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Wrong password!", "null"));
                return "";
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException ex) {
            Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";
    }

    public String validateTmpCode() {

        people = um.getUser(this.uname);

        if (people == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not found!", "null"));
            return "";
        }

        if (people.getSecret() == null ? tmpCode == null : people.getSecret().equals(this.tmpCode)) {

            try {
                String otpSecret = SecurityUtils.calculateSecretKey();

                um.updateSecret(people.getUid(), otpSecret);
                qrCode = QRCodeGenerator.getQRCode(people.getEmail(), CustomAuthentication.ISSUER, otpSecret);
                return "qrcode";

            } catch (IOException | WriterException ex) {
                Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            int val = people.getFalseLogin();
            um.increaseLockNum(people.getUid(), val + 1);
            if (val > 5) {
                um.deactivateUser(people.getUid());
                try {
                    email.sendEmail(people.getEmail(), "Account blocked", accountBlockedMessage());
                } catch (MessagingException ex1) {
                    Logger.getLogger(CustomAuthentication.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }

            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Wrong password!", "null"));
            return "";
        }
        return "";
    }

    public String sendYubiReq() {

        people = um.getUser(this.uname);

        if (people == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not found!", "null"));
            return "";
        }

        if (people.getStatus() == PeoplAccountStatus.ACCOUNT_BLOCKED.getValue() ) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Account is blocked!", "null"));
            return "";
        }

        if (people.getYubikeyUser() != 1) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "No Yubikey user found", "null"));
            return "";
        }

        try {
            if (people.getPassword().equals(SecurityUtils.converToSHA256(passwd))) {

                String message = buildYubResetMessage();
                email.sendEmail(people.getEmail(), "Yubikey Request", message);
                people.setStatus(PeoplAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue());
                um.updatePeople(people);
                return "yubico";
            } else {

                int val = people.getFalseLogin();
                um.increaseLockNum(people.getUid(), val + 1);
                if (val > 5) {
                    um.deactivateUser(people.getUid());
                    try {
                        email.sendEmail(people.getEmail(), "BBC Account Blocked", accountBlockedMessage());
                    } catch (MessagingException ex1) {
                        Logger.getLogger(CustomAuthentication.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Wrong password!", "null"));
                return "";
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException ex) {
            Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";

    }

    private String accountBlockedMessage() {
        String l1 = "Greetings!\n\n"
                + "Your account in the Biobankcloud is blocked due to frequent false attempts.\n\n";
        String l2 = "If you have any questions please contact support@biobankcloud.com";
        return l1 + l2;
    }
    
    private String buildTempResetMessage(String random_password) {

        String content = "Greetings!\n\n"
                + "There have been a mobile device reset request on your behalf.\n\n"
                + "Please use the temporary password"
                + "sent to you as below. You need to validate the code to get a new setup.\n\n";

        String tmp_pass = "Code:" + random_password + "\n\n\n";
        String ending = "If you have any questions please contact support@biobankcloud.com";

        return content + tmp_pass + ending;
    }

    private String buildYubResetMessage() {

        String content = "Greetings!\n\n"
                + "There have been a Yubikey device reset request on your behalf.\n\n"
                + "You will receive a device within 48 hours.\n\n";

        String ending = "If you have any questions please contact support@biobankcloud.com";

        return content + ending;
    }

}
