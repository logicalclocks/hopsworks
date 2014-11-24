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
@FacesValidator("otpValidator")
public class OTPValidator implements Validator {

    
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


        // Let required="true" do its job.
        if (password == null || password.isEmpty()) {
            return;
        }

        if (password.length() < 6 || password.length()> 6) {
            throw new ValidatorException(new FacesMessage(
                    "Password must be at least 6 charachters!"));
        }
     
        if(!isNumeric(password)){
           throw new ValidatorException(new FacesMessage(
                    "Password must be only digits: 123456"));
        }
    }
        
    
    /**
     * Check if the otp is only numbers.
     * @param s
     * @return 
     */
    public boolean isNumeric(String s) {
        String pattern = "^[0-9]*$";
        if (s.matches(pattern)) {
            return true;
        }
        return false;
    }

}
