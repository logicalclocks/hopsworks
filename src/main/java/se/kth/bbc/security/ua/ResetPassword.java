/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import com.google.common.net.UrlEscapers;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.validator.ValidatorException;
import javax.mail.MessagingException;
import se.kth.bbc.security.ua.model.People;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class ResetPassword {

    @EJB
    private UserManager mgr;
    
    @EJB
    private People people;
   
    @EJB
    private Email emailUtil;
    
    //TODO: This hsould be changed to a url and then enforcing the password for reset upon first login
    
    public void sendTmpPassword(String username, String password) throws MessagingException {

        people =  mgr.getUser(username);
        
        try {
            if (!SecurityUtils.converToSHA256(people.getSecurityAnswer()).equals(people.getSecurityAnswer())) {
                FacesMessage facesMsg = new FacesMessage(
                        "Wrong security question answer");
                facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(facesMsg);
            }
                
            String random_password = SecurityUtils.getRandomString();
            mgr.resetPassword(people.getUid(), random_password);
            
            String message = buildResetMessage(random_password);
            // sned the new password to the user email
            emailUtil.sendEmail(people.getEmail(), "reset password", message);
             
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ResetPassword.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Build a URL and send to user to reset their passwords
     * @param random_password
     * @return 
     */
    private String buildResetMessage(String random_password) {
        
        // TODO: make a url
        String urlFormat = random_password;
        return urlFormat;
    }
    
    
}
