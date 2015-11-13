package se.kth.bbc.project.fb;

import java.util.Date;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.util.Settings;

/**
 * Simplified version of the Inode entity to allow for easier access through web
 * interface.
 * <p/>
 * @author stig
 */
@XmlRootElement
public final class InodeView {

  private String name;
  private boolean dir;
  private boolean parent;
  private String path;
  private long size;
  private boolean shared;
  private String owningProjectName;
  private Date modification;
  private Date accessTime;
  private int id;
  private int parentId;
  private int template;
  private String description;
  private boolean status = true;
  private byte underConstruction;
  private String owner;
  private int permission;

  public InodeView() {
  }

  /**
   * Constructor for sub folders
   * <p/>
   * @param i
   * @param path
   */
  public InodeView(Inode i, String path) {
    this.name = i.getInodePK().getName();
    this.dir = i.isDir();
    this.id = i.getId();
    this.parentId = i.getInodePK().getParentId();
    this.size = i.getSize();
    //put the template id in the REST response
    this.template = i.getTemplate();
    this.underConstruction = i.getUnderConstruction();
    this.parent = false;
    this.path = path;
    this.modification = new Date(i.getModificationTime().longValue());
    this.accessTime = new Date(i.getAccessTime().longValue());
    // if it is a sub folder the status should be default true
    // this is used in the front-end to tell apart accepted and pending shared 
    // top level datasets. 
    this.status = true;
    this.owner = i.getHdfsUser().getUsername();
    this.permission = i.getPermission();
  }

  /**
   * Constructor for top level datasets.
   * <p/>
   * @param parent
   * @param ds
   * @param path
   */
  public InodeView(Inode parent, Dataset ds, String path) {
    this.name = ds.getInode().getInodePK().getName();
    this.parentId = parent.getId();
    this.dir = ds.getInode().isDir();
    this.id = ds.getInode().getId();
    this.size = ds.getInode().getSize();
    this.template = ds.getInode().getTemplate();
    this.underConstruction = ds.getInode().getUnderConstruction();
    this.parent = false;
    this.path = path;
    this.modification
            = new Date(ds.getInode().getModificationTime().longValue());
    this.accessTime = new Date(ds.getInode().getAccessTime().longValue());
    this.shared
            = (!parent.inodePK.getName().equals(ds.getProjectId().getName()));
    if (this.shared) {
      this.name = parent.inodePK.getName() + Settings.SHARED_FILE_SEPARATOR
              + this.name;
    }
    this.owningProjectName = parent.inodePK.getName();
    this.description = ds.getDescription();
    this.status = ds.getStatus();
    this.owner = ds.getInode().getHdfsUser().getUsername();
    this.permission = ds.getInode().getPermission();
  }

  private InodeView(String name, boolean dir, boolean parent, String path) {
    this.name = name;
    this.dir = dir;
    this.parent = parent;
    this.path = path;
    this.modification = null;
  }

  public static InodeView getParentInode(String path) {
    String name = "..";
    boolean dir = true;
    boolean parent = true;
    int lastSlash = path.lastIndexOf("/");
    if (lastSlash == path.length() - 1) {
      lastSlash = path.lastIndexOf("/", lastSlash - 1);
    }
    path = path.substring(0, lastSlash);
    return new InodeView(name, dir, parent, path);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDir(boolean dir) {
    this.dir = dir;
  }
  
  public void setParentId(int parentId){
    this.parentId = parentId;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setTemplate(int template) {
    this.template = template;
  }

  public void setParent(boolean parent) {
    this.parent = parent;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setModification(Date modification) {
    this.modification = modification;
  }

  public void setAccessTime(Date accessTime) {
    this.accessTime = accessTime;
  }

  public String getName() {
    return name;
  }

  public boolean isDir() {
    return dir;
  }

  public int getId() {
    return this.id;
  }

  public int getParentId(){
    return this.parentId;
  }
  
  public int getTemplate() {
    return this.template;
  }

  public boolean isParent() {
    return parent;
  }

  public String getPath() {
    return path;
  }

  public Date getModification() {
    return modification;
  }

  public Date getAccessTime() {
    return accessTime;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public boolean isShared() {
    return shared;
  }

  public void setShared(boolean shared) {
    this.shared = shared;
  }

  public String getOwningProjectName() {
    return owningProjectName;
  }

  public void setOwningProjectName(String owningProjectName) {
    this.owningProjectName = owningProjectName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean getStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public byte getUnderConstruction() {
    return underConstruction;
  }

  public void setUnderConstruction(byte underConstruction) {
    this.underConstruction = underConstruction;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public int getPermission() {
    return permission;
  }

  public void setPermission(int permission) {
    this.permission = permission;
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
