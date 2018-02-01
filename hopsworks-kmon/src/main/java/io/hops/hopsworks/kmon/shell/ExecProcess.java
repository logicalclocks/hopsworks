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

package io.hops.hopsworks.kmon.shell;

import java.io.File;
import java.util.List;

/**
 * See TaskEntity.java and TaskManager.java from Serengeti.
 */
public class ExecProcess implements Runnable {

  private volatile boolean finished;
  private final List<String> cmds;
  private String res;
  private File workingDir;

  public ExecProcess(List<String> cmds, File workingDir) {
    this.cmds = cmds;
    finished = false;
    this.workingDir = workingDir;
  }

  public void run() {

    try {
      Process process = new ProcessBuilder(cmds).directory(workingDir).
              redirectErrorStream(true).start();

//      Thread stdoutReaperThread = new Thread(new StreamReaper(proc.getInputStream(),
//            new File(taskEntity.getWorkDir(), TaskManager.STDOUT_FILENAME)));
//      Thread stderrReaperThread = new Thread(new StreamReaper(proc.getErrorStream(),
//            new File(taskEntity.getWorkDir(), TaskManager.STDERR_FILENAME)));                        
      int code = process.waitFor();
      res = "Command executed with exit code - " + code;
    } catch (Exception ex) {
      res = "Command execution failed - " + ex;
    } finally {
    }

  }

  public boolean isComplete() {
    return finished;
  }

}
