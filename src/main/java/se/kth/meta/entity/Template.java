package se.kth.meta.entity;

import java.io.Serializable;
import java.util.Collection;
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
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import se.kth.bbc.project.fb.Inode;

/**
 *
 * @author vangelis
 */
@Entity
@Table(name = "hopsworks.meta_templates")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Template.findAll",
          query = "SELECT t FROM Template t"),
  @NamedQuery(name = "Template.findById",
          query = "SELECT t FROM Template t WHERE t.id = :templateid"),
  @NamedQuery(name = "Template.findByName",
          query = "SELECT t FROM Template t WHERE t.name = :name")})
public class Template implements Serializable, EntityIntf, Comparable<Template> {

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

  @OneToMany(mappedBy = "template",
          targetEntity = MTable.class,
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL)
  private List<MTable> tables;

  @JoinTable(name = "hopsworks.meta_template_to_inode",
          inverseJoinColumns = {
            @JoinColumn(name = "inode_pid",
                    referencedColumnName = "parent_id"),
            @JoinColumn(name = "inode_name",
                    referencedColumnName = "name")
          },
          joinColumns = {
            @JoinColumn(name = "template_id",
                    referencedColumnName = "templateid")
          })
  @ManyToMany(fetch = FetchType.EAGER)
  private Collection<Inode> inodes;

  public Template() {
  }

  public Template(Integer templateid) {
    this.id = templateid;
    this.tables = new LinkedList<>();
    this.inodes = new LinkedList<>();
  }

  public Template(Integer templateid, String name) {
    this.id = templateid;
    this.name = name;
    this.tables = new LinkedList<>();
    this.inodes = new LinkedList<>();
  }

  @Override
  public void copy(EntityIntf template) {
    Template t = (Template) template;

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

  public List<MTable> getMTables() {
    return this.tables;
  }

  public void setMTables(List<MTable> tables) {
    this.tables = tables;
  }

  @XmlTransient
  public Collection<Inode> getInodes() {
    return this.inodes;
  }

  public void setInodes(List<Inode> inodes) {
    this.inodes = inodes;
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
    if (!(object instanceof Template)) {
      return false;
    }
    Template other = (Template) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.Templates[ templateid=" + id + " name=" + name
            + " ]";
  }

  @Override
  public int compareTo(Template t) {
    if(this.getId() > t.getId()){
      return 1;
    }
    else if(this.getId() < t.getId()){
      return -1;
    }
    return 0;
  }

}
