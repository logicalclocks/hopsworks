package se.kth.bbc.study.fb;

/**
 *
 * @author stig
 */
public class NavigationPath {

  private String top;
  private String path;

  public NavigationPath() {
  }

  public NavigationPath(String top, String path) {
    this.top = top;
    this.path = path;
  }

  public String getTop() {
    return top;
  }

  public void setTop(String top) {
    this.top = top;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

}
