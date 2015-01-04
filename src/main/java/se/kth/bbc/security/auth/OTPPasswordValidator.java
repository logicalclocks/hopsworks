/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.auth;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 *
 * @author Ali Gholmai <gholami@pdc.kth.se>
 */
@FacesValidator("otpPasswordValidator")
public class OTPPasswordValidator implements Validator {

    /**
     * Ensure the password presented by user during registration is qualified.
     *
     * @param context
     * @param component
     * @param value
     * @throws ValidatorException
     */
    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String password = value.toString();

        // Fail if otp is not equal to 6 digits. 
        if (password == null || password.isEmpty() || password.length() < 6 || password.length() > 6) {
            FacesMessage facesMsg = new FacesMessage(
                    "PIN must be 6 charachters!");
            facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(facesMsg);

        }

        if (!isNumeric(password)) {

            FacesMessage facesMsg = new FacesMessage(
                    "PIN must be numeric!");
            facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(facesMsg);

        }

    }

    /**
     * Ensure that OTP password is only numbers
     *
     * @param s
     * @return
     */
    public boolean isNumeric(String s) {
        String pattern = "^[0-9]*$";
        return s.matches(pattern);
    }
}
