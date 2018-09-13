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

package io.hops.hopsworks.kmon.mysql;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

public class MySQLAccess implements Serializable {

  final static String USERNAME = "kthfs";
  final static String PASSWORD = "kthfs";
  final static String DATABASE = "kthfs";
  final static String BACKUP_FILENAME = "dashboard.sql";

  final static Logger logger = Logger.getLogger(MySQLAccess.class.getName());

  public StreamedContent getBackup() {
    List<String> command = new ArrayList<String>();
    command.add("mysqldump");
    command.add("--default_character_set=utf8");
    command.add("--single-transaction");
    command.add("-u" + USERNAME);
    command.add("-p" + PASSWORD);
    command.add(DATABASE);
    try {
      ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(
              true);
      Process process = builder.start();
//            process.waitFor();
      InputStream inputStream = process.getInputStream();
      StreamedContent backupContent = new DefaultStreamedContent(inputStream,
              "application/sql", BACKUP_FILENAME);
      return backupContent;
    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public boolean restore(UploadedFile file) {
    List<String> command = new ArrayList<String>();
    command.add("mysql");
    command.add("-u" + USERNAME);
    command.add("-p" + PASSWORD);
    command.add(DATABASE);
    try {
      InputStream inputStream = file.getInputstream();
      Process process = new ProcessBuilder(command).start();
      byte[] bytes = new byte[1024];
      int read;
      while ((read = inputStream.read(bytes)) != -1) {
        process.getOutputStream().write(bytes, 0, read);
      }
      inputStream.close();
      process.getOutputStream().flush();
      process.getOutputStream().close();
      process.waitFor();
      if (process.exitValue() == 0) {
        return true;
      }
      return false;
    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
      return false;
    }
  }
}
