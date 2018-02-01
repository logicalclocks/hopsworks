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

package io.hops.hopsworks.common.dao.kagent;

import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Status;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServiceStatusDTO {

  private static final long serialVersionUID = 1L;
  private String group;
  private String service;
  private Status status;

  public ServiceStatusDTO() {
  }

  public ServiceStatusDTO(String group, String service, Status status) {
    this.service = service;
    this.group = group;
    this.status = status;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Health getHealth() {
    if (status == Status.Failed || status == Status.Stopped) {
      return Health.Bad;
    }
    return Health.Good;
  }
}
