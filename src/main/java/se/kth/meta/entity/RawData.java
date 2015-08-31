package se.kth.meta.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Vangelis
 */
@Entity
@Table(name = "hopsworks.meta_raw_data")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "RawData.findAll",
          query = "SELECT r FROM RawData r"),
  @NamedQuery(name = "RawData.findByPrimaryKey",
          query = "SELECT r FROM RawData r WHERE r.rawdataPK = :rawdataPK"),
  @NamedQuery(name = "RawData.findByFieldid",
          query = "SELECT r FROM RawData r WHERE r.rawdataPK.fieldid = :fieldid"),
  @NamedQuery(name = "RawData.findByTupleid",
          query = "SELECT r FROM RawData r WHERE r.rawdataPK.tupleid = :tupleid"),
  @NamedQuery(name = "RawData.lastInsertedTupleId",
          query
          = "SELECT r FROM RawData r GROUP BY r.rawdataPK.tupleid ORDER BY r.rawdataPK.tupleid desc")})
public class RawData implements EntityIntf, Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private RawDataPK rawdataPK;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "fieldid",
          referencedColumnName = "fieldid")
  private Field fields;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "tupleid",
          referencedColumnName = "tupleid")
  private TupleToFile tupleToFile;

  @OneToMany(mappedBy = "rawdata",
          targetEntity = MetaData.class,
          fetch = FetchType.LAZY,
          cascade = {CascadeType.ALL})
  private List<MetaData> metadata;

  public RawData() {
    this.rawdataPK = new RawDataPK(-1, -1);
  }

  public RawData(RawDataPK rawdataPK) {
    this.rawdataPK = rawdataPK;
  }

  public RawData(int fieldid, int tupleid) {
    this.rawdataPK = new RawDataPK(fieldid, tupleid);
  }

  @Override
  public void copy(EntityIntf raw) {
    RawData r = (RawData) raw;

    this.rawdataPK.copy(r.getRawdataPK());
  }

  public void setRawdataPK(RawDataPK rawdataPK) {
    this.rawdataPK = rawdataPK;
  }

  public RawDataPK getRawdataPK() {
    return this.rawdataPK;
  }

  /*
   * get and set the parent entity
   */
  public void setField(Field fields) {
    this.fields = fields;
  }

  public Field getField() {
    return this.fields;
  }

  /*
   * get and set the parent entity
   */
  public void setTupleToFile(TupleToFile ttf) {
    this.tupleToFile = ttf;
  }

  public TupleToFile getTupleToFile() {
    return this.tupleToFile;
  }
  
  /*
   * get and set the metadata
   */
  public void setMetaData(List<MetaData> metadata){
    this.metadata = metadata;
  }
  
  public List<MetaData> getMetaData(){
    return this.metadata;
  }
  
  public void resetMetadata(){
    this.metadata.clear();
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
  public int hashCode() {
    int hash = 0;
    hash += (this.rawdataPK != null ? this.rawdataPK.hashCode() : 0);
    
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof RawData)) {
      return false;
    }
    RawData other = (RawData) object;

    return !((this.rawdataPK == null && other.getRawdataPK() != null)
            || (this.rawdataPK != null
            && !this.rawdataPK.equals(other.getRawdataPK())));
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.RawData[ rawdataPK= " + this.rawdataPK + " ]";
  }

}
