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

import io.hops.hopsworks.common.exception.DatasetException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.ProjectUtils;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FolderNameValidator {

  /**
   * Check if the given String is a valid folder name.
   * <p/>
   * @param name
   * @param subdir Indicates a directory under a top-level dataset
   */
  private static Pattern datasetNameRegex = Pattern.compile("^((?!__)[-_a-zA-Z0-9\\.]){1,87}[-_a-zA-Z0-9]$");
  private static Pattern subDirNameRegex = Pattern.compile("^((?!__)[-_a-zA-Z0-9 \\.]){0,87}[-_a-zA-Z0-9]$");

  public static void isValidName(String name, boolean subdir) throws DatasetException {
    if (name == null) {
      throw new IllegalArgumentException("Dataset name is null");
    }

    Matcher m;
    if (subdir) {
      m = subDirNameRegex.matcher(name);
    } else {
      m = datasetNameRegex.matcher(name);
    }

    if (!m.find()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NAME_INVALID, Level.FINE);
    }
  }

  private static Pattern projectNameRegexValidator = Pattern.compile(
      "^[a-zA-Z0-9]((?!__)[_a-zA-Z0-9]){0,61}[a-zA-Z0-9]$");

  /**
   * Check if the given String is a valid Project name.
   * <p/>
   * @param name
   */
  public static void isValidProjectName(ProjectUtils projectUtils, String name) throws ProjectException {
    if (name == null) {
      throw new IllegalArgumentException("Project name is null");
    }

    for (String reservedName : projectUtils.getReservedProjectNames()) {
      if (name.compareToIgnoreCase(reservedName) == 0) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.RESERVED_PROJECT_NAME, Level.FINE);
      }
    }
    Matcher m = projectNameRegexValidator.matcher(name);
    if (!m.find()) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.INVALID_PROJECT_NAME, Level.FINE);
    }
  }
}
