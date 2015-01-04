/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import se.kth.bbc.security.ua.model.People;

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

    private String answer;
    private String question;
    private People people;
    @EJB
    private UserManager mgr;
    @EJB
    private EmailBean emailBean;
    private SelectSecurityQuestionMenue secMgr;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public People getPeople() {
        return people;
    }

    public void setPeople(People people) {
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

    /**
     *
     * @return
     */
    public String sendTmpPassword() {

        people = mgr.getUser(this.username);

        try {

            if (!SecurityUtils.converToSHA256(answer).equals(people.getSecurityAnswer())) {

                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Wrong answer", null));

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

            String mess = buildPasswordResetMessage(random_password);

            // sned the new password to the user email
            emailBean.sendEmail(people.getEmail(), "BBC Password Reset", mess);

            // make the account pending until it will be reset by user upon first login
            mgr.updateStatus(people.getUid(), AccountStatus.ACCOUNT_PENDING);

            // reset the old password with a new one
            mgr.resetPassword(people.getUid(), SecurityUtils.converToSHA256(random_password));

        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | MessagingException ex) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Technical Error to reset password!", "null"));
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
            return ("");
        }

        return ("password_sent");
    }

    private String buildPasswordResetMessage(String random_password) {

        String content = "Greetings!\n\n"
                + "There have been a password reset request on your behalf.\n\nPlease use the temporary password"
                + " sent to you as below. You will be required to change your passsword when you login first time.\n\n";

        String tmp_pass = "Password:" + random_password + "\n\n\n";
        String ending = "If you have any questions please contact support@biobankcloud.com";

        return content + tmp_pass + ending;
    }

    /**
     * Construct a message for security question change
     *
     * @param random_password
     * @return
     */
    private String buildSecResetMessage() {

        String l1 = "Greetings!\n\nThere have been a security question change reset request on your behalf.\n\n";
        String l2 = "Your security question is changed successfully\n\n\n";
        String l3 = "If you have any questions please contact support@biobankcloud.com";

        return l1 + l2 + l3;
    }

    /**
     * Construct message for profile password change
     *
     * @return
     */
    private String buildResetMessage() {

        String l1 = "Greetings!\n\n"
                + "There have been a password change reset request on your behalf.\n\n";
        String l2 = "Your password is changed successfully\n\n\n";
        String l3 = "If you have any questions please contact support@biobankcloud.com";

        return l1 + l2 + l3;
    }

    /**
     *
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

        try {
            // reset the old password with a new one
            mgr.resetPassword(people.getUid(), SecurityUtils.converToSHA256(passwd1));

            // make the account active until it will be reset by user upon first login
            mgr.updateStatus(people.getUid(), AccountStatus.ACCOUNT_ACTIVE);

            // send email    
            String message = buildResetMessage();
            emailBean.sendEmail(people.getEmail(), "BBC Password Reset", message);

            // logout user
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
            session.invalidate();
            return ("password_changed");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException ex) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Technical Error to reset password!", "null"));
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
            return ("");

        }
    }

    /**
     * Change security question in through profile
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
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "No entry", "null"));
            return ("");
        }

        if (people == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
            session.invalidate();
            return ("welcome");
        }

        try {
            if (SecurityUtils.converToSHA256(this.current).equals(people.getPassword())) {

                // update the security question
                mgr.resetSecQuestion(people.getUid(), this.question, SecurityUtils.converToSHA256(this.answer));

                // send email    
                String message = buildSecResetMessage();
                emailBean.sendEmail(people.getEmail(), "BBC Profile Update", message);
                this.answer ="";
                return ("sec_question_changed");
            } else {
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Wrong password!", "null"));
                return "";
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException ex) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Technical Error to reset!", "null"));
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
            return ("");
        }

    }

    /**
     * Get the user security question
     *
     * @return
     */
    public String findQuestion() {

        people = mgr.getUser(this.username);
        if (people == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not found!", "null"));
            return "";
        }

        String quest = people.getSecurityQuestion();
        secMgr = new SelectSecurityQuestionMenue();
        this.question = secMgr.getUserQuestion(quest);
        
        return ("reset_password");
    }

    /**
     *
     * @return
     */
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

        if (passwd1 == null || passwd2 == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "No password entry", "null"));
            return ("");
        }

        try {

            if (SecurityUtils.converToSHA256(current).equals(people.getPassword())) {

                // reset the old password with a new one
                mgr.resetPassword(people.getUid(), SecurityUtils.converToSHA256(passwd1));

                // send email    
                String message = buildResetMessage();
                emailBean.sendEmail(people.getEmail(), "Password Reset", message);

                return ("profile_password_changed");
            } else {
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Wrong password!", "null"));
                return "";
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | MessagingException ex) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Technical Error to reset!", "null"));
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
            return ("");
        }
    }

}
