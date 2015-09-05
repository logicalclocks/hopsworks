package se.kth.meta.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author vangelis
 */
@Embeddable
public class MetadataPK implements Serializable {
  
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "fieldid")
  private int fieldid;

  @Basic(optional = false)
  @NotNull
  @Column(name = "tupleid")
  private int tupleid;

  public MetadataPK() {
  }

  public MetadataPK(int id, int fieldid, int tupleid) {
    this.id = id;
    this.fieldid = fieldid;
    this.tupleid = tupleid;
  }

  public void copy(MetadataPK metadataPK) {
    this.id = metadataPK.getId();
    this.fieldid = metadataPK.getFieldid();
    this.tupleid = metadataPK.getTupleid();
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setFieldid(int fieldid) {
    this.fieldid = fieldid;
  }

  public void setTupleid(int tupleid) {
    this.tupleid = tupleid;
  }

  public int getId() {
    return this.id;
  }

  public int getFieldid() {
    return this.fieldid;
  }

  public int getTupleid() {
    return this.tupleid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) this.id;
    hash += (int) this.fieldid;
    hash += (int) this.tupleid;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof MetadataPK)) {
      return false;
    }
    MetadataPK other = (MetadataPK) object;
    if (!this.id.equals(other.id)) {
      return false;
    }
    if (this.fieldid != other.fieldid) {
      return false;
    }

    return this.tupleid == other.tupleid;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.MetaDataPK[ id=" + this.id + ", fieldid="
            + this.fieldid + ", tupleid= " + this.tupleid + " ]";
  }
}