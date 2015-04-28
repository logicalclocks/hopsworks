/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.UserAccountsEmailMessages;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.rest.AuthService;
import se.kth.hopsworks.users.BbcGroupFacade;
import se.kth.hopsworks.users.UserDTO;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.user.model.BbcGroup;
import se.kth.hopsworks.user.model.SecurityQuestions;
import se.kth.hopsworks.user.model.UserAccountStatus;
import se.kth.hopsworks.user.model.Users;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Stateless
public class UsersController {
    @EJB
    private UserFacade userBean;
    @EJB
    private UserValidator userValidator;
    @EJB
    private EmailBean emailBean;
    @EJB
    private BbcGroupFacade groupBean;
    
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void registerUser(UserDTO newUser) throws AppException{
        if (userValidator.isValidEmail(newUser.getEmail())
                && userValidator.isValidPassword(newUser.getChosenPassword(),
                        newUser.getRepeatedPassword())
                && userValidator.isValidsecurityQA(newUser.getSecurityQuestion(),
                        newUser.getSecurityAnswer())) {
            if (newUser.getToS()){
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.TOS_NOT_AGREED);
            }
            if (userBean.findByEmail(newUser.getEmail()) != null){
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.USER_EXIST);
            }

            int uid = userBean.lastUserID() + 1;
            String uname = Users.USERNAME_PREFIX + uid;
            List<BbcGroup> groups = new ArrayList<>();
            groups.add(groupBean.findByGroupName(BbcGroup.USER));

            Users user = new Users(uid);
            user.setUsername(uname);
            user.setEmail(newUser.getEmail());
            user.setFname(newUser.getFirstName());
            user.setLname(newUser.getLastName());
            user.setMobile(newUser.getTelephoneNum());
            user.setStatus(UserAccountStatus.ACCOUNT_INACTIVE.getValue());
            user.setSecurityQuestion(SecurityQuestions.getQuestion(newUser.getSecurityQuestion()));
            user.setPassword(DigestUtils.sha256Hex(newUser.getChosenPassword()));
            user.setSecurityAnswer(DigestUtils.sha256Hex(newUser.getSecurityAnswer()));
            user.setBbcGroupCollection(groups);

            userBean.persist(user);// this would use the clients transaction which is committed after save() has finished
        }
    }
    
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void recoverPassword(String email, String securityQuestion, String securityAnswer) throws AppException {
        if (userValidator.isValidEmail(email) && 
            userValidator.isValidsecurityQA(securityQuestion, securityAnswer)) {
            
            Users user = userBean.findByEmail(email);
            if (user == null) {
                throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                        ResponseMessages.USER_DOES_NOT_EXIST);
            }
            if (!user.getSecurityQuestion().getValue().equalsIgnoreCase(securityQuestion)
                    || !user.getSecurityAnswer().equals(DigestUtils.sha256Hex(securityAnswer))) {
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                        ResponseMessages.SEC_QA_INCORRECT);
            }

            String randomPassword = getRandomPassword(UserValidator.PASSWORD_MIN_LENGTH);
            try {
                String message = UserAccountsEmailMessages.buildTempResetMessage(randomPassword);
                emailBean.sendEmail(email,
                        UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);
                user.setPassword(DigestUtils.sha256Hex(randomPassword));
                userBean.update(user);
            } catch (MessagingException ex) {
                Logger.getLogger(AuthService.class.getName()).log(Level.SEVERE, "Could not send email: ", ex);
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                        ResponseMessages.EMAIL_SENDING_FAILURE);
            }
        }
    }
    
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void changePassword(String email, String oldPassword, String newPassword, String confirmedPassword) throws AppException {
        Users user = userBean.findByEmail(email);

        if (user == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    ResponseMessages.USER_WAS_NOT_FOUND);
        }
        if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.PASSWORD_INCORRECT);
        }
        if (userValidator.isValidPassword(newPassword, confirmedPassword)) {
            user.setPassword(DigestUtils.sha256Hex(newPassword));
            userBean.update(user);
        }
    }
    
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void changeSecQA(String email, String oldPassword, String securityQuestion, String securityAnswer) throws AppException {
        Users user = userBean.findByEmail(email);

        if (user == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    ResponseMessages.USER_WAS_NOT_FOUND);
        }
        if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.PASSWORD_INCORRECT);
        }

        if (userValidator.isValidsecurityQA(securityQuestion, securityAnswer)) {
            user.setSecurityQuestion(SecurityQuestions.getQuestion(securityQuestion));
            user.setSecurityAnswer(DigestUtils.sha256Hex(securityAnswer));
            userBean.update(user);
        }
    }
    
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public UserDTO updateProfile(String email, String firstName, String lastName, String telephoneNum) throws AppException {
        Users user = userBean.findByEmail(email);

        if (user == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    ResponseMessages.USER_WAS_NOT_FOUND);
        }
        if (firstName != null) {
            user.setFname(firstName);
        }
        if (lastName != null) {
            user.setLname(lastName);
        }
        if (telephoneNum != null) {
            user.setMobile(telephoneNum);
        }

        userBean.update(user);
        return new UserDTO(user);
    }
    
    private String getRandomPassword(int length) {
        String randomStr = UUID.randomUUID().toString();
        while (randomStr.length() < length) {
            randomStr += UUID.randomUUID().toString();
        }
        return randomStr.substring(0, length);
    }
}
