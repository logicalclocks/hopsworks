package se.kth.hopsworks.meta.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author vangelis
 */
@Embeddable
public class HdfsMetadataLogPK implements Serializable {

  private static final long serialVersionUID = 1L;

  @Basic(optional = false)
  @Column(name = "dataset_id")
  private Integer datasetId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_id")
  private int inodeid;

  @Basic(optional = false)
  @NotNull
  @Column(name = "logical_time")
  private int ltime;

  public HdfsMetadataLogPK() {
  }

  public HdfsMetadataLogPK(int datasetId, int inodeid, int ltime) {
    this.datasetId = datasetId;
    this.inodeid = inodeid;
    this.ltime = ltime;
  }

  public HdfsMetadataLogPK(HdfsMetadataLogPK pk) {
    this.datasetId = pk.getDatasetId();
    this.inodeid = pk.getInodeid();
    this.ltime = pk.getLtime();
  }

  public void copy(HdfsMetadataLogPK pk) {
    this.datasetId = pk.getDatasetId();
    this.inodeid = pk.getInodeid();
    this.ltime = pk.getLtime();
  }

  public Integer getDatasetId() {
    return this.datasetId;
  }

  public int getInodeid() {
    return this.inodeid;
  }

  public int getLtime() {
    return this.ltime;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public void setInodeid(int inodeid) {
    this.inodeid = inodeid;
  }

  public void setLtime(int ltime) {
    this.ltime = ltime;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.HdfsMetadataLogPK[ dataset_id= " + this.datasetId
            + ", inode_id= " + this.inodeid + ", logical_time= " + this.ltime
            + " ]";
  }
}
