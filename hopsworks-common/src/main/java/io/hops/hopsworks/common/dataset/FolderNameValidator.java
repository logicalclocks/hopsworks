package io.hops.hopsworks.common.dataset;

import io.hops.hopsworks.common.constants.message.ResponseMessages;
import javax.validation.ValidationException;
import io.hops.hopsworks.common.util.Settings;

/**
 * Validator for folder names. A folder name is valid if:
 * <ul>
 * <li> It is not empty. </li>
 * <li> It is not longer than 24 characters.</li>
 * <li> It does not end with a dot.</li>
 * <li> It does not contain any of the disallowed characters space (only for Dataset and Project dirs), /, \, ?, *,
 * :, |, ', \", &lt;, &gt; >, %, (, ), &, ;, #, __</li>
 * </ul>
 * <p/>
 */
public class FolderNameValidator {

  /**
   * Check if the given String is a valid folder name.
   * <p/>
   * @param name
   * @param subdir Indicates a directory under a top-level dataset
   * @return
   * @throws ValidationException If the given String is not a valid folder name.
   */
  public static boolean isValidName(String name, boolean subdir) {
    String reason = "";
    boolean valid = true;
    if (name == null || name.isEmpty()) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_NOT_SET;
    } else if (name.length() > 88) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_TOO_LONG;
    } else if (name.endsWith(".")) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_ENDS_WITH_DOT;
    } else if (name.contains("" + Settings.DOUBLE_UNDERSCORE)) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_CONTAIN_DISALLOWED_CHARS
              + Settings.FILENAME_DISALLOWED_CHARS + Settings.DOUBLE_UNDERSCORE;
    } else {
      char[] disallowedChars = Settings.FILENAME_DISALLOWED_CHARS.toCharArray();
      if (subdir) {
        disallowedChars = Settings.SUBDIR_DISALLOWED_CHARS.toCharArray();
      }
      for (char c : disallowedChars ) {
        if (name.contains("" + c)) {
          valid = false;
          reason = ResponseMessages.FOLDER_NAME_CONTAIN_DISALLOWED_CHARS
                  + Settings.FILENAME_DISALLOWED_CHARS
                  + Settings.DOUBLE_UNDERSCORE;
        }
      }
    }
    if (!valid) {
      throw new ValidationException(reason);
    }
    return valid;
  }
  
  public static boolean isValidProjectName(String name, boolean subdir) {
    String reason = "";
    boolean valid = true;
    if (name == null || name.isEmpty()) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_NOT_SET;
    } else if (name.length() > 88) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_TOO_LONG;
    } else if (name.endsWith(".")) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_ENDS_WITH_DOT;
    } else if (name.contains("" + Settings.DOUBLE_UNDERSCORE)) {
      valid = false;
      reason = ResponseMessages.FOLDER_NAME_CONTAIN_DISALLOWED_CHARS
              + Settings.PRINT_PROJECT_DISALLOWED_CHARS + Settings.DOUBLE_UNDERSCORE;
    } else {
      char[] disallowedChars = Settings.PROJECT_DISALLOWED_CHARS.toCharArray();
      if (subdir) {
        disallowedChars = Settings.SUBDIR_DISALLOWED_CHARS.toCharArray();
      }
      for (char c : disallowedChars ) {
        if (name.contains("" + c)) {
          valid = false;
          reason = ResponseMessages.FOLDER_NAME_CONTAIN_DISALLOWED_CHARS + Settings.PRINT_PROJECT_DISALLOWED_CHARS
                  + Settings.DOUBLE_UNDERSCORE;
        }
      }
    }
    if (!valid) {
      throw new ValidationException(reason);
    }
    return valid;
  }
}
