/*
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
 *
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
