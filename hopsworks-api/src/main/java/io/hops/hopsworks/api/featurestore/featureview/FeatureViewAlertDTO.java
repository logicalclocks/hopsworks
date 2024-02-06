/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.featureview;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlertStatus;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
@Getter
@Setter
public class FeatureViewAlertDTO extends RestDTO<FeatureViewAlertDTO> {
  private Integer id;
  private String featureStoreName;
  private FeatureStoreAlertStatus status;
  private AlertType alertType;
  private AlertSeverity severity;
  private String receiver;
  private Date created;
  private Integer featureViewId;
  private Integer featureViewVersion;
  private String featureViewName;
  
  public FeatureViewAlertDTO() {
  }
  
  @Override
  public String toString() {
    return "FeatureViewAlertDTO{" +
      "id=" + id +
      ", featureStoreName='" + featureStoreName + '\'' +
      ", status=" + status +
      ", alertType=" + alertType +
      ", severity=" + severity +
      ", receiver='" + receiver + '\'' +
      ", created=" + created +
      ", featureViewId=" + featureViewId +
      ", featureViewVersion=" + featureViewVersion +
      ", featureViewName='" + featureViewName + '\'' +
      '}';
  }
  
}