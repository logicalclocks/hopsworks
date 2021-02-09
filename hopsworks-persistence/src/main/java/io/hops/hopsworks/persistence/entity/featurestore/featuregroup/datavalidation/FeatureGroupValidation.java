/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "feature_group_validation", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FeatureGroupValidation.findAll",
    query = "SELECT fgv FROM FeatureGroupValidation fgv"),
  @NamedQuery(name = "FeatureGroupValidation.findByFeatureGroupAndValidationTime",
    query = "SELECT fgv FROM FeatureGroupValidation fgv WHERE " +
      "fgv.featureGroup = :featureGroup AND fgv.validationTime = :validationTime")})
public class FeatureGroupValidation implements Serializable {
  
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @Basic(optional = false)
  @Column(name = "validation_time")
  @NotNull
  @Temporal(TemporalType.TIMESTAMP)
  private Date validationTime;
  
  @JoinColumns({
    @JoinColumn(name = "inode_pid",
        referencedColumnName = "parent_id"),
    @JoinColumn(name = "inode_name",
        referencedColumnName = "name"),
    @JoinColumn(name = "partition_id",
        referencedColumnName = "partition_id")})
  @ManyToOne(optional = false)
  private Inode validations_path;
  
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featureGroup;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private Status status;

  @OneToOne(mappedBy = "validation")
  private FeatureGroupCommit commit;

  public FeatureGroupValidation() {
  
  }
  
  public FeatureGroupValidation( @NotNull Date validationTime, Inode validations_path, Featuregroup featureGroup,
    @NotNull Status status) {
    this.validationTime = validationTime;
    this.validations_path = validations_path;
    this.featureGroup = featureGroup;
    this.status = status;
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
  
  public Date getValidationTime() {
    return this.validationTime;
  }
  
  public void setValidationTime(Date validationTime) {
    this.validationTime = validationTime;
  }
  
  public Inode getValidationsPath() {
    return validations_path;
  }
  
  public void setValidationsPath(Inode validations_path) {
    this.validations_path = validations_path;
  }
  
  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }
  
  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }
  
  public Status getStatus() {
    return status;
  }
  
  public void setStatus(Status status) {
    this.status = status;
  }

  public FeatureGroupCommit getCommit() {
    return commit;
  }

  public void setCommit(FeatureGroupCommit commit) {
    this.commit = commit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    FeatureGroupValidation that = (FeatureGroupValidation) o;
    
    if (!id.equals(that.id)) {
      return false;
    }
    if (!validationTime.equals(that.validationTime)) {
      return false;
    }
    if (!validations_path.equals(that.validations_path)) {
      return false;
    }
    return featureGroup != null ? featureGroup.equals(that.featureGroup) : that.featureGroup == null;
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + validationTime.hashCode();
    result = 31 * result + validations_path.hashCode();
    result = 31 * result + (featureGroup != null ? featureGroup.hashCode() : 0);
    return result;
  }
  
  public enum Status {
    NONE("None",0),
    SUCCESS("Success",1),
    WARNING("Warning",2),
    FAILURE("Failure",3);
  
    private final String name;
    private final int severity;
  
    Status(String name, int severity) {
      this.name = name;
      this.severity = severity;
    }
  
    public int getSeverity() {
      return severity;
    }
  
    public static Status fromString(String name) {
      return valueOf(name.toUpperCase());
    }
  
    public String getName() {
      return name;
    }
  
    @Override
    public String toString() {
      return name;
    }
  }
}
