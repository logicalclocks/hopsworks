package io.hops.hopsworks.common.dao.tensorflow.tf;

import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "tf_serving",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TfServing.findAll",
      query = "SELECT t FROM TfServing t")
  ,
    @NamedQuery(name = "TfServing.findById",
      query = "SELECT t FROM TfServing t WHERE t.id = :id")
  ,
    @NamedQuery(name = "TfServing.findByHostIp",
      query = "SELECT t FROM TfServing t WHERE t.hostIp = :hostIp")
  ,
    @NamedQuery(name = "TfServing.findByPort",
      query = "SELECT t FROM TfServing t WHERE t.port = :port")
  ,
    @NamedQuery(name = "TfServing.findByPid",
      query = "SELECT t FROM TfServing t WHERE t.pid = :pid")
  ,
    @NamedQuery(name = "TfServing.findByTfServingStatus",
      query
      = "SELECT t FROM TfServing t WHERE t.tfServingStatus = :tfServingStatus")
  ,
    @NamedQuery(name = "TfServing.findByHdfsUserId",
      query
      = "SELECT t FROM TfServing t WHERE t.hdfsUserId = :hdfsUserId")
  ,
    @NamedQuery(name = "TfServing.findByCreated",
      query = "SELECT t FROM TfServing t WHERE t.created = :created")
  ,
    @NamedQuery(name = "TfServing.findByModelName",
      query = "SELECT t FROM TfServing t WHERE t.modelName = :modelName")
  ,
    @NamedQuery(name = "TfServing.findByHdfsModelPath",
      query
      = "SELECT t FROM TfServing t WHERE t.hdfsModelPath = :hdfsModelPath")
  ,
    @NamedQuery(name = "TfServing.findByVersion",
      query = "SELECT t FROM TfServing t WHERE t.version = :version")
  ,
    @NamedQuery(name = "TfServing.findByBatchSize",
      query = "SELECT t FROM TfServing t WHERE t.batchSize = :batchSize")})
public class TfServing implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Size(max = 255)
  @Column(name = "host_ip")
  private String hostIp;
  @Column(name = "port")
  private Integer port;
  @Column(name = "pid")
  private BigInteger pid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 50)
  @Column(name = "tf_serving_status")
  private String tfServingStatus;

  @Basic(optional = false)
  @NotNull
  @JoinColumn(name = "hdfs_user_id",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private HdfsUsers hdfsUserId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "model_name")
  private String modelName;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "hdfs_model_path")
  private String hdfsModelPath;
  @Basic(optional = false)
  @NotNull
  @Column(name = "version")
  private int version;
  @Basic(optional = false)
  @NotNull
  @Column(name = "batch_size")
  private int batchSize;
  @JoinColumn(name = "project_id",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project projectId;

  public TfServing() {
  }

  public TfServing(Integer id) {
    this.id = id;
  }

  public TfServing(Integer id, String tfServingStatus, HdfsUsers hdfsUserId, Date created, String modelName,
      String hdfsModelPath, int version, int batchSize) {
    this.id = id;
    this.tfServingStatus = tfServingStatus;
    this.hdfsUserId = hdfsUserId;
    this.created = created;
    this.modelName = modelName;
    this.hdfsModelPath = hdfsModelPath;
    this.version = version;
    this.batchSize = batchSize;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public BigInteger getPid() {
    return pid;
  }

  public void setPid(BigInteger pid) {
    this.pid = pid;
  }

  public String getTfServingStatus() {
    return tfServingStatus;
  }

  public void setTfServingStatus(String tfServingStatus) {
    this.tfServingStatus = tfServingStatus;
  }

  public HdfsUsers getHdfsUserId() {
    return hdfsUserId;
  }

  public void setHdfsUserId(HdfsUsers hdfsUserId) {
    this.hdfsUserId = hdfsUserId;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getHdfsModelPath() {
    return hdfsModelPath;
  }

  public void setHdfsModelPath(String hdfsModelPath) {
    this.hdfsModelPath = hdfsModelPath;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public Project getProjectId() {
    return projectId;
  }

  public void setProjectId(Project projectId) {
    this.projectId = projectId;
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
    if (!(object instanceof TfServing)) {
      return false;
    }
    TfServing other = (TfServing) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.tensorflow.tf.TfServing[ id=" + id + " ]";
  }

}
