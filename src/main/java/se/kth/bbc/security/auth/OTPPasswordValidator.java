package se.kth.bbc.security.auth;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import se.kth.bbc.lims.MessagesController;

/**
 *
 * @author Ali Gholmai <gholami@pdc.kth.se>
 */
@FacesValidator("otpPasswordValidator")
public class OTPPasswordValidator implements Validator {

    // pattenr for password validation
    private final String pattern = "^[0-9]*$";

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
            FacesMessage facesMsg = new FacesMessage(AccountStatusErrorMessages.INCORRECT_PIN);
            facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(facesMsg);
        }

        if (!isNumeric(password)) {
            FacesMessage facesMsg = new FacesMessage(AccountStatusErrorMessages.PIN_REQUIERMENTS);
            facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(facesMsg);

        }

    }

    /**
     * Ensure that OTP password is only numbers.
     *
     * @param s
     * @return
     */
    public boolean isNumeric(String s) {
        return s.matches(pattern);
    }
}
