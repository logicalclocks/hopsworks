package se.kth.meta.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author vangelis
 */
@Entity
@Table(name = "hops.hdfs_metadata_log")
@NamedQueries({
  @NamedQuery(name = "HdfsMetadataLog.findAll",
          query
          = "SELECT h FROM HdfsMetadataLog h"),
  @NamedQuery(name = "HdfsMetadataLog.findByPrimaryKey",
          query
          = "SELECT h FROM HdfsMetadataLog h WHERE h.pk = :pk"),
  //find the most recent inode mutation
  @NamedQuery(name = "HdfsMetadataLog.findMostRecentMutation",
          query
          = "SELECT h FROM HdfsMetadataLog h WHERE h.pk.datasetId = :datasetid "
                  + "AND h.pk.inodeid = :inodeid ORDER BY h.pk.ltime DESC")
})
public class HdfsMetadataLog implements EntityIntf, Serializable {

  @EmbeddedId
  private HdfsMetadataLogPK pk;

  @Basic(optional = false)
  @NotNull
  @Column(name = "operation")
  private int operation;

  public HdfsMetadataLog() {
    this.pk = new HdfsMetadataLogPK();
  }

  public HdfsMetadataLog(HdfsMetadataLogPK pk, int operationn) {
    this.pk = pk;
    this.operation = operationn;
  }

  public HdfsMetadataLog(HdfsMetadataLog log){
    this.pk = new HdfsMetadataLogPK(log.getHdfsMetadataLogPK());
    this.operation = log.getOperation();
  }
  
  public void copy(HdfsMetadataLog log) {
    this.pk.copy(log.getHdfsMetadataLogPK());
    this.operation = log.getOperation();
  }

  public HdfsMetadataLogPK getHdfsMetadataLogPK() {
    return this.pk;
  }

  public int getOperation() {
    return this.operation;
  }

  public void setHdfsMetadataLogPK(HdfsMetadataLogPK pk) {
    this.pk = pk;
  }

  public void setOperation(int operationn) {
    this.operation = operationn;
  }

  @Override
  public Integer getId() {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public void setId(Integer id) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public void copy(EntityIntf entity) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.HdfsMetadataLog[ pk= " + this.pk + " ]";
  }
}
