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

package io.hops.hopsworks.common.dao.zeppelin;

import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.zeppelin_interpreter_confs")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ZeppelinInterpreterConfs.findAll",
          query
          = "SELECT z FROM ZeppelinInterpreterConfs z"),
  @NamedQuery(name = "ZeppelinInterpreterConfs.findByProject",
          query
          = "SELECT z FROM ZeppelinInterpreterConfs z WHERE z.projectId = :projectId"),
  @NamedQuery(name = "ZeppelinInterpreterConfs.findByLastUpdate",
          query
          = "SELECT z FROM ZeppelinInterpreterConfs z WHERE z.lastUpdate = :lastUpdate")})
public class ZeppelinInterpreterConfs implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "last_update")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdate;
  @Basic(optional = false)
  @NotNull
  @Lob
  @Size(min = 1,
      max = 65535)
  @Column(name = "interpreter_conf")
  private String interpreterConf;
  @JoinColumn(name = "project_id",
      referencedColumnName = "id")
  @OneToOne(optional = false)
  private Project projectId;

  public ZeppelinInterpreterConfs() {
  }

  public ZeppelinInterpreterConfs(Project projectId, String interpreterConf) {
    this.projectId = projectId;
    this.interpreterConf = interpreterConf;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public String getInterpreterConf() {
    return interpreterConf;
  }

  public void setInterpreterConf(String interpreterConf) {
    this.interpreterConf = interpreterConf;
  }

  public Project getProjectId() {
    return projectId;
  }

  public void setProjectId(Project projectId) {
    this.projectId = projectId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ZeppelinInterpreterConfs)) {
      return false;
    }
    ZeppelinInterpreterConfs other = (ZeppelinInterpreterConfs) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.zeppelin.util.ZeppelinInterpreterConfs[ id=" + id + " ]";
  }

}
