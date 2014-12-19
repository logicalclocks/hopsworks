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
import se.kth.bbc.security.auth.totp.Gauth;
import se.kth.bbc.security.auth.totp.QRCodeGenerator;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class UserRegistration implements Serializable {

    private static final Logger logger = Logger.getLogger(UserRegistration.class.getName());

    @EJB
    private UserManager mgr;
    @EJB
    private EmailBean emailBean;

    private String fname;
    private String lname;
    private String username;
    private String mail;
    private String mobile;
    private String org;
    private String orcid;
    private String security_question;
    private String security_answer;
    private String title;
    private String password;
    private String passwordAgain;
    private String address1;
    private String address2;
    private String address3;
    private String city;
    private String state;
    private String country;
    private String postalcode;

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalcode() {
        return postalcode;
    }

    public void setPostalcode(String postalcode) {
        this.postalcode = postalcode;
    }

    public String getSecurity_question() {
        return security_question;
    }

    public void setSecurity_question(String security_question) {
        this.security_question = security_question;
    }

    public String getSecurity_answer() {
        return security_answer;
    }

    public void setSecurity_answer(String security_answer) {
        this.security_answer = security_answer;
    }
    // Quick response code URL
    private String qrUrl = "Pass";
    private StreamedContent qrCode;

    public StreamedContent getQrCode() {
        return qrCode;
    }

    public void setQrCode(StreamedContent qrCode) {
        this.qrCode = qrCode;
    }

    public UserManager getMgr() {
        return mgr;
    }

    public void setMgr(UserManager mgr) {
        this.mgr = mgr;
    }

    public String getQrUrl() {
        return qrUrl;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }
    private String tel;

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
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

    public String getPasswordAgain() {
        return passwordAgain;
    }

    public void setPasswordAgain(String passwordAgain) {
        this.passwordAgain = passwordAgain;
    }

    
    /**
     * Registration of users with mobile devices.
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws IOException 
     */
    public String registerMobileUser() throws UnsupportedEncodingException, NoSuchAlgorithmException, IOException {

        try {
            
            /* generates a UNIX compliant account*/
            int uid = mgr.lastUserID() + 1;
            String otpSecret = SecurityUtils.calculateSecretKey();
            short yubikey = -1;
            username = mgr.register(fname, lname, mail, title, org, tel, orcid, uid,
                    SecurityUtils.converToSHA256(password), otpSecret, security_question,
                    SecurityUtils.converToSHA256(security_answer), AccountStatusIF.MOBILE_ACCOUNT_INACTIVE, yubikey);

            // register group
            mgr.registerGroup(uid, GroupsIf.BBC_GUEST);
           
            // create address entry
            mgr.registerAddress(uid);
            
            // generate qr code to be displayed to user
            qrCode = QRCodeGenerator.getQRCode(mail, Gauth.ISSUER, otpSecret);
            
            // notify user about the request
            emailBean.sendEmail(mail, "Mobile account request", buildMobileRequestMessage());
            // Reset the values
            fname = "";
            lname = "";
            mail = "";
            title = "";
            org = "";
            tel = "";
            orcid = "";
            security_answer = "";
            security_question = "";
            password = "";
            passwordAgain = "";

        } catch (NoSuchAlgorithmException | IOException | WriterException e) {
            logger.log(Level.INFO, "Error {0}", e.getMessage());
            return null;
        } catch (MessagingException ex) {
            Logger.getLogger(UserRegistration.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ("qrcode");
    }
   
    public String registerYubikey() throws UnsupportedEncodingException, NoSuchAlgorithmException, IOException, MessagingException {

        try {
            /* generates a UNIX compliant account*/
            int uid = mgr.lastUserID() + 1;

            short yubikey = 1;
            String otp ="-1";
             logger.info("Username: "+ password);    
            logger.info("Pass: "+ password);
            username = mgr.register(fname, lname, mail, title, org,
                    tel, orcid, uid, SecurityUtils.converToSHA256(password), otp, 
                    security_question, SecurityUtils.converToSHA256(security_answer), AccountStatusIF.YUBIKEY_ACCOUNT_INACTIVE, yubikey);

            mgr.registerGroup(uid, GroupsIf.BBC_GUEST);
            mgr.registerAddress(uid, address1, address2, address3, city, state, country, postalcode);
            mgr.registerYubikey(uid);
            emailBean.sendEmail(mail, "Yubikey Request", buildYubikeyRequestMessage());
            // Reset the values
            fname = "";
            lname = "";
            mail = "";
            title = "";
            org = "";
            tel = "";
            orcid = "";
            security_answer = "";
            security_question = "";
            password = "";
            passwordAgain = "";
            address1 = "";
            address2 = "";
            address3 = ""; 
            city ="";
            state ="";
            country ="";
            postalcode="";

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            logger.log(Level.INFO, "Error {0}", e.getMessage());
            return null;
        }
        return ("yubico");
    }
    
    private String buildYubikeyRequestMessage() {

        String l1 = "Greetings!\n\nThere have been a Yubikey account request your behalf.\n\n";
        String l2 = "You will receive a Yubikey within 48 hours to your address.\n\n\n";
        String l3 = "If you have any questions please contact support@biobankcloud.com";

        return l1 + l2 + l3;
    }

    private String buildMobileRequestMessage() {

        String l1 = "Greetings!\n\nThere have been a Mobile account request your behalf.\n\n";
        String l2 = "Your account will be activated within 48 after approval.\n\n\n";
        String l3 = "If you have any questions please contact support@biobankcloud.com";

        return l1 + l2 + l3;
    }

}
