package se.kth.hopsworks.meta.entity;

import java.io.Serializable;
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
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.fb.Inode;

/**
 *
 * @author vangelis
 */
@Entity
@Table(name = "hopsworks.meta_tuple_to_file")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TupleToFile.findAll",
          query = "SELECT t FROM TupleToFile t"),
  @NamedQuery(name = "TupleToFile.findById",
          query = "SELECT t FROM TupleToFile t WHERE t.tupleid = :tupleid"),
  @NamedQuery(name = "TupleToFile.findByInodeid",
          query
          = "SELECT t FROM TupleToFile t WHERE t.inode.inodePK.parentId = :parentid AND t.inode.inodePK.name = :name"),
  @NamedQuery(name = "TupleToFile.findByTupleidAndInodeid",
          query
          = "SELECT t FROM TupleToFile t WHERE t.tupleid = :tupleid AND t.inode.inodePK.parentId = :parentid AND t.inode.inodePK.name = :name")})

public class TupleToFile implements Serializable, EntityIntf {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @NotNull
  @Column(name = "tupleid")
  private Integer tupleid;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inodeid")
  private Integer inodeid;

  @JoinColumns({
    @JoinColumn(name = "inode_pid",
            referencedColumnName = "parent_id"),
    @JoinColumn(name = "inode_name",
            referencedColumnName = "name")
  })
  @ManyToOne(optional = false)
  private Inode inode;

  @OneToMany(mappedBy = "tupleToFile",
          targetEntity = RawData.class,
          fetch = FetchType.LAZY,
          cascade = {CascadeType.ALL})
  private List<RawData> raw;

  public TupleToFile() {
  }

  public TupleToFile(int tupleid, Inode inode) {
    this.tupleid = tupleid;
    this.inode = inode;
    this.inodeid = inode.getId();
  }

  public TupleToFile(Integer tupleid) {
    this.tupleid = tupleid;
  }

  @Override
  public void copy(EntityIntf ttf) {
    TupleToFile t = (TupleToFile) ttf;

    this.tupleid = t.getId();
    this.inode = new Inode(t.getInode());
    this.raw = t.getRawData();
  }

  @Override
  public Integer getId() {
    return this.tupleid;
  }

  @Override
  public void setId(Integer id) {
    this.tupleid = id;
  }

  public void setInodeId(Integer inodeid){
    this.inodeid = inodeid;
  }
  
  public Integer getInodeId(){
    return this.inodeid;
  }
  
  public Inode getInode() {
    return this.inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  /*
   * get and set the child entities
   */
  public List<RawData> getRawData() {
    return this.raw;
  }

  public void setRawData(List<RawData> raw) {
    this.raw = raw;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (tupleid != null ? tupleid.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof TupleToFile)) {
      return false;
    }
    TupleToFile other = (TupleToFile) object;
    if ((this.tupleid == null && other.tupleid != null) || (this.tupleid != null
            && !this.tupleid.
            equals(other.tupleid))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.TupleToFile[ id=" + this.tupleid + ", inode="
            + this.inode + " ]";
  }

}
