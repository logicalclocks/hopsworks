/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.dao.kafka.schemas;

import io.hops.hopsworks.persistence.entity.kafka.schemas.Subjects;

import javax.xml.bind.annotation.XmlRootElement;

/*
 * Confluent Schema Registry does not follow the Hopsworks RESTful
 * guidelines so SubjectDTO doesn't extend RestDTO
 */
@XmlRootElement
public class SubjectDTO {
  
  private Integer id;
  private String subject;
  private Integer version;
  private String schema;
  
  public SubjectDTO() {
  }
  
  public SubjectDTO(String schema) {
    this.schema = schema;
  }
  
  public SubjectDTO(Integer id) {
    this.id = id;
  }
  
  public SubjectDTO(Integer id, String subject, Integer version, String schema) {
    this.id = id;
    this.subject = subject;
    this.version = version;
    this.schema = schema;
  }
  
  public SubjectDTO(Subjects subject) {
    this.id = subject.getSchema().getId();
    this.subject = subject.getSubject();
    this.version = subject.getVersion();
    this.schema = subject.getSchema().getSchema();
  }
  
  public String getSubject() {
    return subject;
  }
  
  public void setSubject(String subject) {
    this.subject = subject;
  }
  
  public Integer getVersion() {
    return version;
  }
  
  public void setVersion(Integer version) {
    this.version = version;
  }
  
  public String getSchema() {
    return schema;
  }
  
  public void setSchema(String schema) {
    this.schema = schema;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
}
