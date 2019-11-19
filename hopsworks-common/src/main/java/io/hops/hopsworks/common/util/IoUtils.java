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

package io.hops.hopsworks.common.util;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

public class IoUtils {

  private static final Logger logger = Logger.getLogger(IoUtils.class.getName());

  public static String readContentFromClasspath(String path) throws IOException {
    URL url = IoUtils.class.getResource(path);// Resources.getResource(path);
    if (url == null) {
      throw new IOException("No config.props file found in cookbook");
    }
    return Resources.toString(url, Charsets.UTF_8);
  }

  public static String readContentFromPath(String path) throws IOException {
    return Files.toString(new File(path), Charsets.UTF_8);
  }

  public static List<String> readLinesFromClasspath(String url) throws
          IOException {
    return Resources.readLines(Resources.getResource(url), Charsets.UTF_8);
  }

  public static List<String> readLinesFromPath(String url) throws IOException {
    return Files.readLines(new File(url), Charsets.UTF_8);
  }

  public static List<String> readLinesFromWeb(String url) throws IOException {
    URL fileUrl = new URL(url);
    return Resources.readLines(fileUrl, Charsets.UTF_8);
  }

  public static String readContentFromWeb(String url) throws IOException {
    URL fileUrl = new URL(url);
    return Resources.toString(fileUrl, Charsets.UTF_8);
  }
}
