package se.kth.meta.entity;

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
  
  public int getLength(){
    return this.path.length();
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
