/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.descriptivestatistics.DescriptiveStatisticsComparisonConfig;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.result.FeatureMonitoringResult;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.scheduler.JobScheduleV2;

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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

@Entity
@Table(name = "feature_monitoring_config", catalog = "hopsworks")
@NamedQueries({
  @NamedQuery(name = "FeatureMonitoringConfiguration.findByFeatureGroup",
    query = "SELECT config FROM FeatureMonitoringConfiguration config" +
      " WHERE config.featureGroup=:featureGroup ORDER BY config.name DESC"),
  @NamedQuery(name = "FeatureMonitoringConfiguration.findByFeatureGroupAndFeatureName",
    query = "SELECT config FROM FeatureMonitoringConfiguration config" +
      " WHERE config.featureGroup=:featureGroup AND config.featureName=:featureName ORDER BY config.name DESC"),
  @NamedQuery(name = "FeatureMonitoringConfiguration.findByFeatureGroupAndConfigId",
    query = "SELECT config FROM FeatureMonitoringConfiguration config" +
      " WHERE config.featureGroup=:featureGroup AND config.id=:configId"),
  @NamedQuery(name = "FeatureMonitoringConfiguration.findByFeatureView",
    query = "SELECT config FROM FeatureMonitoringConfiguration config" +
      " WHERE config.featureView=:featureView ORDER BY config.name DESC"),
  @NamedQuery(name = "FeatureMonitoringConfiguration.findByFeatureViewAndFeatureName",
    query = "SELECT config FROM FeatureMonitoringConfiguration config" +
      " WHERE config.featureView=:featureView AND config.featureName=:featureName ORDER BY config.name DESC"),
  @NamedQuery(name = "FeatureMonitoringConfiguration.findById",
    query = "SELECT config FROM FeatureMonitoringConfiguration config" + " WHERE config.id=:configId"),
  @NamedQuery(name = "FeatureMonitoringConfiguration.findByJobId",
    query = "SELECT config FROM FeatureMonitoringConfiguration config WHERE config.job.id=:jobId"),
  @NamedQuery(name = "FeatureMonitoringConfiguration.findByName",
    query = "SELECT config FROM FeatureMonitoringConfiguration config" + " WHERE config.name=:name"),
  @NamedQuery(name = "FeatureMonitoringConfiguration.findByFeatureGroupAndName",
    query = "SELECT config FROM FeatureMonitoringConfiguration config" +
      " WHERE config.name=:name AND config.featureGroup=:featureGroup"),
  @NamedQuery(name = "FeatureMonitoringConfiguration.findByFeatureViewAndName",
    query = "SELECT config FROM FeatureMonitoringConfiguration config" +
      " WHERE config.name=:name AND config.featureView=:featureView"),})
@XmlRootElement
public class FeatureMonitoringConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featureGroup;
  
  @JoinColumn(name = "feature_view_id", referencedColumnName = "id")
  private FeatureView featureView;
  
  @NotNull
  @Basic(optional = false)
  @Column(name = "feature_name")
  @Size(max = 63)
  private String featureName;
  
  @NotNull
  @Basic
  @Column(name = "name")
  @Size(max = 63)
  private String name;
  
  @Basic
  @Column(name = "description")
  @Size(max = 2000)
  private String description;
  
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "feature_monitoring_type")
  private FeatureMonitoringType featureMonitoringType;
  
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "job_id", referencedColumnName = "id")
  private Jobs job;
  
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "job_schedule_id", referencedColumnName = "id")
  private JobScheduleV2 jobSchedule;
  
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "detection_window_config_id", referencedColumnName = "id")
  private MonitoringWindowConfiguration detectionWindowConfig;
  
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "reference_window_config_id", referencedColumnName = "id")
  private MonitoringWindowConfiguration referenceWindowConfig;
  
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "statistics_comparison_config_id", referencedColumnName = "id")
  private DescriptiveStatisticsComparisonConfig dsComparisonConfig;
  
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featureMonitoringConfig")
  private Collection<FeatureMonitoringResult> results;
  
  public Integer getId() {
    return this.id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Featuregroup getFeatureGroup() {
    return this.featureGroup;
  }
  
  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }
  
  public FeatureView getFeatureView() {
    return this.featureView;
  }
  
  public void setFeatureView(FeatureView featureView) {
    this.featureView = featureView;
  }
  
  public Jobs getJob() {
    return job;
  }
  
  public void setJob(Jobs job) {
    this.job = job;
  }
  
  public String getFeatureName() {
    return this.featureName;
  }
  
  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }
  
  public String getName() {
    return this.name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getDescription() {
    return this.description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public JobScheduleV2 getJobSchedule() {
    return this.jobSchedule;
  }

  public void setJobSchedule(JobScheduleV2 jobSchedule) {
    this.jobSchedule = jobSchedule;
  }
  
  public FeatureMonitoringType getFeatureMonitoringType() {
    return this.featureMonitoringType;
  }
  
  public void setFeatureMonitoringType(FeatureMonitoringType featureMonitoringType) {
    this.featureMonitoringType = featureMonitoringType;
  }
  
  public MonitoringWindowConfiguration getDetectionWindowConfig() {
    return this.detectionWindowConfig;
  }
  
  public void setDetectionWindowConfig(MonitoringWindowConfiguration detectionWindowConfig) {
    this.detectionWindowConfig = detectionWindowConfig;
  }
  
  public MonitoringWindowConfiguration getReferenceWindowConfig() {
    return this.referenceWindowConfig;
  }
  
  public void setReferenceWindowConfig(MonitoringWindowConfiguration referenceWindowConfig) {
    this.referenceWindowConfig = referenceWindowConfig;
  }
  
  public DescriptiveStatisticsComparisonConfig getDsComparisonConfig() {
    return this.dsComparisonConfig;
  }
  
  public void setDsComparisonConfig(DescriptiveStatisticsComparisonConfig dsComparisonConfig) {
    this.dsComparisonConfig = dsComparisonConfig;
  }
  
  public Collection<FeatureMonitoringResult> getResults() {
    return this.results;
  }
  
  public void setResults(Collection<FeatureMonitoringResult> results) {
    this.results = results;
  }
  
  
  // Convenience
  public String getEntityName() {
    if (featureGroup != null) {
      return featureGroup.getName();
    } else {
      return featureView.getName();
    }
  }
  
  public Integer getEntityVersion() {
    if (featureGroup != null) {
      return featureGroup.getVersion();
    } else {
      return featureView.getVersion();
    }
  }
  
  public Integer getEntityId() {
    if (featureGroup != null) {
      return featureGroup.getId();
    } else {
      return featureView.getId();
    }
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof FeatureMonitoringConfiguration)) {
      return false;
    }
    FeatureMonitoringConfiguration featureMonitoringConfiguration = (FeatureMonitoringConfiguration) o;
    Integer entityId = getEntityId();
    Integer otherEntityId = featureMonitoringConfiguration.getEntityId();
    return Objects.equals(id, featureMonitoringConfiguration.id) &&
      Objects.equals(name, featureMonitoringConfiguration.name) &&
      Objects.equals(entityId, otherEntityId) &&
      Objects.equals(featureName, featureMonitoringConfiguration.featureName) &&
      Objects.equals(description, featureMonitoringConfiguration.description) &&
      Objects.equals(featureMonitoringType, featureMonitoringConfiguration.featureMonitoringType);
  }
  
  @Override
  public int hashCode() {
    Integer entityId = getEntityId();
    return Objects.hash(id, name, description, featureMonitoringType, featureName, entityId, job.getId());
  }
  
}