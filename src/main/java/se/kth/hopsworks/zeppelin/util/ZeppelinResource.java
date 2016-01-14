package se.kth.hopsworks.zeppelin.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.zeppelin.server.ZeppelinSingleton;

@Stateless
public class ZeppelinResource {

  private static final Logger logger
          = Logger.getLogger(ZeppelinResource.class.getName());

  private final ZeppelinSingleton zeppelin = ZeppelinSingleton.SINGLETON;

  public ZeppelinResource() {
  }

  /**
   * Checks if an interpreter is running
   * can return false if pid file reading fails. 
   * <p/>
   * @param interpreter
   * @return
   */
  public boolean isInterpreterRunning(InterpreterSetting interpreter) {
    ZeppelinConfiguration conf = this.zeppelin.getConf();
    String binPath = conf.getRelativeDir("bin");
    FileObject[] pidFiles;
    try {
      pidFiles = getPidFiles();
    } catch (URISyntaxException | FileSystemException ex) {
      logger.log(Level.SEVERE, "Could not read pid files ", ex);
      return false;
    }
    boolean running = false;

    for (FileObject file : pidFiles) {
      if (file.getName().toString().contains(interpreter.getGroup())) {
        running = isProccessAlive(binPath + "/alive.sh", readPid(file));
        //in the rare case were there are more that one pid files for the same 
        //interpreter break only when we find running one
        if (running) { 
          break;
        }
      }
    }
    return running;
  }

  private FileObject[] getPidFiles() throws URISyntaxException,
          FileSystemException {
    ZeppelinConfiguration conf = this.zeppelin.getConf();
    URI filesystemRoot;
    FileSystemManager fsManager;
    String runPath = conf.getRelativeDir("run");//the string run should be a constant.
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
      pidFiles = fsManager.resolveFile(filesystemRoot.toString() + "/").
              getChildren();
    } catch (FileSystemException ex) {
      throw new FileSystemException("Directory not found: " + filesystemRoot.getPath(), ex.getMessage());
    }
    return pidFiles;
  }

  /**
   * sets up a notebook repo for the given project.
   * <p/>
   * @param project
   * @return
   */
  public NotebookRepo setupNotebookRepo(Project project) {
    ZeppelinConfiguration conf = zeppelin.getConf();
    Class<?> notebookStorageClass;
    NotebookRepo repo;
    try {
      notebookStorageClass = Class.forName(conf.getString(
              ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_STORAGE));
      Constructor<?> constructor = notebookStorageClass.getConstructor(
              ZeppelinConfiguration.class, Project.class);
      repo = (NotebookRepo) constructor.newInstance(conf, project);

    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException |
            InstantiationException | IllegalAccessException |
            IllegalArgumentException | InvocationTargetException ex) {
      logger.log(Level.SEVERE, "Could not instantiate notebook repo", ex);
      return null;
    }

    return repo;
  }

  private boolean isProccessAlive(String bashPath, String pid) {
    
    logger.log(Level.INFO, "Checking if Zeppelin Interpreter alive at: " + bashPath + "   with PID: " +  pid);
    
    ProcessBuilder pb = new ProcessBuilder(bashPath, pid);
    if (pid == null) {
      return false;
    }
    int exitValue;
    Map<String, String> env = pb.environment();
    env.put("SPARK_HOME","/srv/spark");
    try {
      Process p = pb.start();
      p.waitFor();
      exitValue = p.exitValue();
    } catch (IOException | InterruptedException ex) {

      logger.warning("Problem starting Zeppelin Interpreter: " + ex.toString());
      //if the pid file exists but we can not test if it is alive then
      //we answer true, b/c pid files are deleted when a process is killed.
      return true;
    }
    return exitValue == 0;
  }

  private String readPid(FileObject file) {
    //pid value can only be extended up to a theoretical maximum of 
    //32768 for 32 bit systems or 4194304 for 64 bit:
    byte[] pid = new byte[8];
    try {
      file.getContent().getInputStream().read(pid);
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
}
