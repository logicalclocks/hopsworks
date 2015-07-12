package se.kth.meta.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

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
          query = "SELECT t FROM TupleToFile t WHERE t.id = :id"),
  @NamedQuery(name = "TupleToFile.findByTupleid",
          query = "SELECT t FROM TupleToFile t WHERE t.tupleid = :tupleid"),
  @NamedQuery(name = "TupleToFile.findByInodeid",
          query = "SELECT t FROM TupleToFile t WHERE t.inodeid = :inodeid"),
  @NamedQuery(name = "TupleToFile.findByTupleidAndInodeid",
          query
          = "SELECT t FROM TupleToFile t WHERE t.tupleid = :tupleid AND t.inodeid = :inodeid")})

public class TupleToFile implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "tupleid")
  private int tupleid;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inodeid")
  private int inodeid;

  public TupleToFile() {
  }

  public TupleToFile(int tupleid, int inodeid) {
    this.tupleid = tupleid;
    this.inodeid = inodeid;
  }

  public TupleToFile(Integer id) {
    this.id = id;
  }

  public TupleToFile(Integer id, int tupleid, int inodeid) {
    this.id = id;
    this.tupleid = tupleid;
    this.inodeid = inodeid;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getTupleid() {
    return tupleid;
  }

  public void setTupleid(int tupleid) {
    this.tupleid = tupleid;
  }

  public int getInodeid() {
    return inodeid;
  }

  public void setInodeid(int inodeid) {
    this.inodeid = inodeid;
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
    if (!(object instanceof TupleToFile)) {
      return false;
    }
    TupleToFile other = (TupleToFile) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.TupleToFile[ id=" + id + " ]";
  }

}
