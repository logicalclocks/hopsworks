package se.kth.bbc.project;

import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import org.primefaces.validate.ClientValidator;
import se.kth.hopsworks.util.Settings;

/**
 *
 * @author stig
 */
@FacesValidator("newProjectNameValidator")
public class NewProjectNameValidator implements Validator, ClientValidator {

  /**
   * Check if the value is acceptable as a project name. This means:
   * <ul>
   * <li> It does not contain any of the characters '
   * ',/,\,?,*,:,|,',",<,>,%,(,),&,;,# </li>
   * <li> It has a limited length (24 characters?) </li>
   * <li> It does not have a trailing period. </li>
   * </ul>
   * <p/>
   * @param value The name whose validity to test.
   */
  @Override
  public void validate(FacesContext context, UIComponent component, Object value)
          throws ValidatorException {
    if (value == null || value.toString().isEmpty()) {
      throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Validation error", "Project name cannot be empty."));
    }
    String val = value.toString();
    if (val.length() > 24) {
      throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Too long",
              "The project name cannot be longer than 24 characters."));
    }
    String title = "Illegal character";
<<<<<<< HEAD
    for (char c : Settings.FILENAME_DISALLOWED_CHARS.toCharArray()) {
=======
    if (val.contains("" + "__")) {
        throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, title,
                "The project name cannot contain any of the characters "
                        + Constants.PRINT_FILENAME_DISALLOWED_CHARS + ". You used "
                        + "__."));
    }
    for (char c : Constants.FILENAME_DISALLOWED_CHARS.toCharArray()) {
>>>>>>> 0ec3772b790777acdf1ace75c132fa9b3eceb2b3
      if (val.contains("" + c)) {
        throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, title,
                "The project name cannot contain any of the characters "
                + Settings.PRINT_FILENAME_DISALLOWED_CHARS + ". You used "
                + (c == ' ' ? "space" : c) + "."));
      }
    }
    if (val.endsWith(".")) {
      throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Cannot end in '.'",
              "The project name cannot end in a period."));
    }
  }

  @Override
  public Map<String, Object> getMetadata() {
    return null;
  }

  @Override
  public String getValidatorId() {
    return "newProjectNameValidator";
  }
}
