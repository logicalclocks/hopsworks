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

package io.hops.hopsworks.common.dao.airflow;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@XmlRootElement
@Table(name = "dag", catalog = "airflow")
@NamedQueries({
    @NamedQuery(name = "AirflowDag.getAll",
                query = "SELECT d FROM AirflowDag d"),
    @NamedQuery(name = "AirflowDag.filterByOwner",
                query = "SELECT d FROM AirflowDag d WHERE d.owners = :owner")
  })
public class AirflowDag implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Id
  @Column(name = "dag_id")
  private String id;
  @Column(name = "is_paused")
  private Boolean paused;
  @Column(name = "is_subdag")
  private Boolean subdag;
  @Column(name = "is_active")
  private Boolean active;
  @Column(name = "last_scheduler_run")
  private Date lastSchedulerRun;
  @Column(name = "last_pickled")
  private Date lastPickled;
  @Column(name = "last_expired")
  private Date lastExpired;
  @Column(name = "scheduler_lock")
  private Boolean schedulerLock;
  @Column(name = "pickle_id")
  private Integer pickleID;
  @Column(name = "fileloc")
  private String fileLocation;
  private String owners;
  
  public AirflowDag() {
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public Boolean getPaused() {
    return paused;
  }
  
  public void setPaused(Boolean paused) {
    this.paused = paused;
  }
  
  public Boolean getSubdag() {
    return subdag;
  }
  
  public void setSubdag(Boolean subdag) {
    this.subdag = subdag;
  }
  
  public Boolean getActive() {
    return active;
  }
  
  public void setActive(Boolean active) {
    this.active = active;
  }
  
  public Date getLastSchedulerRun() {
    return lastSchedulerRun;
  }
  
  public void setLastSchedulerRun(Date lastSchedulerRun) {
    this.lastSchedulerRun = lastSchedulerRun;
  }
  
  public Date getLastPickled() {
    return lastPickled;
  }
  
  public void setLastPickled(Date lastPickled) {
    this.lastPickled = lastPickled;
  }
  
  public Date getLastExpired() {
    return lastExpired;
  }
  
  public void setLastExpired(Date lastExpired) {
    this.lastExpired = lastExpired;
  }
  
  public Boolean getSchedulerLock() {
    return schedulerLock;
  }
  
  public void setSchedulerLock(Boolean schedulerLock) {
    this.schedulerLock = schedulerLock;
  }
  
  public Integer getPickleID() {
    return pickleID;
  }
  
  public void setPickleID(Integer pickleID) {
    this.pickleID = pickleID;
  }
  
  public String getFileLocation() {
    return fileLocation;
  }
  
  public void setFileLocation(String fileLocation) {
    this.fileLocation = fileLocation;
  }
  
  public String getOwners() {
    return owners;
  }
  
  public void setOwners(String owners) {
    this.owners = owners;
  }
}
