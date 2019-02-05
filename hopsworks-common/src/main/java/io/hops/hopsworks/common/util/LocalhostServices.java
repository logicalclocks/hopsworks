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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalhostServices {

  private static final Logger logger = Logger.getLogger(LocalhostServices.class.getName());

  //Dela Certificates
  public static void generateHopsSiteKeystore(Settings settings, OSProcessExecutor osProcessExecutor,
      String userKeyPwd) throws IOException {
    
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(settings.getHopsSiteCaScript())
        .addCommand(userKeyPwd)
        .build();
    
    ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
    if (!processResult.processExited()) {
      throw new IOException("Generating Hops site keystore time-out");
    }
    int exitCode = processResult.getExitCode();
    if (exitCode != 0) {
      throw new IOException("stdout: " + processResult.getStdout() + "\nstderr: " + processResult.getStderr());
    }
  }
  //Dela Certificates end

  /**
   * We should NOT pass EJBs as method arguments!!!
   *
   * @param base - root directory for calculating disk usage for the subtree
   * @return the disk usage in Bytes for the subtree rooted at base.
   */
  public static String du(OSProcessExecutor osProcessExecutor, File base) throws IOException {

// This java implementation is like 100 times slower than calling 'du -sh'
//    long totalBytes = base.length();    
//    if (base.isDirectory()) {           
//      for (String child : base.list()) {      
//        File inode = new File(base, child);       
//        totalBytes += du(inode);                 
//      }
//    }
//    return totalBytes;

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("du")
        .addCommand("-sh")
        .addCommand(base.getCanonicalPath())
        .build();
    
    logger.log(Level.FINE, processDescriptor.toString());
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (!processResult.processExited()) {
        logger.log(Level.SEVERE, "Operation to calculate disk usage time-out");
        return "";
      }
      return processResult.getStdout();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Problem getting logs: {0}", ex.
          toString());
    }
    return "";
  }

}
