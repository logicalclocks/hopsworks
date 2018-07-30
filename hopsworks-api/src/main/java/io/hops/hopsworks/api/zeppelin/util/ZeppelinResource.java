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

package io.hops.hopsworks.api.zeppelin.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfig;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.zeppelin.ZeppelinInterpreterConfFacade;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.InterpreterSetting;

@Stateless
public class ZeppelinResource {

  private static final Logger logger = Logger.getLogger(ZeppelinResource.class.getName());

  @EJB
  private ProjectFacade projectBean;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private ZeppelinInterpreterConfFacade zeppelinInterpreterConfFacade;

  public ZeppelinResource() {
  }

  /**
   * Checks if an interpreter is running
   * can return false if pid file reading fails.
   * <p/>
   * @param interpreter
   * @param project
   * @return
   */
  public boolean isInterpreterRunning(InterpreterSetting interpreter,
          Project project) {
    FileObject[] pidFiles;
    try {
      pidFiles = getPidFiles(project);
    } catch (URISyntaxException | FileSystemException ex) {
      logger.log(Level.SEVERE, "Could not read pid files ", ex);
      return false;
    }
    boolean running = false;

    for (FileObject file : pidFiles) {
      if (file.getName().toString().contains("interpreter-" + interpreter.
              getName() + "-")) {
        running = isProccessAlive(readPid(file));
        //in the rare case were there are more that one pid files for the same 
        //interpreter break only when we find running one
        if (running) {
          break;
        }
      }
    }
    return running;
  }

  public void forceKillInterpreter(InterpreterSetting interpreter,
          Project project) {
    FileObject[] pidFiles;
    try {
      pidFiles = getPidFiles(project);
    } catch (URISyntaxException | FileSystemException ex) {
      logger.log(Level.SEVERE, "Could not read pid files ", ex);
      return;
    }
    boolean running = false;
    for (FileObject file : pidFiles) {
      if (file.getName().toString().contains("interpreter-" + interpreter.
              getName() + "-")) {
        running = isProccessAlive(readPid(file));
        if (running) {
          forceKillProccess(readPid(file));
          break;
        }
      }
    }
  }

  private FileObject[] getPidFiles(Project project) throws URISyntaxException,
          FileSystemException {
    ZeppelinConfig zepConf = zeppelinConfFactory.getProjectConf(project.getName());
    if(zepConf==null){
      return new FileObject[0];
    }
    ZeppelinConfiguration conf = zepConf.getConf();
    URI filesystemRoot;
    FileSystemManager fsManager;
    String runPath = conf.getRelativeDir("run");
    try {
      filesystemRoot = new URI(runPath);
    } catch (URISyntaxException e1) {
      throw new URISyntaxException("Not a valid URI", e1.getMessage());
    }

    if (filesystemRoot.getScheme() == null) { // it is local path
      try {
        filesystemRoot = new URI(new File(runPath).getAbsolutePath());
      } catch (URISyntaxException e) {
        throw new URISyntaxException("Not a valid URI", e.getMessage());
      }
    }
    FileObject[] pidFiles = null;
    try {
      fsManager = VFS.getManager();
//      pidFiles = fsManager.resolveFile(filesystemRoot.toString() + "/").
      pidFiles = fsManager.resolveFile(filesystemRoot.getPath()).getChildren();
    } catch (FileSystemException ex) {
      throw new FileSystemException("Directory not found: " + filesystemRoot.
              getPath(), ex.getMessage());
    }
    return pidFiles;
  }

  /**
   * Retrieves projectId from cookies and returns the project associated with
   * the id.
   *
   * @param request
   * @return
   */
  public Project getProjectNameFromCookies(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    String projectId = null;
    Integer pId;
    Project project;
    if (cookies != null) {
      for (int i = 0; i < cookies.length; i++) {
        if (cookies[i].getName().equals("projectID")) {
          projectId = cookies[i].getValue();
          break;
        }
      }
    }
    try {
      pId = Integer.valueOf(projectId);
      project = projectBean.find(pId);
    } catch (NumberFormatException e) {
      return null;
    }
    return project;
  }

  public Project getProject(String projectId) {
    Integer pId;
    Project proj;
    try {
      pId = Integer.valueOf(projectId);
      proj = projectBean.find(pId);
    } catch (NumberFormatException e) {
      return null;
    }
    return proj;
  }

  public boolean isJSONValid(String jsonInString) {
    Gson gson = new Gson();
    try {
      gson.fromJson(jsonInString, Object.class);
      return true;
    } catch (JsonSyntaxException ex) {
      return false;
    }
  }

  private boolean isProccessAlive(String pid) {
    String[] command = {"kill", "-0", pid};
    ProcessBuilder pb = new ProcessBuilder(command);
    if (pid == null) {
      return false;
    }

    //TODO: We should clear the environment variables before launching the 
    // redirect stdout and stderr for child process to the zeppelin/project/logs file.
    int exitValue;
    try {
      Process p = pb.start();
      p.waitFor();
      exitValue = p.exitValue();
    } catch (IOException | InterruptedException ex) {

      logger.log(Level.WARNING, "Problem testing Zeppelin Interpreter: {0}", ex.
              toString());
      //if the pid file exists but we can not test if it is alive then
      //we answer true, b/c pid files are deleted when a process is killed.
      return true;
    }
    return exitValue == 0;
  }

  private void forceKillProccess(String pid) {
    String[] command = {"kill", "-9", pid};
    ProcessBuilder pb = new ProcessBuilder(command);
    if (pid == null) {
      return;
    }
    try {
      Process p = pb.start();
      p.waitFor();
      p.exitValue();
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.WARNING, "Problem killing Zeppelin Interpreter: {0}", ex.
              toString());
    }
  }

  private String readPid(FileObject file) {
    //pid value can only be extended up to a theoretical maximum of 
    //32768 for 32 bit systems or 4194304 for 64 bit:
    byte[] pid = new byte[8];
    if (file == null) {
      return null;
    }
    try {
      file.getContent().getInputStream().read(pid);
      file.close();
    } catch (FileSystemException ex) {
      return null;
    } catch (IOException ex) {
      return null;
    }
    String s;
    try {
      s = new String(pid, "UTF-8").trim();
    } catch (UnsupportedEncodingException ex) {
      return null;
    }
    return s;
  }

  public void persistToDB(Project project) {
    if (project == null) {
      logger.log(Level.SEVERE, "Can not persist interpreter json for null project.");
      return;
    }
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getProjectConf(project.getName());
    if (zeppelinConf == null) {
      logger.log(Level.SEVERE, "Can not persist interpreter json for project.");
      return;
    }
    try {
      String s = readConfigFile(new File(zeppelinConf.getConfDirPath() + ZeppelinConfig.INTERPRETER_JSON));
      if (!isJSONValid(s)) {
        logger.log(Level.SEVERE, "Zeppelin interpreter json not valid. For project {0}", project.getName() );
        throw new IllegalStateException("Zeppelin interpreter json not valid.");
      }
      zeppelinInterpreterConfFacade.create(project, s);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }

  private String readConfigFile(File path) throws IOException {
    // read contents from file
    if (!path.exists()) {
      throw new IOException("File not found: " + path);
    }
    return new String(Files.readAllBytes(path.toPath()));
  }
}
