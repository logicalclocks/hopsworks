package se.kth.hopsworks.controller;

import javax.validation.ValidationException;
import se.kth.hopsworks.util.Settings;

/**
 * Validator for folder names. A folder name is valid if:
 * <ul>
 * <li> It is not empty. </li>
 * <li> It is not longer than 24 characters.</li>
 * <li> It does not end with a dot.</li>
 * <li> It does not contain any of the disallowed characters space, /, \, ?, *,
 * :, |, ', \", &lt;, &gt; >, %, (, ), &, ;, #, __</li>
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
    } else if (name.contains("" + Settings.DOUBLE_UNDERSCORE)) {
      valid = false;
       reason = ResponseMessages.FOLDER_NAME_CONTAIN_DISALLOWED_CHARS
               + Settings.FILENAME_DISALLOWED_CHARS + Settings.DOUBLE_UNDERSCORE;
    }  else {
      for (char c : Settings.FILENAME_DISALLOWED_CHARS.toCharArray()) {
        if (name.contains("" + c)) {
          valid = false;
          reason = ResponseMessages.FOLDER_NAME_CONTAIN_DISALLOWED_CHARS
                  + Settings.FILENAME_DISALLOWED_CHARS + Settings.DOUBLE_UNDERSCORE;
        }
      }
    }
    if (!valid) {
      throw new ValidationException(reason);
    }
    return valid;
  }
}
