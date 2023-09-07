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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup;

import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivity;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticsConfig;
import io.hops.hopsworks.persistence.entity.user.Users;

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
    @NamedQuery(name = "Featuregroup.findAll", query = "SELECT fg FROM Featuregroup fg "
        + "WHERE (fg.onDemandFeaturegroup IS NOT null "
        + "OR fg.cachedFeaturegroup IS NOT null "
        + "OR fg.streamFeatureGroup IS NOT null)"),
    @NamedQuery(name = "Featuregroup.findById", query = "SELECT fg FROM Featuregroup fg WHERE fg.id = :id"
        + " AND (fg.onDemandFeaturegroup IS NOT null "
        + "OR fg.cachedFeaturegroup IS NOT null "
        + "OR fg.streamFeatureGroup IS NOT null)"),
    @NamedQuery(name = "Featuregroup.findByFeaturestore", query = "SELECT fg FROM Featuregroup fg " +
        "WHERE fg.featurestore = :featurestore"
        + " AND (fg.onDemandFeaturegroup IS NOT null "
        + "OR fg.cachedFeaturegroup IS NOT null "
        + "OR fg.streamFeatureGroup IS NOT null)"),
    @NamedQuery(name = "Featuregroup.countByFeaturestore", query = "SELECT count(fg.id) FROM Featuregroup fg " +
        "WHERE fg.featurestore = :featurestore"
        + " AND (fg.onDemandFeaturegroup IS NOT null "
        + "OR fg.cachedFeaturegroup IS NOT null "
        + "OR fg.streamFeatureGroup IS NOT null)"),
    @NamedQuery(name = "Featuregroup.findByFeaturestoreAndId", query = "SELECT fg FROM Featuregroup fg " +
        "WHERE fg.featurestore = :featurestore AND fg.id = :id"),
    @NamedQuery(name = "Featuregroup.findByFeaturestoreAndNameVersion", query = "SELECT fg FROM Featuregroup fg " +
        "WHERE fg.featurestore = :featurestore AND fg.name = :name AND fg.version = :version"
        + " AND (fg.onDemandFeaturegroup IS NOT null "
        + "OR fg.cachedFeaturegroup IS NOT null "
        + "OR fg.streamFeatureGroup IS NOT null)"),
    @NamedQuery(name = "Featuregroup.findByFeaturestoreAndName", query = "SELECT fg FROM Featuregroup fg " +
        "WHERE fg.featurestore = :featurestore AND fg.name = :name"
        + " AND (fg.onDemandFeaturegroup IS NOT null "
        + "OR fg.cachedFeaturegroup IS NOT null "
        + "OR fg.streamFeatureGroup IS NOT null)"),
    @NamedQuery(name = "Featuregroup.findByFeaturestoreAndNameOrderedByDescVersion", query = "SELECT fg FROM " +
        "Featuregroup fg WHERE fg.featurestore = :featurestore AND fg.name = :name "
        + " AND (fg.onDemandFeaturegroup IS NOT null "
        + "OR fg.cachedFeaturegroup IS NOT null "
        + "OR fg.streamFeatureGroup IS NOT null)"
        + " ORDER BY fg.version DESC")})
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
  @Column(name = "description")
  private String description;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featurestore featurestore;
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
  @Column(name = "event_time")
  private String eventTime;
  @Column(name = "online_enabled")
  private boolean onlineEnabled;
  @Column(name = "topic_name")
  private String topicName;
  @Column(name = "deprecated")
  private boolean deprecated;
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "feature_group_type")
  private FeaturegroupType featuregroupType = FeaturegroupType.CACHED_FEATURE_GROUP;
  @JoinColumn(name = "on_demand_feature_group_id", referencedColumnName = "id")
  @OneToOne
  private OnDemandFeaturegroup onDemandFeaturegroup;
  @JoinColumn(name = "stream_feature_group_id", referencedColumnName = "id")
  @OneToOne
  private StreamFeatureGroup streamFeatureGroup;
  @JoinColumn(name = "cached_feature_group_id", referencedColumnName = "id")
  @OneToOne
  private CachedFeaturegroup cachedFeaturegroup;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featureGroup")
  private Collection<FeaturestoreActivity> activities;
  @OneToOne(cascade = CascadeType.ALL, mappedBy = "featuregroup")
  private StatisticsConfig statisticsConfig;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featureGroup")
  private Collection<FeatureGroupAlert> featureGroupAlerts;
  @OneToOne(cascade = CascadeType.ALL, mappedBy = "featuregroup")
  private ExpectationSuite expectationSuite;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featuregroup")
  private Collection<ValidationReport> validationReports;

  public Featuregroup() { }

  public Featuregroup(Integer id) {
    this.id = id;
  }

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Featurestore getFeaturestore() {
    return featurestore;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
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
  
  public StreamFeatureGroup getStreamFeatureGroup() {
    return streamFeatureGroup;
  }
  
  public void setStreamFeatureGroup(
    StreamFeatureGroup streamFeatureGroup) {
    this.streamFeatureGroup = streamFeatureGroup;
  }
  
  public StatisticsConfig getStatisticsConfig() {
    return statisticsConfig;
  }

  public void setStatisticsConfig(StatisticsConfig statisticsConfig) {
    this.statisticsConfig = statisticsConfig;
  }

  public Collection<FeaturestoreActivity> getActivities() {
    return activities;
  }

  public void setActivities(Collection<FeaturestoreActivity> activities) {
    this.activities = activities;
  }
  
  public Collection<FeatureGroupAlert> getFeatureGroupAlerts() {
    return featureGroupAlerts;
  }
  
  public void setFeatureGroupAlerts(
      Collection<FeatureGroupAlert> featureGroupAlerts) {
    this.featureGroupAlerts = featureGroupAlerts;
  }

  public String getEventTime() {
    return eventTime;
  }

  public void setEventTime(String eventTime) {
    this.eventTime = eventTime;
  }
  
  public ExpectationSuite getExpectationSuite() {
    return expectationSuite;
  }
  
  public void setExpectationSuite(ExpectationSuite expectationSuite) {
    this.expectationSuite = expectationSuite;
  }

  public Collection<ValidationReport> getValidationReports() {
    return validationReports;
  }

  public void setValidationReports(Collection<ValidationReport> validationReports) {
    this.validationReports = validationReports;
  }

  public boolean isOnlineEnabled() {
    return onlineEnabled;
  }

  public void setOnlineEnabled(boolean onlineEnabled) {
    this.onlineEnabled = onlineEnabled;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public boolean isDeprecated() {
    return deprecated;
  }

  public void setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Featuregroup that = (Featuregroup) o;

    if (!Objects.equals(id, that.id)) return false;
    if (!Objects.equals(name, that.name)) return false;
    if (!Objects.equals(description, that.description)) return false;
    if (!Objects.equals(featurestore, that.featurestore)) return false;
    if (!Objects.equals(created, that.created)) return false;
    if (!Objects.equals(creator, that.creator)) return false;
    if (!Objects.equals(version, that.version)) return false;
    if (featuregroupType != that.featuregroupType) return false;
    if (!Objects.equals(onDemandFeaturegroup, that.onDemandFeaturegroup)) return false;
    if (!Objects.equals(cachedFeaturegroup, that.cachedFeaturegroup)) return false;
    if (!Objects.equals(streamFeatureGroup, that.streamFeatureGroup)) return false;
    if (!Objects.equals(eventTime, that.eventTime)) return false;
    if (!Objects.equals(onlineEnabled, that.onlineEnabled)) return false;
    if (!Objects.equals(topicName, that.topicName)) return false;
    if (!Objects.equals(deprecated, that.deprecated)) return false;
    if (!Objects.equals(expectationSuite, that.expectationSuite)) return false;
    return Objects.equals(statisticsConfig, that.statisticsConfig);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (featurestore != null ? featurestore.hashCode() : 0);
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (creator != null ? creator.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (featuregroupType != null ? featuregroupType.hashCode() : 0);
    result = 31 * result + (onDemandFeaturegroup != null ? onDemandFeaturegroup.hashCode() : 0);
    result = 31 * result + (cachedFeaturegroup != null ? cachedFeaturegroup.hashCode() : 0);
    result = 31 * result + (streamFeatureGroup != null ? streamFeatureGroup.hashCode() : 0);
    result = 31 * result + (statisticsConfig != null ? statisticsConfig.hashCode() : 0);
    result = 31 * result + (eventTime != null ? eventTime.hashCode() : 0);
    result = 31 * result + (onlineEnabled ? 1: 0);
    result = 31 * result + (topicName != null ? topicName.hashCode() : 0);
    result = 31 * result + (deprecated ? 1: 0);
    result = 31 * result + (expectationSuite != null ? expectationSuite.hashCode(): 0);
    return result;
  }
}
