package se.kth.bbc.lims;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author stig
 */
public final class Utils {

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
}
