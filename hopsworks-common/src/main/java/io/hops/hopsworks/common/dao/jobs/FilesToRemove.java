/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.jobs;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "hopsworks.files_to_remove")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FilesToRemove.findAll",
      query
      = "SELECT j FROM FilesToRemove j"),
  @NamedQuery(name = "FilesToRemove.findByExecutionId",
      query
      = "SELECT j FROM FilesToRemove j WHERE j.filesToRemovePK.executionId = :executionId"),
  @NamedQuery(name = "FilesToRemove.findByPath",
      query
      = "SELECT j FROM FilesToRemove j WHERE j.filesToRemovePK.filepath = :filepath")})
public class FilesToRemove implements Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  protected FilesToRemovePK filesToRemovePK;

  @JoinColumn(name = "execution_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  @ManyToOne(optional = false)
  private Execution execution;

  public FilesToRemove() {
  }

  public FilesToRemove(FilesToRemovePK filesToRemovePK) {
    this.filesToRemovePK = filesToRemovePK;
  }

  public FilesToRemove(long executionId, String path) {
    this.filesToRemovePK = new FilesToRemovePK(executionId, path);
  }

  public FilesToRemovePK getFilesToRemovePK() {
    return filesToRemovePK;
  }

  public void setFilesToRemovePK(FilesToRemovePK filesToRemovePK) {
    this.filesToRemovePK = filesToRemovePK;
  }

  @XmlTransient
  @JsonIgnore
  public Execution getExecution() {
    return execution;
  }

  public void setExecution(Execution execution) {
    this.execution = execution;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (filesToRemovePK != null ? filesToRemovePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof FilesToRemove)) {
      return false;
    }
    FilesToRemove other = (FilesToRemove) object;
    return !((this.filesToRemovePK == null && other.filesToRemovePK != null) || (this.filesToRemovePK != null
        && !this.filesToRemovePK.equals(other.filesToRemovePK)));
  }

  @Override
  public String toString() {
    return "se.kth.bbc.job.FilesToRemove[ filesToRemovePK=" + filesToRemovePK + " ]";
  }

}
