/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.jobs;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "hopsworks.job_output_files")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JobOutputFile.findAll",
          query
          = "SELECT j FROM JobOutputFile j"),
  @NamedQuery(name = "JobOutputFile.findByExecutionId",
          query
          = "SELECT j FROM JobOutputFile j WHERE j.jobOutputFilePK.executionId = :executionId"),
  @NamedQuery(name = "JobOutputFile.findByName",
          query
          = "SELECT j FROM JobOutputFile j WHERE j.jobOutputFilePK.name = :name"),
  @NamedQuery(name = "JobOutputFile.findByPath",
          query
          = "SELECT j FROM JobOutputFile j WHERE j.path = :path"),
  @NamedQuery(name = "JobOutputFile.findByNameAndExecutionId",
          query
          = "SELECT j FROM JobOutputFile j WHERE j.jobOutputFilePK.name = :name "
          + "AND j.jobOutputFilePK.executionId = :executionId")})
public class JobOutputFile implements Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  protected JobOutputFilePK jobOutputFilePK;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "path")
  private String path;

  @JoinColumn(name = "execution_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private Execution execution;

  public JobOutputFile() {
  }

  public JobOutputFile(JobOutputFilePK jobOutputFilePK) {
    this.jobOutputFilePK = jobOutputFilePK;
  }

  public JobOutputFile(JobOutputFilePK jobOutputFilePK, String path) {
    this.jobOutputFilePK = jobOutputFilePK;
    this.path = path;
  }

  public JobOutputFile(long executionId, String name) {
    this.jobOutputFilePK = new JobOutputFilePK(executionId, name);
  }

  public JobOutputFilePK getJobOutputFilePK() {
    return jobOutputFilePK;
  }

  public void setJobOutputFilePK(JobOutputFilePK jobOutputFilePK) {
    this.jobOutputFilePK = jobOutputFilePK;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
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
    hash += (jobOutputFilePK != null ? jobOutputFilePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JobOutputFile)) {
      return false;
    }
    JobOutputFile other = (JobOutputFile) object;
    if ((this.jobOutputFilePK == null && other.jobOutputFilePK != null)
            || (this.jobOutputFilePK != null && !this.jobOutputFilePK.equals(
                    other.jobOutputFilePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.job.JobOutputFile[ jobOutputFilePK=" + jobOutputFilePK
            + " ]";
  }

}
