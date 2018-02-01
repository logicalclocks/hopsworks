/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
