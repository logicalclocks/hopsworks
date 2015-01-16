package se.kth.bbc.lims;

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
}
