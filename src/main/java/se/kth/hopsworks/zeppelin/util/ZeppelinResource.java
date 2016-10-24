package se.kth.hopsworks.zeppelin.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.hopsworks.util.Settings;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfigFactory;

@Stateless
public class ZeppelinResource {

  private static final Logger logger
          = Logger.getLogger(ZeppelinResource.class.getName());

  @EJB
  private ProjectFacade projectBean;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private Settings settings;

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
              getGroup() + "-")) {
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

  public boolean isLivySessionAlive(int sessionId) {
    LivyMsg.Session session = getLivySession(sessionId);
    return session != null;
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
              getGroup() + "-")) {
        running = isProccessAlive(readPid(file));
        if (running) {
          forceKillProccess(readPid(file));
          break;
        }
      }
    }
    zeppelinConfFactory.removeFromCache(project.getName());
  }

  private FileObject[] getPidFiles(Project project) throws URISyntaxException,
          FileSystemException {
    ZeppelinConfiguration conf = zeppelinConfFactory.getprojectConf(
            project.getName()).getConf();
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

  public int deleteLivySession(int sessionId) {
    String livyUrl = settings.getLivyUrl();
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(livyUrl).path("/sessions/" + sessionId);
    Response res;
    try {
      res = target.request().delete();
    } catch (NotFoundException e) {
      return Response.Status.NOT_FOUND.getStatusCode();
    }finally {
      client.close();
    }
    return res.getStatus();
  }

  public LivyMsg.Session getLivySession(int sessionId) {
    String livyUrl = settings.getLivyUrl();
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(livyUrl).path("/sessions/" + sessionId);
    LivyMsg.Session session = null;
    try {
      session = target.request().get(LivyMsg.Session.class);
    } catch (NotFoundException e) {
      return null;
    }finally {
      client.close();
    }
    return session;
  }
  
  public LivyMsg getLivySessions() {
    String livyUrl = settings.getLivyUrl();
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(livyUrl).path("/sessions");
    LivyMsg livySession = null;
    try {
      livySession = target.request().get(LivyMsg.class);
    }finally {
      client.close();
    }
    return livySession;
  }
}
