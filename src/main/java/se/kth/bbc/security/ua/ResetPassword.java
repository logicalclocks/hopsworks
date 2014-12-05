/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.mail.MessagingException;
import se.kth.bbc.security.ua.model.People;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class ResetPassword implements Serializable{

    
    private static final long serialVersionUID = 1L;
   
    private static final Logger logger = Logger.getLogger(UserRegistration.class.getName());

    private String username;
    private String passwd1;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
    private String passwd2;
    private String answer;
    private String question;

    private People people;

    @EJB
    private UserManager mgr;
    
    @EJB
    private Email emailUtil;

    private SelectSecurityQuestionMenue secMgr;
    
    public People getPeople() {
        return people;
    }

    public void setPeople(People people) {
        this.people = people;
    }

    public Email getEmailUtil() {
        return emailUtil;
    }

    public void setEmailUtil(Email emailUtil) {
        this.emailUtil = emailUtil;
    }

    ;
   
    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswd1() {
        return passwd1;
    }

    public void setPasswd1(String passwd1) {
        this.passwd1 = passwd1;
    }

    public String getPasswd2() {
        return passwd2;
    }

    public void setPasswd2(String passwd2) {
        this.passwd2 = passwd2;
    }

    //TODO: This hsould be changed to a url and then enforcing the password for reset upon first login
    public void sendTmpPassword() throws MessagingException {

        if(this.username==null) {
        
                        FacesMessage facesMsg = new FacesMessage(
                        "Wrong username");
                facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(facesMsg);

        }
        people = mgr.getUser(this.username);
        try {
            if (!SecurityUtils.converToSHA256(answer).equals(people.getSecurityAnswer())) {
                FacesMessage facesMsg = new FacesMessage(
                        "Wrong security question answer");
                facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(facesMsg);
            }

            // reset the old password with a new one
            String random_password = SecurityUtils.getRandomString();
            mgr.resetPassword(people.getUid(), SecurityUtils.converToSHA256(random_password));

            // make the account pending until it will be reset by user upon first login
            mgr.updateStatus(people.getUid(), AccountStatusIF.ACCOUNT_PENDING);
            String message = buildResetMessage(random_password);
          
            // sned the new password to the user email
            emailUtil.sendEmail(people.getEmail(), "reset password", message);

        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Build a URL and send to user to reset their passwords
     *
     * @param random_password
     * @return
     */
    private String buildResetMessage(String random_password) {

        // TODO: make a url
        String urlFormat = random_password;
        return urlFormat;
    }

    public void changePassword() {

        people = mgr.getUser(username);
        try {
            // reset the old password with a new one
            mgr.resetPassword(people.getUid(), SecurityUtils.converToSHA256(passwd1));

            // make the account active until it will be reset by user upon first login
            mgr.updateStatus(people.getUid(), AccountStatusIF.ACCOUNT_ACTIVE);

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String findQuestion() {
        
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String,String> params = 
        fc.getExternalContext().getRequestParameterMap();
        this.username =  (String) params.get("username"); 
        String quest = mgr.getSecurityQuestion(this.username);
        secMgr = new SelectSecurityQuestionMenue();
        question = secMgr.getUserQuestion(quest);

        return "reset_password";
    }
    
}
