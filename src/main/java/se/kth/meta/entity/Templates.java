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
@Table(name = "vangelis_kthfs.meta_templates")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Templates.findAll",
          query = "SELECT t FROM Templates t"),
  @NamedQuery(name = "Templates.findByTemplateid",
          query = "SELECT t FROM Templates t WHERE t.id = :templateid"),
  @NamedQuery(name = "Templates.findByName",
          query = "SELECT t FROM Templates t WHERE t.name = :name")})
public class Templates implements Serializable, EntityIntf {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "templateid")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 250)
  @Column(name = "name")
  private String name;

  @OneToMany(mappedBy = "templates",
          targetEntity = Tables.class,
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL)
  private List<Tables> tables;

  public Templates() {
  }

  public Templates(Integer templateid) {
    this.id = templateid;
    this.tables = new LinkedList<>();
  }

  public Templates(Integer templateid, String name) {
    this.id = templateid;
    this.name = name;
    this.tables = new LinkedList<>();
  }

  @Override
  public void copy(EntityIntf template) {
    Templates t = (Templates) template;

    this.id = t.getId();
    this.name = t.getName();
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Tables> getTables() {
    return this.tables;
  }

  public void setTables(List<Tables> tables) {
    this.tables = tables;
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
    if (!(object instanceof Templates)) {
      return false;
    }
    Templates other = (Templates) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.Templates[ templateid=" + id + " ]";
  }

}
