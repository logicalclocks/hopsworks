package se.kth.meta.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author vangelis
 */
@Embeddable
public class RawDataPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "fieldid")
  private int fieldid;

  @Basic(optional = false)
  @NotNull
  @Column(name = "tupleid")
  private int tupleid;

  public RawDataPK(){
  }
  
  public RawDataPK(int fieldid, int tupleid){
    this.fieldid = fieldid;
    this.tupleid = tupleid;
  }
  
  public void copy(RawDataPK rawdataPK){
    this.fieldid = rawdataPK.getFieldid();
    this.tupleid = rawdataPK.getTupleid();
  }
  
  public void setFieldid(int fieldid){
    this.fieldid = fieldid;
  }
  
  public void setTupleid(int tupleid){
    this.tupleid = tupleid;
  }
  
  public int getFieldid(){
    return this.fieldid;
  }
  
  public int getTupleid(){
    return this.tupleid;
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) this.fieldid;
    hash += (int) this.tupleid;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof RawDataPK)) {
      return false;
    }
    RawDataPK other = (RawDataPK) object;

    if (this.fieldid != other.fieldid) {
      return false;
    }

    return this.tupleid == other.tupleid;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.RawDataPK[ fieldid="
            + this.fieldid + ", tupleid= " + this.tupleid + " ]";
  }
}