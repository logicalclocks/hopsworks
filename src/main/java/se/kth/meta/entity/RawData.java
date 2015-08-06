package se.kth.meta.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
  @NamedQuery(name = "RawData.findById",
          query = "SELECT r FROM RawData r WHERE r.id = :id"),
  @NamedQuery(name = "RawData.findByFieldid",
          query = "SELECT r FROM RawData r WHERE r.fieldid = :fieldid"),
  @NamedQuery(name = "RawData.findByTupleid",
          query = "SELECT r FROM RawData r WHERE r.tupleid = :tupleid"),
  @NamedQuery(name = "RawData.lastInsertedTupleId",
          query
          = "SELECT r FROM RawData r GROUP BY r.tupleid ORDER BY r.tupleid desc")})
public class RawData implements Serializable, EntityIntf {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "fieldid")
  private int fieldid;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "fieldid",
          referencedColumnName = "fieldid")
  private Field fields;

  @Basic(optional = false)
  @NotNull
  @Column(name = "tupleid")
  private int tupleid;

  @Basic(optional = false)
  @NotNull
  @Lob
  @Size(min = 1,
          max = 65535)
  @Column(name = "data")
  private String data;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "tupleid",
          referencedColumnName = "tupleid")
  private TupleToFile tupleToFile;

  public RawData() {
  }

  public RawData(Integer id) {
    this.id = id;
  }

  public RawData(int fieldid, int tupleid, String data) {
    this.fieldid = fieldid;
    this.tupleid = tupleid;
    this.data = data;
  }

  @Override
  public void copy(EntityIntf raw) {
    RawData r = (RawData) raw;

    this.id = r.getId();
    this.fieldid = r.getFieldid();
    this.tupleid = r.getTupleid();
    this.data = r.getData();
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public int getFieldid() {
    return fieldid;
  }

  public void setFieldid(int fieldid) {
    this.fieldid = fieldid;
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
   * -------------------------------
   */

  public int getTupleid() {
    return tupleid;
  }

  public void setTupleid(int tupleid) {
    this.tupleid = tupleid;
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

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof RawData)) {
      return false;
    }
    RawData other = (RawData) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "entity.RawData[ id=" + id + ", fieldid=" + fieldid + ", tupleid="
            + tupleid + ", data=" + data + " ]";
  }

}
