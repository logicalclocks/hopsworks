package se.kth.hopsworks.meta.entity;

/**
 * Represents a path in the file system. Whenever there is metadata associated
 * to an inode in this path, then this inode has to be reinserted in the
 * metadata logs table. The front end sends a message request with this path
 * <p>
 * @author vangelis
 */
public class DirPath implements EntityIntf {

  private String path;

  public DirPath(String path) {
    this.path = path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPath() {
    return this.path;
  }

  public int getLength() {
    return this.path.length();
  }

  /**
   * Extracts the last directory name from the current path
   * <p>
   * @return
   */
  public String getParent() {
    //get the last two file separator positions
    String dirPart = se.kth.bbc.lims.Utils.getDirectoryPart(this.path);

    int lastSlash = dirPart.lastIndexOf("/");
    int secondLastSlash = dirPart.lastIndexOf("/", lastSlash - 1);

    return dirPart.substring(secondLastSlash + 1, lastSlash);
  }

  /**
   * Extracts the last directory name from the given path
   * <p>
   * @param path
   * @return
   */
  public String getParent(String path) {

    int dirIndex = path.indexOf(path);
    int previousSlash = path.lastIndexOf("/", dirIndex - 1);

    return path.substring(previousSlash + 1, dirIndex - 1);
  }

  /**
   * Returns the dir path without the file name, if there is one
   * 
   * @param path
   * @return 
   */
  public String getDirPart(String path) {

    String dirPart = se.kth.bbc.lims.Utils.getDirectoryPart(path);
    return dirPart.substring(0, dirPart.length() - 1);
  }

  /**
   * Returns the dir path until the given point.
   * 
   * @param part
   * @return 
   */
  public String getDirPartUntil(String part){
    
    int partIndex = this.path.indexOf(part);
    return this.path.substring(0, partIndex);
  }
  
  @Override
  public Integer getId() {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public void setId(Integer id) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public void copy(EntityIntf entity) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }
}
