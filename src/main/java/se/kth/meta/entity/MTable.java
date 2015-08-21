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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Vangelis
 */
@Entity
@Table(name = "hopsworks.meta_tables")

@NamedQueries({
  @NamedQuery(name = "MTable.findAll",
          query = "SELECT t FROM MTable t"),
  @NamedQuery(name = "Tables.findById",
          query = "SELECT t FROM MTable t WHERE t.id = :id"),
  @NamedQuery(name = "Tables.findByName",
          query = "SELECT t FROM MTable t WHERE t.name = :name"),
  @NamedQuery(name = "MTable.findByTemplateId",
          query
          = "SELECT DISTINCT t FROM MTable t WHERE t.templateid = :templateid")})
public class MTable implements Serializable, EntityIntf, Comparable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "tableid")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 50)
  @Column(name = "name")
  private String name;

  @Basic(optional = false)
  @NotNull
  @Column(name = "templateid")
  private int templateid;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "templateid",
          referencedColumnName = "templateid")
  private Template template;

  @OneToMany(mappedBy = "table",
          targetEntity = Field.class,
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL) //cascade type all updates the child entities
  private List<Field> fields;

  public MTable() {
  }

  public MTable(Integer id) {
    this.id = id;
    this.fields = new LinkedList<>();
  }

  public MTable(Integer id, String name) {
    this.id = id;
    this.name = name;
    this.fields = new LinkedList<>();
  }

  @Override
  public void copy(EntityIntf table) {
    MTable t = (MTable) table;

    this.id = t.getId();
    this.templateid = t.getTemplateid();
    this.name = t.getName();
    this.fields = t.getFields();
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public void setTemplateid(int templateid) {
    this.templateid = templateid;
  }

  public int getTemplateid() {
    return this.templateid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Field> getFields() {
    return this.fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  public void addField(Field field) {
    this.fields.add(field);
    if (field != null) {
      field.setMTable(this);
    }
  }

  public void removeField(Field field) {
    this.fields.remove(field);
    if (field != null) {
      field.setMTable(null);
    }
  }

  public void resetFields() {
    this.fields = new LinkedList<>();
  }

  public Template getTemplate() {
    return this.template;
  }

  public void setTemplate(Template template) {
    this.template = template;
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
    if (!(object instanceof MTable)) {
      return false;
    }
    MTable other = (MTable) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "entity.Tables[ id=" + this.id + ", name=" + this.name
            + ", templateId=" + this.templateid + " ]";
  }

  @Override
  public int compareTo(Object o) {
    MTable table = (MTable) o;

    if (this.getId() > table.getId()) {
      return 1;
    } else if (this.getId() < table.getId()) {
      return -1;
    }

    return 0;
  }
}
