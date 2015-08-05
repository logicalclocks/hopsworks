package se.kth.meta.entity;

import java.io.Serializable;

/**
 * Represents a composite entity holding a table id and an inode id.
 * It's necessary when asking for the metadata of a specific file
 * 
 * @author vangelis
 */
public class InodeTableComposite implements Serializable, EntityIntf {
  
  private static final long serialVersionUID = 1L;

  private int tableid;
  private int inodeid;
  
  public InodeTableComposite(){
  }
  
  public InodeTableComposite(int tableid, int inodeid){
    this.tableid = tableid;
    this.inodeid = inodeid;
  }

  public int getTableid() {
    return this.tableid;
  }

  public int getInodeid() {
    return this.inodeid;
  }

  public void setTableid(int tableid) {
    this.tableid = tableid;
  }

  public void setInodeid(int inodeid) {
    this.inodeid = inodeid;
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
