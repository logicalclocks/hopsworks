package io.hops.hopsworks.common.dao.log.meta;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.metadata.Metadata;
import io.hops.hopsworks.common.dao.metadata.MetadataPK;
import io.hops.hopsworks.common.dao.metadata.SchemalessMetadata;
import io.hops.hopsworks.common.dao.metadata.SchemalessMetadataPK;

@Entity
@Table(name = "hopsworks.meta_log")
public class MetaLog implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "meta_pk1")
  private Integer metaPk1;

  @Basic(optional = false)
  @NotNull
  @Column(name = "meta_pk2")
  private Integer metaPk2;

  @Basic(optional = false)
  @NotNull
  @Column(name = "meta_pk3")
  private Integer metaPk3;

  @Basic(optional = false)
  @NotNull
  @Column(name = "meta_type")
  private MetaType metaType;

  @Basic(optional = false)
  @NotNull
  @Column(name = "meta_op_type")
  private OperationType metaOpType;

  public MetaLog() {

  }

  public MetaLog(Metadata metaData, OperationType opType) {
    this.metaType = MetaType.SchemaBased;
    MetadataPK pk = metaData.getMetadataPK();
    this.metaPk1 = pk.getId();
    this.metaPk2 = pk.getFieldid();
    this.metaPk3 = pk.getTupleid();
    this.metaOpType = opType;
  }

  public MetaLog(SchemalessMetadata metaData, OperationType opType) {
    this.metaType = MetaType.SchemaLess;
    SchemalessMetadataPK pk = metaData.getPK();
    this.metaPk1 = pk.getId();
    this.metaPk2 = pk.getInodeId();
    this.metaPk3 = pk.getInodeParentId();
    this.metaOpType = opType;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getMetaPk1() {
    return metaPk1;
  }

  public void setMetaPk1(Integer metaPk1) {
    this.metaPk1 = metaPk1;
  }

  public Integer getMetaPk2() {
    return metaPk2;
  }

  public void setMetaPk2(Integer metaPk2) {
    this.metaPk2 = metaPk2;
  }

  public Integer getMetaPk3() {
    return metaPk3;
  }

  public void setMetaPk3(Integer metaPk3) {
    this.metaPk3 = metaPk3;
  }

  public MetaType getMetaType() {
    return metaType;
  }

  public void setMetaType(MetaType metaType) {
    this.metaType = metaType;
  }

  public OperationType getMetaOpType() {
    return metaOpType;
  }

  public void setMetaOpType(OperationType metaOpType) {
    this.metaOpType = metaOpType;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 73 * hash + Objects.hashCode(this.id);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final MetaLog other = (MetaLog) obj;
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return true;
  }

}
