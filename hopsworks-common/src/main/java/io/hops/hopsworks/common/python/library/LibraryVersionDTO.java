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
package io.hops.hopsworks.common.python.library;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.Objects;

@XmlRootElement
public class LibraryVersionDTO {
  
  private String version;
  private Date uploadTime;
  
  public LibraryVersionDTO() {
  }
  
  public LibraryVersionDTO(String version) {
    this.version = version;
  }
  
  public LibraryVersionDTO(String version, Date uploadTime) {
    this.version = version;
    this.uploadTime = uploadTime;
  }
  
  public String getVersion() {
    return version;
  }
  
  public void setVersion(String version) {
    this.version = version;
  }
  
  public Date getUploadTime() {
    return uploadTime;
  }
  
  public void setUploadTime(Date uploadTime) {
    this.uploadTime = uploadTime;
  }
  
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LibraryVersionDTO that = (LibraryVersionDTO) o;
    return version.equals(that.version);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(version);
  }
}
