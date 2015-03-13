package se.kth.bbc.security.ua;

import com.google.zxing.WriterException;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.mail.MessagingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.auth.CustomAuthentication;
import se.kth.bbc.security.auth.QRCodeGenerator;

/**
 * This class provides user registration functions to get the input through the
 * user registration GUIs and register the info in the database.
 * 
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class UserRegistration implements Serializable {

    @EJB
    private UserManager mgr;
    @EJB
    private EmailBean emailBean;

    @Resource
    private UserTransaction userTransaction;

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
    private boolean tos;

    private List<String> questions;
    
    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }
    


    public boolean isTos() {
        return tos;
    }

    public void setTos(boolean tos) {
        this.tos = tos;
    }

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
    
    // To send the user the QR code image
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

    @PostConstruct
    private void init()
    {
        questions = new ArrayList<>();
        for(SecurityQuestions value: SecurityQuestions.values()){
           questions.add(value.getValue());
       }
        

    }
    /**
     * Register new mobile users.
     *
     * @return
     */
    public String registerMobileUser() {

        try {

            String otpSecret = SecurityUtils.calculateSecretKey();
            short yubikey = -1;

            // Generates a UNIX compliant account
            int uid = mgr.lastUserID() + 1;

            // Register the new request in the platform
            userTransaction.begin();

            
            username = mgr.register(fname, lname, mail, title, org, tel, orcid, uid,
                    SecurityUtils.converToSHA256(password), otpSecret, SecurityQuestions.getQuestion(security_question).name(),
                    SecurityUtils.converToSHA256(security_answer), PeoplAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue(), yubikey);

            // Register group
            mgr.registerGroup(uid, BBCGroups.BBC_GUEST.getValue());

            // Create address entry
            mgr.registerAddress(uid);

            // Generate qr code to be displayed to user
            qrCode = QRCodeGenerator.getQRCode(mail, CustomAuthentication.ISSUER, otpSecret);

            userTransaction.commit();

            // Notify user about the request
            emailBean.sendEmail(mail, UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT, UserAccountsEmailMessages.buildMobileRequestMessage());

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
            tos = false;

        } catch (NotSupportedException | SystemException | NoSuchAlgorithmException | IOException | WriterException | MessagingException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException e) {
            MessagesController.addSecurityErrorMessage("Technical Error" );
            return ("");

        }
        return ("qrcode");
    }

    /**
     * Register new Yubikey users.
     *
     * @return
     */
    public String registerYubikey() {

        try {

            short yubikey = 1;

            String otp = "-1";

            // Generates a UNIX compliant account
            int uid = mgr.lastUserID() + 1;

            // Register the request in the platform
            userTransaction.begin();

            username = mgr.register(fname, lname, mail, title, org,
                    tel, orcid, uid, SecurityUtils.converToSHA256(password), otp,
                    SecurityQuestions.getQuestion(security_question).name(), SecurityUtils.converToSHA256(security_answer), PeoplAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue(), yubikey);

            mgr.registerGroup(uid, BBCGroups.BBC_GUEST.getValue());

            mgr.registerAddress(uid, address1, address2, address3, city, state, country, postalcode);
            mgr.registerYubikey(uid);

            userTransaction.commit();

            // Send email to the user to get notified about the account request
            emailBean.sendEmail(mail, UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT, UserAccountsEmailMessages.buildYubikeyRequestMessage());

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
            city = "";
            state = "";
            country = "";
            postalcode = "";
            tos = false;

        } catch (NotSupportedException | SystemException | NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException e) {
            MessagesController.addSecurityErrorMessage("Technical Error" );
            return ("");
        }
        return ("yubico");
    }
}
