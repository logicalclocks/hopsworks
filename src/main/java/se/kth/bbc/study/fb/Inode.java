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
    @NamedQuery(name = "Inode.findByName", query = "SELECT i FROM Inode i WHERE i.name = :name")})
public class Inode implements Serializable {

    private static final long serialVersionUID = 1L;

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
        this(0, name, modified, dir, status);
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

    boolean isRoot() {
        //Cannot check on name because sometimes a folder with name ".." is root.
        return parent == null;
    }

    public boolean isParent() {
        return name.compareTo("..") == 0;
    }

    public List<NavigationPath> getPath() {
        if (isRoot()) {
            List<NavigationPath> p = new ArrayList<>();
            //TODO: change "study" to real study name
            p.add(new NavigationPath(name, name+"/"));
            return p;
        } else {
            List<NavigationPath> p = parent.getPath();
            NavigationPath a = new NavigationPath(name, p.get(p.size() - 1).getPath() + name + "/");
            p.add(a);
            return p;
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
