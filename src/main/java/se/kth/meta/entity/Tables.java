package se.kth.meta.entity;

import se.kth.meta.entity.listener.EntityListener;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
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
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Vangelis
 */
@Entity
@Table(name = "vangelis_kthfs.meta_tables")
@EntityListeners(EntityListener.class)

@NamedQueries({
  @NamedQuery(name = "Tables.findAll",
          query = "SELECT t FROM Tables t"),
  @NamedQuery(name = "Tables.findById",
          query = "SELECT t FROM Tables t WHERE t.id = :id"),
  @NamedQuery(name = "Tables.findByName",
          query = "SELECT t FROM Tables t WHERE t.name = :name"),
  @NamedQuery(name = "Tables.fetchTemplate",
          query
          = "SELECT DISTINCT t FROM Tables t WHERE t.templateid = :templateid")})
public class Tables implements Serializable, EntityIntf {

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
  private Templates templates;

  @OneToMany(mappedBy = "tables",
          targetEntity = Fields.class,
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL) //cascade type all updates the child entities
  private List<Fields> fields;

  @Transient
  private int inodeid;
  /*
   * indicates whether a table containing fields can be deleted along
   * with its fields or not
   */
  @Transient
  private boolean forceDelete;

  public Tables() {
  }

  public Tables(Integer id) {
    this.id = id;
    this.fields = new LinkedList<>();
  }

  public Tables(Integer id, String name) {
    this.id = id;
    this.name = name;
    this.fields = new LinkedList<>();
  }

  @Override
  public void copy(EntityIntf table) {
    Tables t = (Tables) table;

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

  public List<Fields> getFields() {
    return this.fields;
  }

  public void setFields(List<Fields> fields) {
    this.fields = fields;
  }

  public void addField(Fields field) {
    this.fields.add(field);
    if (field != null) {
      field.setTables(this);
    }
  }

  public void removeField(Fields field) {
    this.fields.remove(field);
    if (field != null) {
      field.setTables(null);
    }
  }

  public void resetFields() {
    this.fields = new LinkedList<>();
  }

  public void setForceDelete(boolean forceDelete) {
    this.forceDelete = forceDelete;
  }

  public boolean forceDelete() {
    return this.forceDelete;
  }

  public Templates getTemplates() {
    return this.templates;
  }

  public void setTemplates(Templates templates) {
    this.templates = templates;
  }

  public void setInodeid(int inodeid) {
    this.inodeid = inodeid;
  }

  public int getInodeid() {
    return this.inodeid;
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
    if (!(object instanceof Tables)) {
      return false;
    }
    Tables other = (Tables) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "entity.Tables[ id=" + id + ", name=" + name + " ]";
  }
}
