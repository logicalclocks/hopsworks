package se.kth.bbc.study.fb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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

/**
 *
 * @author stig
 */
@Entity
@Table(name = "Inodes")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Inode.findAll", query = "SELECT i FROM Inode i"),
    @NamedQuery(name = "Inode.findById", query = "SELECT i FROM Inode i WHERE i.id = :id"),
    @NamedQuery(name = "Inode.findByParent", query = "SELECT i FROM Inode i WHERE i.parent = :parent ORDER BY i.dir DESC, i.name ASC"),
    @NamedQuery(name = "Inode.findDirByParent", query = "SELECT i FROM Inode i WHERE i.parent = :parent AND i.dir = TRUE ORDER BY i.name ASC"),
    @NamedQuery(name = "Inode.findByName", query = "SELECT i FROM Inode i WHERE i.name = :name"),
    @NamedQuery(name = "Inode.findRootByName", query = "SELECT i FROM Inode i WHERE i.parent IS NULL AND i.name = :name")})
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
    @Size(min = 1, max = 128)
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
    @OneToMany(mappedBy = "parent")
    private List<Inode> children;
    @JoinColumn(name = "pid", referencedColumnName = "id")
    @ManyToOne
    private Inode parent;

    public Inode() {
    }

    public Inode(Integer id) {
        this.id = id;
    }

    public Inode(Integer id, String name, Date modified, boolean isDir, String status) {
        this.id = id;
        this.name = name;
        this.modified = modified;
        this.dir = isDir;
        this.status = status;
    }

    public Inode(String name, Date modified, boolean dir, String status) {
        this.name = name;
        this.modified = modified;
        this.dir = dir;
        this.status = status;
    }

    public Inode(String name, Inode parent, boolean dir, int size, String status) {
        this.name = name;
        this.parent = parent;
        this.dir = dir;
        this.size = size;
        this.status = status;
        this.modified = new Date();
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

    public void addChild(Inode i) {
        this.children.add(i);
    }

    public Inode getParent() {
        return parent;
    }

    public void setParent(Inode parent) {
        this.parent = parent;
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
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.study.fb.Inode[ id=" + id + " ]";
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
            //TODO: change "study" to real study name
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

}
