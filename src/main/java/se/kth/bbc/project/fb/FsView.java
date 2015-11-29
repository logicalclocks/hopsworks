package se.kth.bbc.project.fb;

import java.io.File;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public final class FsView {

  private String name;
  private String path;
  private boolean dir;

  public FsView() {
  }

  public FsView(String path, boolean dir) {
    this.name = new File(path).getName();
    this.dir = dir;
    this.path = path;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public boolean isDir() {
    return dir;
  }

  public void setDir(boolean dir) {
    this.dir = dir;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 13 * hash + Objects.hashCode(this.path);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final FsView other = (FsView) obj;
    return Objects.equals(this.path, other.path);
  }

}
