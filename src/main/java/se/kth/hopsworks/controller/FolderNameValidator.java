package se.kth.hopsworks.controller;

import javax.validation.ValidationException;
import se.kth.bbc.lims.Constants;

/**
 * Validator for folder names. A folder name is valid if:
 * <ul>
 * <li> It is not empty. </li>
 * <li> It is not longer than 24 characters.</li>
 * <li> It does not end with a dot.</li>
 * <li> It does not contain any of the disallowed characters space, /, \, ?, *,
 * :, |, ', \", &lt;, &gt; >, %, (, ), &, ;, #</li>
 * </ul>
 * <p/>
 * @author Ermias
 */
public class FolderNameValidator {

  /**
   * Check if the given String is a valid folder name.
   * <p/>
   * @param name
   * @return
   * @throws ValidationException If the given String is not a valid folder name.
   */
  public static boolean isValidName(String name) {
    String reason = "";
    boolean valid = true;
    if (name == null || name.isEmpty()) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_NOT_SET;
    } else if (name.length() > 24) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_TOO_LONG;
    } else if (name.endsWith(".")) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_ENDS_WITH_DOT;
    } else {
      for (char c : Constants.FILENAME_DISALLOWED_CHARS.toCharArray()) {
        if (name.contains("" + c)) {
          valid = false;
          reason = ResponseMessages.FOLDER_NAME_CONTAIN_DISALLOWED_CHARS
                  + Constants.FILENAME_DISALLOWED_CHARS;
        }
      }
    }
    if (!valid) {
      throw new ValidationException(reason);
    }
    return valid;
  }
}
