package se.kth.hopsworks.meta.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import se.kth.bbc.project.fb.Inode;

/**
 *
 * @author vangelis
 */
@Entity
@Table(name = "hopsworks.meta_inode_basic_metadata")

@NamedQueries({
  @NamedQuery(name = "InodeBasicMetadata.findAll",
          query = "SELECT m FROM InodeBasicMetadata m"),
  @NamedQuery(name = "InodeBasicMetadata.findById",
          query
          = "SELECT m FROM InodeBasicMetadata m WHERE m.inode.inodePK.parentId = :id AND m.inode.inodePK.name = :name")})
public class InodeBasicMetadata implements Serializable, EntityIntf {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @JoinColumns({
    @JoinColumn(name = "inode_pid",
            referencedColumnName = "parent_id"),
    @JoinColumn(name = "inode_name",
            referencedColumnName = "name")
  })
  @OneToOne(optional = false)
  private Inode inode;

  @Size(max = 3000)
  @Column(name = "description")
  private String description;

  @Column(name = "searchable")
  private boolean searchable;

  public InodeBasicMetadata() {

  }

  public InodeBasicMetadata(Inode inode, String description, boolean searchable) {
    this.inode = inode;
    this.description = description;
    this.searchable = searchable;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  @Override
  public Integer getId() {
    return this.id;
  }

  public Inode getInode() {
    return this.inode;
  }

  public String getDescription() {
    return this.description;
  }

  public boolean getSearchable() {
    return this.searchable;
  }

  @Override
  public void copy(EntityIntf entity) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }
}
