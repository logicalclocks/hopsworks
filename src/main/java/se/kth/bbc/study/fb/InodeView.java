package se.kth.bbc.study.fb;

import java.util.Objects;

/**
 * Simplified version of the Inode entity to allow for easier access through web interface.
 * @author stig
 */
public final class InodeView {
  private final String name;
  private final boolean dir;
  private final boolean parent;
  private final String path;
  
  public InodeView(Inode i, String path){
    this.name = i.getInodePK().getName();
    this.dir = i.getDir();
    this.parent = false;
    this.path = path;
  }

  private InodeView(String name, boolean dir, boolean parent, String path) {
    this.name = name;
    this.dir = dir;
    this.parent = parent;
    this.path = path;
  }
  
  public static InodeView getParentInode(String path){
    String name = "..";
    boolean dir = true;
    boolean parent = true;
    int lastSlash = path.lastIndexOf("/");
    if(lastSlash == path.length()-1){
      lastSlash = path.lastIndexOf("/", lastSlash-1);
    }
    path = path.substring(0, lastSlash);
    return new InodeView(name,dir,parent,path);
  }

  public String getName() {
    return name;
  }

  public boolean isDir() {
    return dir;
  }

  public boolean isParent() {
    return parent;
  }

  public String getPath() {
    return path;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 13 * hash + Objects.hashCode(this.name);
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
    final InodeView other = (InodeView) obj;
    if (!Objects.equals(this.name, other.name)) {
      return false;
    }
    return Objects.equals(this.path, other.path);
  }
  
}
