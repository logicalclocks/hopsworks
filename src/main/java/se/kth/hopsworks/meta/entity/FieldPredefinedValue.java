package se.kth.hopsworks.meta.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
 * @author vangelis
 */
@Entity
@Table(name = "hopsworks.meta_field_predefined_values")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FieldPredefinedValue.findAll",
          query = "SELECT f FROM FieldPredefinedValue f"),
  @NamedQuery(name = "FieldPredefinedValue.findById",
          query = "SELECT f FROM FieldPredefinedValue f WHERE f.id = :id"),
  @NamedQuery(name = "FieldPredefinedValue.findByFieldid",
          query
          = "SELECT f FROM FieldPredefinedValue f WHERE f.fieldid = :fieldid"),
  @NamedQuery(name = "FieldPredefinedValue.findByValue",
          query
          = "SELECT f FROM FieldPredefinedValue f WHERE f.valuee = :valuee")})
public class FieldPredefinedValue implements Serializable, EntityIntf {

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

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 250)
  @Column(name = "valuee")
  private String valuee;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "fieldid",
          referencedColumnName = "fieldid")
  private Field fields;

  public FieldPredefinedValue() {
  }

  public FieldPredefinedValue(Integer id) {
    this.id = id;
  }

  public FieldPredefinedValue(Integer id, int fieldid, String valuee) {
    this.id = id;
    this.fieldid = fieldid;
    this.valuee = valuee;
  }

  @Override
  public void copy(EntityIntf entity) {
    FieldPredefinedValue fpfv = (FieldPredefinedValue) entity;

    this.id = fpfv.getId();
    this.fieldid = fpfv.getFieldid();
    this.valuee = fpfv.getValue();
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public void setField(Field fields) {
    this.fields = fields;
  }

  public Field getField() {
    return this.fields;
  }

  public int getFieldid() {
    return fieldid;
  }

  public void setFieldid(int fieldid) {
    this.fieldid = fieldid;
  }

  public String getValue() {
    return valuee;
  }

  public void setValue(String value) {
    this.valuee = value;
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
    if (!(object instanceof FieldPredefinedValue)) {
      return false;
    }
    FieldPredefinedValue other = (FieldPredefinedValue) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.FieldPredefinedValues[ id=" + id + " ]";
  }

}
