package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@FacesValidator("passwordValidator")
public class PasswordValidator implements Validator {

  final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=\\S+$).{6,}$";

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

    UIInput uiInputConfirmPassword = (UIInput) component.getAttributes()
            .get("confirmPassword");
    String confirmPassword = uiInputConfirmPassword.getSubmittedValue()
            .toString();

    if (password == null || password.isEmpty() || confirmPassword == null
            || confirmPassword.isEmpty()) {

      FacesMessage facesMsg = new FacesMessage(
              AccountStatusErrorMessages.PASSWORD_EMPTY);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);
    }

    if (password.length() < AuthenticationConstants.PASSWORD_MIN_LENGTH
            || password.length() > AuthenticationConstants.PASSWORD_MAX_LENGTH) {
      uiInputConfirmPassword.setValid(false);
      FacesMessage facesMsg = new FacesMessage(
              AccountStatusErrorMessages.PASSWORD_REQUIREMNTS);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);

    }

    if (!isAlphaNumeric(password)) {
      uiInputConfirmPassword.setValid(false);
      FacesMessage facesMsg = new FacesMessage(
              AccountStatusErrorMessages.PASSWORD_ALPAHNUMERIC);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);
    }

    if (!password.equals(confirmPassword)) {
      uiInputConfirmPassword.setValid(false);
      FacesMessage facesMsg = new FacesMessage(
              AccountStatusErrorMessages.PASSWORD_MISMATCH);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);
    }
  }

  /**
   * To check a string if it contains alphanumeric values: MyPassww132.
   *
   * @param s
   * @return
   */
  public boolean isAlphaNumeric(String s) {
    Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
    Matcher matcher = pattern.matcher(s);
    return matcher.matches();
  }
}
