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

package io.hops.hopsworks.common.hdfs;

import com.google.common.base.CharMatcher;
import io.hops.hopsworks.common.util.Settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonParsingException;
import org.apache.hadoop.fs.Path;

public final class Utils {

  private static final Logger logger = Logger.getLogger(Utils.class.getName());

  public static String getFileName(String path) {
    int lastSlash = path.lastIndexOf("/");
    int startName = (lastSlash > -1) ? lastSlash + 1 : 0;
    return path.substring(startName);
  }

  public static String getExtension(String filename) {
    int lastDot = filename.lastIndexOf(".");
    if (lastDot < 0) {
      return "";
    } else {
      return filename.substring(lastDot);
    }
  }

  public static String stripExtension(String filename) {
    int lastDot = filename.lastIndexOf(".");
    if (lastDot < 0) {
      return filename;
    } else {
      return filename.substring(0, lastDot);
    }
  }

  public static String getDirectoryPart(String path) {
    int lastSlash = path.lastIndexOf("/");
    int startName = (lastSlash > -1) ? lastSlash + 1 : 0;
    return path.substring(0, startName);
  }

  public static String getProjectPath(String projectname) {
    return "/" + Settings.DIR_ROOT + "/" + projectname + "/";
  }

  public static String ensurePathEndsInSlash(String path) {
    if (!path.endsWith(File.separator)) {
      return path + File.separator;
    }
    return path;
  }

  /**
   * The root '/' is considered '0', so the answer is incorrect for root, but
   * that doesn't matter. '/blah.txt' should return '1'.
   *
   * @param path
   * @return
   */
  public static int pathLen(String path) {
    return CharMatcher.is('/').countIn(path);
  }

  /**
   * Checks if a given file contains actual json content.
   * <p/>
   * @param pathName
   * @return
   * @throws FileNotFoundException
   */
  public static boolean checkJsonValidity(String pathName) throws
          FileNotFoundException {

    String fileContent = Utils.getFileContents(pathName);

    if (fileContent == null) {
      return false;
    }

    try {
      //check if the file content is actually a json string
      Json.createReader(new StringReader(fileContent)).readObject();
    } catch (JsonParsingException e) {
      return false;
    }

    return true;
  }

  public static String getFileContents(String filePath) throws
          FileNotFoundException {
    File file = new File(filePath);
    Scanner scanner = new Scanner(file);

    //check if the file is empty
    if (!scanner.hasNext()) {
      return null;
    }

    //fetch the whole file content at once
    return scanner.useDelimiter("\\Z").next();
  }
  
  /**
   * take a String corresponding to a file uri and return only the path part of the it.
   * And transform shared path into there full path.
   * @param path the string from which to extract the path
   * @return the path
   * @throws UnsupportedEncodingException 
   */
  public static String prepPath(String path) throws UnsupportedEncodingException {
    Path p;
    String result = path;
    if (path.contains(Settings.SHARED_FILE_SEPARATOR)) {
      String[] parts = path.split(Settings.SHARED_FILE_SEPARATOR);
      p = new Path(parts[0]);
      //remove hdfs://10.0.2.15:8020//
      p = new Path(URLDecoder.decode(p.toUri().getRawPath(), StandardCharsets.UTF_8.toString()));
      result = p.toString() + Settings.SHARED_FILE_SEPARATOR + parts[1];
    } else {
      p = new Path(path);
      //remove hdfs://10.0.2.15:8020//
      p = new Path(URLDecoder.decode(p.toUri().getRawPath(), StandardCharsets.UTF_8.toString()));
      result = p.toString();
    }
    return result;
  }
}
