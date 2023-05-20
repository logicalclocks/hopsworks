/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
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
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "validation_report", catalog = "hopsworks")
@NamedQueries({
  @NamedQuery(
    name = "ValidationReport.findByFeaturegroup",
    query = "SELECT vr FROM ValidationReport vr WHERE vr.featuregroup=:featuregroup"
  ),
  @NamedQuery(name = "ValidationReport.findById", query = "SELECT vr FROM ValidationReport vr WHERE vr.id = :id"),
  @NamedQuery(
    name = "ValidationReport.findByFeaturegroupOrderedByDescDate",
    query = "SELECT vr FROM ValidationReport vr WHERE vr.featuregroup=:featuregroup ORDER BY vr.validationTime DESC"
  )})
@XmlRootElement
public class ValidationReport implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featuregroup featuregroup;

  @Basic(optional = false)
  @NotNull
  @Column(name = "success")
  private Boolean success;

  @Basic
  @Column(name = "statistics")
  private String statistics;

  @Basic
  @Column(name = "file_name")
  @Size(max = 255)
  private String fileName;

  @Basic
  @Column(name = "meta")
  private String meta;

  @Basic
  @Column(name = "evaluation_parameters")
  private String evaluationParameters;

  @Basic(optional = false)
  @Column(name = "validation_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date validationTime;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "ingestion_result")
  private IngestionResult ingestionResult;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "validationReport")
  private Collection<ValidationResult> validationResults;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getStatistics() {
    return statistics;
  }

  public void setStatistics(String statistics) {
    this.statistics = statistics;
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getEvaluationParameters() {
    return evaluationParameters;
  }

  public void setEvaluationParameters(String evaluationParameters) {
    this.evaluationParameters = evaluationParameters;
  }

  public Date getValidationTime() {
    return validationTime;
  }

  public void setValidationTime(Date validationTime) {
    this.validationTime = validationTime;
  }

  public IngestionResult getIngestionResult() {
    return ingestionResult;
  }

  public void setIngestionResult(IngestionResult ingestionResult) {
    this.ingestionResult = ingestionResult;
  }

  public Featuregroup getFeaturegroup() {
    return featuregroup;
  }

  public void setFeaturegroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }

  public Collection<ValidationResult> getValidationResults() {
    return validationResults;
  }

  public void setValidationResults(
    Collection<ValidationResult> validationResults) {
    this.validationResults = validationResults;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValidationReport that = (ValidationReport) o;
    return Objects.equals(id, that.id) && Objects.equals(success, that.success) && Objects.equals(meta, that.meta) 
      && Objects.equals(validationResults, that.validationResults);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, success, statistics, meta, validationResults);
  }
}
