package se.kth.bbc.project.fb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.meta.entity.Templates;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "inodes")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Inode.findAll",
          query = "SELECT i FROM Inode i"),
  @NamedQuery(name = "Inode.findById",
          query = "SELECT i FROM Inode i WHERE i.id = :id"),
  @NamedQuery(name = "Inode.findByParent",
          query
          = "SELECT i FROM Inode i WHERE i.parent = :parent ORDER BY i.dir DESC, i.name ASC"),
  @NamedQuery(name = "Inode.findByParentAndName",
          query
          = "SELECT i FROM Inode i WHERE i.parent = :parent AND i.name = :name"),
  @NamedQuery(name = "Inode.findDirByParent",
          query
          = "SELECT i FROM Inode i WHERE i.parent = :parent AND i.dir = TRUE ORDER BY i.name ASC"),
  @NamedQuery(name = "Inode.findByName",
          query = "SELECT i FROM Inode i WHERE i.name = :name"),
  @NamedQuery(name = "Inode.findRootByName",
          query
          = "SELECT i FROM Inode i WHERE i.parent IS NULL AND i.name = :name")})
public class Inode implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final String AVAILABLE = "available";
  public static final String UPLOADING = "uploading";
  public static final String COPYING = "copying_to_hdfs";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "name")
  private String name;

  @Basic(optional = false)
  @NotNull
  @Column(name = "modified")
  @Temporal(TemporalType.TIMESTAMP)
  private Date modified;

  @Basic(optional = false)
  @NotNull
  @Column(name = "isDir")
  private boolean dir;

  @Column(name = "size")
  private Integer size;

  @Basic(optional = false)
  @NotNull
  @Column(name = "status")
  private String status;

  @Basic(optional = false)
  @NotNull
  @Column(name = "searchable")
  private boolean searchable;

  @Basic(optional = false)
  @NotNull
  @Column(name = "root")
  private int root;

  @OneToMany(mappedBy = "parent")
  private List<Inode> children;

  @JoinColumn(name = "pid",
          referencedColumnName = "id")
  @ManyToOne
  private Inode parent;

  @ManyToMany
  @JoinTable(
          name = "meta_template_to_inode",
          joinColumns = {
            @JoinColumn(name = "inode_id",
                    referencedColumnName = "id")},
          inverseJoinColumns = {
            @JoinColumn(name = "template_id",
                    referencedColumnName = "templateid")})
  private Collection<Templates> templates;

  public Inode() {
    this.children = new LinkedList<>();
    this.templates = new LinkedList<>();
  }

  public Inode(Integer id) {
    this.children = new LinkedList<>();
    this.templates = new LinkedList<>();
    this.id = id;
  }

  public Inode(Integer id, String name, Date modified, boolean isDir,
          boolean searchable,
          String status) {
    this.id = id;
    this.name = name;
    this.modified = modified;
    this.dir = isDir;
    this.searchable = searchable;
    this.status = status;
    this.children = new LinkedList<>();
    this.templates = new LinkedList<>();
  }

  public Inode(String name, Date modified, boolean dir, boolean searchable,
          String status) {
    this.name = name;
    this.modified = modified;
    this.dir = dir;
    this.searchable = searchable;
    this.status = status;
    this.children = new LinkedList<>();
    this.templates = new LinkedList<>();
  }

  public Inode(String name, Inode parent, boolean dir, boolean searchable,
          int size, String status) {
    this.name = name;
    this.parent = parent;
    this.dir = dir;
    this.searchable = searchable;
    this.size = size;
    this.status = status;
    this.modified = new Date();
    this.children = new LinkedList<>();
    this.templates = new LinkedList<>();
  }

  /**
   * Defines the root field of a inode. Root is the id of the study under
   * which this node is created no matter the depth down the path. This way
   * all the paths can be flattened under the study name and be searched upon
   * at once.
   * <p>
   * @param name
   * @param parent
   * @param root
   * @param dir
   * @param searchable
   * @param size
   * @param status
   * @param templateId
   */
  public Inode(String name, Inode parent, int root, boolean dir,
          boolean searchable, int size, String status) {
    this(name, parent, dir, searchable, size, status);
    this.root = root;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getRoot() {
    return this.root;
  }

  public void setRoot(int root) {
    this.root = root;
  }

  public Date getModified() {
    return modified;
  }

  public void setModified(Date modified) {
    this.modified = modified;
  }

  public boolean isDir() {
    return dir;
  }

  public void setDir(boolean dir) {
    this.dir = dir;
  }

  public boolean isSearchable() {
    return this.searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @XmlTransient
  @JsonIgnore
  public List<Inode> getChildren() {
    return children;
  }

  public void setChildren(List<Inode> children) {
    this.children = children;
  }

  public void clearChildren() {
    this.children.clear();
  }

  public void addChild(Inode i) {
    this.children.add(i);
  }

  public Inode getParent() {
    return parent;
  }

  public void setParent(Inode parent) {
    this.parent = parent;
  }

  public Collection<Templates> getTemplates() {
    return this.templates;
  }

  public void setTemplates(Collection<Templates> templates) {
    this.templates = templates;
  }

  public void addTemplate(Templates template) {
    if (template != null) {
      this.templates.add(template);
    }
  }

  /**
   * for the time being we treat the many to many relationship between inodes
   * and templates as a many to one, where an inode may be associated only to
   * one template, while the same template may be associated to many inodes
   */
  public int getTemplate() {

    int templateId = -1;

    if (this.templates != null && !this.templates.isEmpty()) {
      Iterator it = this.templates.iterator();
      Templates template = (Templates) it.next();
      templateId = template.getId();
    }

    return templateId;
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
    if (!(object instanceof Inode)) {
      return false;
    }
    Inode other = (Inode) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    String isDir = (this.isDir()) ? "1" : "0";
    String isSearchable = (this.isSearchable()) ? "1" : "0";

    return this.getId() + "|" + this.getName() + "|" + this.getParent().getId()
            + "|"
            + this.getRoot() + "|" + this.getModified() + "|" + isDir + "|"
            + this.getSize() + "|" + this.getStatus() + "|" + isSearchable;
  }

  public boolean isRoot() {
    return parent == null;
  }

  /**
   * Determines whether the Inode is the root of this study subtree. It is so
   * if its parent is the "ultimate root". This method should always be used
   * to check for root-being in non-filesystem operation contexts to guarantee
   * safety.
   *
   * @return True if the Inode is the root of its study subtree.
   */
  public boolean isStudyRoot() {
    if (parent != null) {
      return parent.parent == null;
    } else {
      return false;
    }
  }

  public boolean isParent() {
    return name.compareTo("..") == 0;
  }

  public List<NavigationPath> getConstituentsPath() {
    if (isStudyRoot()) {
      List<NavigationPath> p = new ArrayList<>();
      p.add(new NavigationPath(name, name + "/"));
      return p;
    } else {
      List<NavigationPath> p = parent.getConstituentsPath();
      NavigationPath a;
      if (dir) {
        a = new NavigationPath(name, p.get(p.size() - 1).getPath() + name + "/");
      } else {
        a = new NavigationPath(name, p.get(p.size() - 1).getPath() + name);
      }
      p.add(a);
      return p;
    }
  }

  public String getStudyPath() {
    if (isStudyRoot()) {
      return name + "/";
    } else if (dir) {
      return parent.getStudyPath() + name + "/";
    } else {
      return parent.getStudyPath() + name;
    }
  }

  public String getStudyRoot() {
    if (isStudyRoot()) {
      return name;
    } else {
      return parent.getStudyRoot();
    }
  }

  /**
   * Get the path to this Inode.
   *
   * @return The path to this inode. If the Inode is a folder, the path ends
   * in a "/".
   */
  public String getPath() {
    if (isRoot()) {
      return "/" + name + "/";
    } else if (dir) {
      return parent.getPath() + name + "/";
    } else {
      return parent.getPath() + name;
    }
  }

  public Inode getChild(String name) {
    for (Inode i : children) {
      if (i.name.equals(name)) {
        return i;
      }
    }
    return null;
  }

  Object getInodePK() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
