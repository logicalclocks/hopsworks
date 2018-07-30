/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
