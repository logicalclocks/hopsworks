/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.auth.AccountStatusErrorMessages;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class ResetPassword implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(ResetPassword.class.getName());

    private String username;
    private String passwd1;
    private String passwd2;
    private String current;
    private SecurityQuestion question;
    private User people;

    private String answer;
    
    private String notes;
    
    @EJB
    private UserManager mgr;

    @EJB
    private EmailBean emailBean;

    @Resource
    private UserTransaction userTransaction;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    
    public SecurityQuestion[] getQuestions() {
        return SecurityQuestion.values();
    }
    
    public SecurityQuestion getQuestion() {
        return question;
    }

    public void setQuestion(SecurityQuestion question) {
        this.question = question;
    }

    
    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public User getPeople() {
        return people;
    }

    public void setPeople(User people) {
        this.people = people;
    }

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

    public String sendTmpPassword() {

        people = mgr.getUser(this.username);

        try {

            if (!SecurityUtils.converToSHA256(answer).equals(people.getSecurityAnswer())) {

                MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.INVALID_SEQ_ANSWER);

                // Lock the account if 5 tmies wrong answer  
                int val = people.getFalseLogin();
                mgr.increaseLockNum(people.getUid(), val + 1);
                if (val > 5) {
                    mgr.deactivateUser(people.getUid());
                    return ("welcome");
                }
                return "";
            }

            // generate a radndom password
            String random_password = SecurityUtils.getRandomString();

            String mess = UserAccountsEmailMessages.buildPasswordResetMessage(random_password);

            userTransaction.begin();
            // make the account pending until it will be reset by user upon first login
            mgr.updateStatus(people, PeopleAccountStatus.ACCOUNT_PENDING.getValue());

            // reset the old password with a new one
            mgr.resetPassword(people, SecurityUtils.converToSHA256(random_password));

            userTransaction.commit();

            // sned the new password to the user email
            emailBean.sendEmail(people.getEmail(), UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, mess);

        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | MessagingException ex) {
            MessagesController.addSecurityErrorMessage("Technical Error!");
            return ("");
        } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException | SystemException | NotSupportedException ex) {
            MessagesController.addSecurityErrorMessage("Technical Error!");
            return ("");
        }

        return ("password_sent");
    }

    /**
     * Change password through profile.
     * @return
     */
    public String changePassword() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        if (req.getRemoteUser() == null) {
            return ("welcome");
        }

        people = mgr.getUser(req.getRemoteUser());

        if (people == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
            session.invalidate();
            return ("welcome");
        }

        if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
            MessagesController.addSecurityErrorMessage("Inactive Account");
            return "";
        }

        try {

            // Reset the old password with a new one
            mgr.resetPassword(people, SecurityUtils.converToSHA256(passwd1));

            mgr.updateStatus(people, PeopleAccountStatus.ACCOUNT_ACTIVE.getValue());

            // Send email    
            String message = UserAccountsEmailMessages.buildResetMessage();
            emailBean.sendEmail(people.getEmail(), UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);

            // logout user
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
            session.invalidate();
            return ("password_changed");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException ex) {
            MessagesController.addSecurityErrorMessage("Technical Error!");
            return ("");

        }
    }

    /**
     * Change security question in through profile.
     *
     * @return
     */
    public String changeSecQuestion() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        if (req.getRemoteUser() == null) {
            return ("welcome");
        }

        people = mgr.getUser(req.getRemoteUser());

        if (this.answer.isEmpty() || this.answer == null || this.current == null || this.current.isEmpty()) {
            MessagesController.addSecurityErrorMessage("No valid answer!");
            return ("");
        }

        if (people == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
            session.invalidate();
            return ("welcome");
        }

        // Check the status to see if user is not blocked or deactivate
        if (people.getStatus() == PeopleAccountStatus.ACCOUNT_BLOCKED.getValue()) {
            MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.BLOCKED_ACCOUNT);
            return "";
        }

        if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
            MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
            return "";
        }

        try {
            if (SecurityUtils.converToSHA256(this.current).equals(people.getPassword())) {

                // update the security question
                mgr.resetSecQuestion(people.getUid(), question, SecurityUtils.converToSHA256(this.answer));

                // send email    
                String message = UserAccountsEmailMessages.buildSecResetMessage();
                emailBean.sendEmail(people.getEmail(), UserAccountsEmailMessages.ACCOUNT_PROFILE_UPDATE, message);
                return ("sec_question_changed");
            } else {
                MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.INCCORCT_CREDENTIALS);
                return "";
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException ex) {
            MessagesController.addSecurityErrorMessage("Technical Error!");
            return ("");
        }

    }

    /**
     * Get the user security question.
     *
     * @return
     */
    public String findQuestion() {

        people = mgr.getUser(this.username);
        if (people == null) {
            MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.USER_NOT_FOUND);
            return "";
        }

        if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
            MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
            return "";
        }

        this.question = people.getSecurityQuestion();

        return ("reset_password");
    }

    
    public String deactivatedProfile(){
         FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        if (req.getRemoteUser() == null) {
            return ("welcome");
        }

        people = mgr.getUser(req.getRemoteUser());
 
        try {
            
            // check the deactivation reason length
            if(this.notes.length() <  5 || this.notes.length() > 500){
                    MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.INCCORCT_DEACTIVATION_LENGTH);
            }
        
            if (SecurityUtils.converToSHA256(this.current).equals(people.getPassword())) {
                
                // close the account
                mgr.closeUserAccount(people.getUid(), this.notes);
                // send email    
                String message = UserAccountsEmailMessages.buildSecResetMessage();
                emailBean.sendEmail(people.getEmail(), UserAccountsEmailMessages.ACCOUNT_DEACTIVATED, message);
             
 
            } else {
                MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.INCCORCT_PASSWORD);
                return "";
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException ex) {
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return logout();
    }
    
    
    
    public String changeProfilePassword() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

        if (req.getRemoteUser() == null) {
            return ("welcome");
        }

        people = mgr.getUser(req.getRemoteUser());

        if (people == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
            session.invalidate();
            return ("welcome");
        }

        // Check the status to see if user is not blocked or deactivate
        if (people.getStatus() == PeopleAccountStatus.ACCOUNT_BLOCKED.getValue()) {
            MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.BLOCKED_ACCOUNT);
            return "";
        }

        if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
            MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
            return "";
        }

        if (passwd1 == null || passwd2 == null) {
            MessagesController.addSecurityErrorMessage("No Password Entry!");
            return ("");
        }

        try {

            if (SecurityUtils.converToSHA256(current).equals(people.getPassword())) {

                // reset the old password with a new one
                mgr.resetPassword(people, SecurityUtils.converToSHA256(passwd1));

                // send email    
                String message = UserAccountsEmailMessages.buildResetMessage();
                emailBean.sendEmail(people.getEmail(), UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT, message);

                return ("profile_password_changed");
            } else {
                MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.INCCORCT_CREDENTIALS);
                return "";
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException ex) {
            MessagesController.addSecurityErrorMessage("Email Technical Error!");
            return ("");
        }
    }

    public String logout() {
        
        
        // Logout user
        FacesContext context = FacesContext.getCurrentInstance();
        
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();

        HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
      
        if (req.getRemoteUser() == null) {
            return ("welcome");
        }

        people = mgr.getUser(req.getRemoteUser());
        session.invalidate();
        
        mgr.setOnline(people.getUid(), -1);
        return ("welcome");
    }

}
