package se.kth.hopsworks.meta.entity;

import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Mahmoud Ismail<maism@kth.se>
 */
@Embeddable
public class SchemalessMetadataPK {
  
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_id")
  private int inodeId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_parent_id")
  private int inodeParentId;

  public SchemalessMetadataPK() {
  }

  public SchemalessMetadataPK(Integer inodeId, Integer inodeParentId){
    this.inodeId = inodeId;
    this.inodeParentId = inodeParentId;
  }
  
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getInodeId() {
    return inodeId;
  }

  public void setInodeId(int inodeId) {
    this.inodeId = inodeId;
  }

  public int getInodeParentId() {
    return inodeParentId;
  }

  public void setInodeParentId(int inodeParentId) {
    this.inodeParentId = inodeParentId;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 47 * hash + Objects.hashCode(this.id);
    hash = 47 * hash + this.inodeId;
    hash = 47 * hash + this.inodeParentId;
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SchemalessMetadataPK other = (SchemalessMetadataPK) obj;
    if (this.inodeId != other.inodeId) {
      return false;
    }
    if (this.inodeParentId != other.inodeParentId) {
      return false;
    }
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "SchemalessMetadataPK{" + "id=" + id + ", inodeId=" + inodeId + ", inodeParentId=" + inodeParentId + '}';
  }
  
}
