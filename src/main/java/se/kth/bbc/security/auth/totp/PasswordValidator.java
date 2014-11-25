/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.auth.totp;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 *
 * @author Ali Gholmai <gholami@pdc.kth.se>
 */
@FacesValidator("passwordValidator")
public class PasswordValidator implements Validator {

    
    /**
     * Ensure the password presented by user during registration is qualified.
     * @param context
     * @param component
     * @param value
     * @throws ValidatorException 
     */
    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String password = value.toString();

        UIInput uiInputConfirmPassword = (UIInput) component.getAttributes()
                .get("confirmPassword");
        String confirmPassword = uiInputConfirmPassword.getSubmittedValue()
                .toString();

        // Let required="true" do its job.
        if (password == null || password.isEmpty() || confirmPassword == null
                || confirmPassword.isEmpty()) {
            return;
        }

        if (password.length() < 6) {
            uiInputConfirmPassword.setValid(false);
            throw new ValidatorException(new FacesMessage(
                    "Password must be at least 6 charachters!"));
        }

        if (isAlphaNumeric(password)) {
            uiInputConfirmPassword.setValid(false);
            throw new ValidatorException(new FacesMessage(
                    "Password must be letters, numbers, and non-alphanumeric"));
        }

        if (!password.equals(confirmPassword)) {
            uiInputConfirmPassword.setValid(false);
            throw new ValidatorException(new FacesMessage(
                    "Passwords don't match!"));
        }

    }

    /**
     * To check a string if it contains alphanumeric values: MyPassww232¤!#.
     * @param s
     * @return 
     */
    public boolean isAlphaNumeric(String s) {
        String pattern = "^[a-zA-Z0-9]*$";
        if (s.matches(pattern)) {
            return true;
        }
        return false;
    }
}
