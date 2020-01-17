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
import io.hops.hopsworks.common.dao.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.common.dao.featurestore.jobs.FeaturestoreJob;
import io.hops.hopsworks.common.dao.featurestore.statistics.columns.StatisticColumn;
import io.hops.hopsworks.common.dao.featurestore.statistics.FeaturestoreStatistic;
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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

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
        "WHERE fg.featurestore = :featurestore AND fg.id = :id"),
    @NamedQuery(name = "Featuregroup.findByFeaturestoreAndNameVersion", query = "SELECT fg FROM Featuregroup fg " +
        "WHERE fg.featurestore = :featurestore AND fg.name = :name AND fg.version = :version"),
    @NamedQuery(name = "Featuregroup.findByFeaturestoreAndName", query = "SELECT fg FROM Featuregroup fg " +
    "WHERE fg.featurestore = :featurestore AND fg.name = :name")})
public class Featuregroup implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "name")
  private String name;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featurestore featurestore;
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
  @NotNull
  @Column(name = "version")
  private Integer version;
  @Basic(optional = false)
  @NotNull
  @Column(name = "desc_stats_enabled")
  private boolean descStatsEnabled = true;
  @Basic(optional = false)
  @NotNull
  @Column(name = "feat_corr_enabled")
  private boolean featCorrEnabled = true;
  @Basic(optional = false)
  @NotNull
  @Column(name = "feat_hist_enabled")
  private boolean featHistEnabled = true;
  @Basic(optional = false)
  @NotNull
  @Column(name = "cluster_analysis_enabled")
  private boolean clusterAnalysisEnabled = true;
  @Basic(optional = false)
  @NotNull
  @Column(name = "num_bins")
  private Integer numBins = 5;
  @Basic(optional = false)
  @NotNull
  @Column(name = "num_clusters")
  private Integer numClusters = 20;
  @Basic(optional = false)
  @NotNull
  @Column(name = "corr_method")
  private String corrMethod = "pearson";
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featuregroup")
  private Collection<FeaturestoreStatistic> statistics;
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "feature_group_type")
  private FeaturegroupType featuregroupType = FeaturegroupType.CACHED_FEATURE_GROUP;
  @JoinColumn(name = "on_demand_feature_group_id", referencedColumnName = "id")
  @OneToOne
  private OnDemandFeaturegroup onDemandFeaturegroup;
  @JoinColumn(name = "cached_feature_group_id", referencedColumnName = "id")
  @OneToOne
  private CachedFeaturegroup cachedFeaturegroup;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featuregroup")
  private Collection<FeaturestoreJob> jobs;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featuregroup")
  private Collection<StatisticColumn> statisticColumns;

  public static long getSerialVersionUID() {
    return serialVersionUID;
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

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public boolean isDescStatsEnabled() {
    return descStatsEnabled;
  }

  public void setDescStatsEnabled(boolean descStatsEnabled) {
    this.descStatsEnabled = descStatsEnabled;
  }

  public boolean isFeatCorrEnabled() {
    return featCorrEnabled;
  }

  public void setFeatCorrEnabled(boolean featCorrEnabled) {
    this.featCorrEnabled = featCorrEnabled;
  }

  public boolean isFeatHistEnabled() {
    return featHistEnabled;
  }

  public void setFeatHistEnabled(boolean featHistEnabled) {
    this.featHistEnabled = featHistEnabled;
  }

  public boolean isClusterAnalysisEnabled() {
    return clusterAnalysisEnabled;
  }

  public void setClusterAnalysisEnabled(boolean clusterAnalysisEnabled) {
    this.clusterAnalysisEnabled = clusterAnalysisEnabled;
  }
  
  public Integer getNumBins() {
    return numBins;
  }
  
  public void setNumBins(Integer numBins) {
    this.numBins = numBins;
  }
  
  public Integer getNumClusters() {
    return numClusters;
  }
  
  public void setNumClusters(Integer numClusters) {
    this.numClusters = numClusters;
  }
  
  public String getCorrMethod() {
    return corrMethod;
  }
  
  public void setCorrMethod(String corrMethod) {
    this.corrMethod = corrMethod;
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

  public OnDemandFeaturegroup getOnDemandFeaturegroup() {
    return onDemandFeaturegroup;
  }

  public void setOnDemandFeaturegroup(OnDemandFeaturegroup onDemandFeaturegroup) {
    this.onDemandFeaturegroup = onDemandFeaturegroup;
  }

  public CachedFeaturegroup getCachedFeaturegroup() {
    return cachedFeaturegroup;
  }

  public void setCachedFeaturegroup(CachedFeaturegroup cachedFeaturegroup) {
    this.cachedFeaturegroup = cachedFeaturegroup;
  }
  
  public Collection<FeaturestoreJob> getJobs() {
    return jobs;
  }
  
  public void setJobs(Collection<FeaturestoreJob> jobs) {
    this.jobs = jobs;
  }

  public Collection<StatisticColumn> getStatisticColumns() {
    return statisticColumns;
  }
  
  public void setStatisticColumns(Collection<StatisticColumn> statisticColumns) {
    this.statisticColumns = statisticColumns;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Featuregroup that = (Featuregroup) o;

    if (descStatsEnabled != that.descStatsEnabled) return false;
    if (featCorrEnabled != that.featCorrEnabled) return false;
    if (featHistEnabled != that.featHistEnabled) return false;
    if (clusterAnalysisEnabled != that.clusterAnalysisEnabled) return false;
    if (!Objects.equals(id, that.id)) return false;
    if (!Objects.equals(name, that.name)) return false;
    if (!Objects.equals(featurestore, that.featurestore)) return false;
    if (!Objects.equals(hdfsUserId, that.hdfsUserId)) return false;
    if (!Objects.equals(created, that.created)) return false;
    if (!Objects.equals(creator, that.creator)) return false;
    if (!Objects.equals(version, that.version)) return false;
    if (!Objects.equals(numBins, that.numBins)) return false;
    if (!Objects.equals(numClusters, that.numClusters)) return false;
    if (!Objects.equals(corrMethod, that.corrMethod)) return false;
    if (!Objects.equals(statistics, that.statistics)) return false;
    if (featuregroupType != that.featuregroupType) return false;
    if (!Objects.equals(onDemandFeaturegroup, that.onDemandFeaturegroup))
      return false;
    if (!Objects.equals(cachedFeaturegroup, that.cachedFeaturegroup))
      return false;
    if (!Objects.equals(jobs, that.jobs)) return false;
    return Objects.equals(statisticColumns, that.statisticColumns);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (featurestore != null ? featurestore.hashCode() : 0);
    result = 31 * result + (hdfsUserId != null ? hdfsUserId.hashCode() : 0);
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (creator != null ? creator.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (descStatsEnabled ? 1 : 0);
    result = 31 * result + (featCorrEnabled ? 1 : 0);
    result = 31 * result + (featHistEnabled ? 1 : 0);
    result = 31 * result + (clusterAnalysisEnabled ? 1 : 0);
    result = 31 * result + (numBins != null ? numBins.hashCode() : 0);
    result = 31 * result + (numClusters != null ? numClusters.hashCode() : 0);
    result = 31 * result + (corrMethod != null ? corrMethod.hashCode() : 0);
    result = 31 * result + (statistics != null ? statistics.hashCode() : 0);
    result = 31 * result + (featuregroupType != null ? featuregroupType.hashCode() : 0);
    result = 31 * result + (onDemandFeaturegroup != null ? onDemandFeaturegroup.hashCode() : 0);
    result = 31 * result + (cachedFeaturegroup != null ? cachedFeaturegroup.hashCode() : 0);
    result = 31 * result + (jobs != null ? jobs.hashCode() : 0);
    result = 31 * result + (statisticColumns != null ? statisticColumns.hashCode() : 0);
    return result;
  }
}
