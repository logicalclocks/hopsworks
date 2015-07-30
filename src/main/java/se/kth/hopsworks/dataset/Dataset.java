package se.kth.hopsworks.dataset;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.fb.Inode;

/**
 *
 * @author ermiasg
 */
@Entity
@Table(name = "hopsworks.dataset")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Dataset.findAll",
          query
          = "SELECT d FROM Dataset d"),
  @NamedQuery(name = "Dataset.findById",
          query
          = "SELECT d FROM Dataset d WHERE d.id = :id"),
  @NamedQuery(name = "Dataset.findByInode",
          query
          = "SELECT d FROM Dataset d WHERE d.inode = :inode"),

  @NamedQuery(name = "Dataset.findByProjectAndInode",
          query
          = "SELECT d FROM Dataset d WHERE d.projectId = :projectId AND d.inode = :inode"),
  @NamedQuery(name = "Dataset.findByProject",
          query
          = "SELECT d FROM Dataset d WHERE d.projectId = :projectId"),
  @NamedQuery(name = "Dataset.findByDescription",
          query
          = "SELECT d FROM Dataset d WHERE d.description = :description")})
public class Dataset implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final boolean PENDING = false;
  public static final boolean ACCEPTED = true;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumns({
    @JoinColumn(name = "inode_pid",
            referencedColumnName = "parent_id"),
    @JoinColumn(name = "inode_name",
            referencedColumnName = "name")
  })
  @ManyToOne(optional = false)
  private Inode inode;
  @Size(max = 3000)
  @Column(name = "description")
  private String description;
  @JoinColumn(name = "projectId",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project projectId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "editable")
  private boolean editable;
  @Basic(optional = false)
  @NotNull
  @Column(name = "status")
  private boolean status;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "dataset")
  private Collection<DatasetRequest> datasetRequestCollection;

  public Dataset() {
  }

  public Dataset(Integer id) {
    this.id = id;
  }

  public Dataset(Integer id, Inode inode) {
    this.id = id;
    this.inode = inode;
  }

  public Dataset(Inode inode, Project project) {
    this.inode = inode;
    this.projectId = project;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Inode getInode() {
    return inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Project getProjectId() {
    return projectId;
  }

  public void setProjectId(Project projectId) {
    this.projectId = projectId;
  }

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public boolean getStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public Collection<DatasetRequest> getDatasetRequestCollection() {
    return datasetRequestCollection;
  }

  public void setDatasetRequestCollection(
          Collection<DatasetRequest> datasetRequestCollection) {
    this.datasetRequestCollection = datasetRequestCollection;
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
    if (!(object instanceof Dataset)) {
      return false;
    }
    Dataset other = (Dataset) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.dataset.Dataset[ id=" + id + " ]";
  }

}
