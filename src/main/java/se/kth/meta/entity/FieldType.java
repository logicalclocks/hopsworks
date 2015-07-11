package se.kth.meta.entity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author vangelis
 */
@Entity
@Table(name = "hopsworks_kthfs.meta_field_types")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FieldType.findAll",
          query = "SELECT f FROM FieldType f"),
  @NamedQuery(name = "FieldType.findById",
          query = "SELECT f FROM FieldType f WHERE f.id = :id"),
  @NamedQuery(name = "FieldType.findByDescription",
          query
          = "SELECT f FROM FieldType f WHERE f.description = :description")})
public class FieldType implements Serializable, EntityIntf {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 50)
  @Column(name = "description")
  private String description;

  @OneToMany(mappedBy = "fieldTypes",
          targetEntity = Field.class,
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL) //cascade type all updates the child entities
  private List<Field> fields;

  public FieldType() {
  }

  public FieldType(Integer id) {
    this.id = id;
    this.fields = new LinkedList<>();
  }

  public FieldType(Integer id, String description) {
    this.id = id;
    this.description = description;
    this.fields = new LinkedList<>();
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Field> getFields() {
    return this.fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
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
    if (!(object instanceof FieldType)) {
      return false;
    }
    FieldType other = (FieldType) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.FieldTypes[ id=" + id + " ]";
  }

  @Override
  public void copy(EntityIntf entity) {
    FieldType ft = (FieldType) entity;

    this.id = ft.getId();
    this.description = ft.getDescription();
  }

}
