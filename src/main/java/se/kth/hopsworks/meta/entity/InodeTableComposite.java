package se.kth.hopsworks.meta.entity;

import java.io.Serializable;

/**
 * Represents a composite entity holding a table id and an inode primary key.
 * It's necessary when asking for the metadata of a specific file
 * <p/>
 * @author vangelis
 */
public class InodeTableComposite implements Serializable, EntityIntf {

  private static final long serialVersionUID = 1L;

  private int tableid;
  private int inodePid;
  private String inodeName;

  public InodeTableComposite() {
  }

  public InodeTableComposite(int tableid, int inodePid, String inodeName) {
    this.tableid = tableid;
    this.inodePid = inodePid;
    this.inodeName = inodeName;
  }

  public int getTableid() {
    return this.tableid;
  }

  public int getInodePid() {
    return this.inodePid;
  }

  public String getInodeName() {
    return this.inodeName;
  }

  public void setTableid(int tableid) {
    this.tableid = tableid;
  }

  public void setInodePid(int inodePid) {
    this.inodePid = inodePid;
  }

  public void setInodeName(String inodeName) {
    this.inodeName = inodeName;
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

  @Override
  public String toString() {
    return "se.kth.hopsworks.meta.entity.InodeTableComposite [ inodepid="
            + this.inodePid + ", inodename=" + this.inodeName + ", tableid="
            + this.tableid + "]";
  }

}
