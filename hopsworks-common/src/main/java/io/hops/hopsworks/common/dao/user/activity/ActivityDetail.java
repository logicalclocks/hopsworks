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

package io.hops.hopsworks.common.dao.user.activity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Entity class mapped to the view "activity_details". Used to present the
 * activity in a complete way to the user in the activity log.
 */
@Entity
@Table(name = "hopsworks.activity_details")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ActivityDetail.findAll",
          query = "SELECT a FROM ActivityDetail a ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.findById",
          query = "SELECT a FROM ActivityDetail a WHERE a.id = :id"),
  @NamedQuery(name = "ActivityDetail.findByPerformedByEmail",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.performedByEmail = :performedByEmail ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.findByPerformedByName",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.performedByName = :performedByName ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.findByDescription",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.description = :description ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.findByStudyname",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.studyname = :studyname ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.findByTimestamp",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.timestamp = :timestamp ORDER BY a.timestamp DESC")})
public class ActivityDetail implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private int id;
  @Size(max = 255)
  @Column(name = "performed_by_email")
  private String performedByEmail;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "performed_by_name")
  private String performedByName;
  @Size(max = 128)
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "studyname")
  private String studyname;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;

  public ActivityDetail() {
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getPerformedByEmail() {
    return performedByEmail;
  }

  public void setPerformedByEmail(String performedByEmail) {
    this.performedByEmail = performedByEmail;
  }

  public String getPerformedByName() {
    return performedByName;
  }

  public void setPerformedByName(String performedByName) {
    this.performedByName = performedByName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStudyname() {
    return studyname;
  }

  public void setStudyname(String studyname) {
    this.studyname = studyname;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

}
