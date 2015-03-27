package se.kth.bbc.lims;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author stig
 */
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

  public static String getDirectoryPart(String path) {
    int lastSlash = path.lastIndexOf("/");
    int startName = (lastSlash > -1) ? lastSlash + 1 : 0;
    return path.substring(0, startName);
  }

  public static String getMimeType(String filename) {
    HttpServletRequest hsr = (HttpServletRequest) FacesContext.
            getCurrentInstance().getExternalContext().getRequest();
    String type = hsr.getSession().getServletContext().getMimeType(filename);
    if (type == null) {
      return "text/plain";
    } else {
      return type;
    }
  }

  public static String getHdfsRootPath(String studyname) {
    return "/Projects/" + studyname + "/";
  }

  public static String getYarnUser() {
    String machineUser = System.getProperty("user.name");
    if (machineUser == null) {
      machineUser = Constants.DEFAULT_YARN_USER;
      logger.log(Level.WARNING,
              "Username not found in system properties, using default \""
              + Constants.DEFAULT_YARN_USER + "\"");
    }
    return machineUser;
  }
  
  public static String ensurePathEndsInSlash(String path){
    if(!path.endsWith(File.separator)){
      return path + File.separator;
    }else{
      return path;
    }
  }
}
