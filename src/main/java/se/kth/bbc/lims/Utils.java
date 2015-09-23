package se.kth.bbc.lims;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.json.Json;
import javax.json.stream.JsonParsingException;
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

  public static String getHdfsRootPath(String projectname) {
    return "/" + Constants.DIR_ROOT + "/" + projectname + "/";
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

  public static String ensurePathEndsInSlash(String path) {
    if (!path.endsWith(File.separator)) {
      return path + File.separator;
    }
    return path;
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
}
