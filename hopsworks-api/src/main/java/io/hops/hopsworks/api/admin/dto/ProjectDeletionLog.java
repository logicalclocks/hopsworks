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

package io.hops.hopsworks.api.admin.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class ProjectDeletionLog implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private String successLog;
  private String errorLog;
  
  public ProjectDeletionLog() {
  }
  
  public ProjectDeletionLog(String successLog, String errorLog) {
    this.successLog = successLog;
    this.errorLog = errorLog;
  }
  
  public String getSuccessLog() {
    return successLog;
  }
  
  public void setSuccessLog(String successLog) {
    this.successLog = successLog;
  }
  
  public String getErrorLog() {
    return errorLog;
  }
  
  public void setErrorLog(String errorLog) {
    this.errorLog = errorLog;
  }
}
