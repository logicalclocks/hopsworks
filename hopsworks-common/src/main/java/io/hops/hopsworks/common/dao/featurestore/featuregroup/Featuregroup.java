/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.featurestore.featuregroup;

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatistic;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.external_sql_query.FeaturestoreExternalSQLQuery;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 * Entity class representing the feature_group table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_group", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Featuregroup.findAll", query = "SELECT fg FROM Featuregroup fg"),
    @NamedQuery(name = "Featuregroup.findById", query = "SELECT fg FROM Featuregroup fg WHERE fg.id = :id"),
    @NamedQuery(name = "Featuregroup.findByFeaturestore", query = "SELECT fg FROM Featuregroup fg " +
        "WHERE fg.featurestore = :featurestore"),
    @NamedQuery(name = "Featuregroup.findByFeaturestoreAndId", query = "SELECT fg FROM Featuregroup fg " +
        "WHERE fg.featurestore = :featurestore AND fg.id = :id")})
public class Featuregroup implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featurestore featurestore;
  @JoinColumn(name = "job_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Jobs job;
  @Basic(optional = false)
  @NotNull
  @Column(name = "hdfs_user_id")
  private Integer hdfsUserId;
  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @JoinColumn(name = "creator", referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users creator;
  @Basic(optional = false)
  @Column(name = "hive_tbl_id")
  private Long hiveTblId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "version")
  private Integer version;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featuregroup")
  private Collection<FeaturestoreStatistic> statistics;
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "feature_group_type")
  private FeaturegroupType featuregroupType = FeaturegroupType.CACHED_FEATURE_GROUP;
  @JoinColumn(name = "feature_store_external_sql_query_id", referencedColumnName = "id")
  private FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery;


  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Featurestore getFeaturestore() {
    return featurestore;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  public Integer getHdfsUserId() {
    return hdfsUserId;
  }

  public void setHdfsUserId(Integer hdfsUserId) {
    this.hdfsUserId = hdfsUserId;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
  }

  public Jobs getJob() {
    return job;
  }

  public void setJob(Jobs job) {
    this.job = job;
  }

  public Long getHiveTblId() {
    return hiveTblId;
  }

  public void setHiveTblId(Long hiveTblId) {
    this.hiveTblId = hiveTblId;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Collection<FeaturestoreStatistic> getStatistics() {
    return statistics;
  }

  public void setStatistics(Collection<FeaturestoreStatistic> statistics) {
    this.statistics = statistics;
  }
  
  public FeaturegroupType getFeaturegroupType() {
    return featuregroupType;
  }
  
  public void setFeaturegroupType(FeaturegroupType featuregroupType) {
    this.featuregroupType = featuregroupType;
  }
  
  public FeaturestoreExternalSQLQuery getFeaturestoreExternalSQLQuery() {
    return featurestoreExternalSQLQuery;
  }
  
  public void setFeaturestoreExternalSQLQuery(
    FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery) {
    this.featurestoreExternalSQLQuery = featurestoreExternalSQLQuery;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Featuregroup)) return false;

    Featuregroup that = (Featuregroup) o;

    if (id != null)
      if (!id.equals(that.id)) return false;
    if (job != null)
      if (!job.equals(that.job)) return false;
    if (!featurestore.equals(that.featurestore)) return false;
    if (!hdfsUserId.equals(that.hdfsUserId)) return false;
    if (!version.equals(that.version)) return false;
    if (!hiveTblId.equals(that.hiveTblId)) return false;
    if (created != null)
      if (!created.equals(that.created)) return false;
    if (!creator.equals(that.creator)) return false;
    if (!featurestoreExternalSQLQuery.equals(that.featurestoreExternalSQLQuery)) return false;
    if (featuregroupType != null ? !featuregroupType.equals(that.featuregroupType) :
      that.featuregroupType != null) return false;
    return featurestore.equals(that.featurestore);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + featurestore.hashCode();
    result = 31 * result + hdfsUserId.hashCode();
    result = 31 * result + hiveTblId.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (job != null ? job.hashCode() : 0);
    result = 31 * result + creator.hashCode();
    result = 31 * result + (featuregroupType != null ? featuregroupType.hashCode() : 0);
    result = 31 * result + featurestoreExternalSQLQuery.hashCode();
    return result;
  }
}
