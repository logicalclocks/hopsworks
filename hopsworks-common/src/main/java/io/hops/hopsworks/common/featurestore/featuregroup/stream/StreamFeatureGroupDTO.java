/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 *  Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.featurestore.featuregroup.stream;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonTypeName("streamFeatureGroupDTO")
public class StreamFeatureGroupDTO extends FeaturegroupDTO {
  private TimeTravelFormat timeTravelFormat = TimeTravelFormat.HUDI;
  private DeltaStreamerJobConf deltaStreamerJobConf;
  private Boolean onlineEnabled = true;
  
  public StreamFeatureGroupDTO() {
    super();
  }

  public StreamFeatureGroupDTO(Featuregroup featuregroup) {
    super(featuregroup);
  }
  
  public DeltaStreamerJobConf getDeltaStreamerJobConf() {
    return deltaStreamerJobConf;
  }
  
  public void setDeltaStreamerJobConf(
    DeltaStreamerJobConf deltaStreamerJobConf) {
    this.deltaStreamerJobConf = deltaStreamerJobConf;
  }
  
  public Boolean getOnlineEnabled() {
    return onlineEnabled;
  }
  
  public void setOnlineEnabled(Boolean onlineEnabled) {
    this.onlineEnabled = onlineEnabled;
  }
  
  public TimeTravelFormat getTimeTravelFormat() {
    return timeTravelFormat;
  }
  
  public void setTimeTravelFormat(
    TimeTravelFormat timeTravelFormat) {
    this.timeTravelFormat = timeTravelFormat;
  }

  @Override
  public String toString() {
    return "StreamFeatureGroupDTO{" +
      "timeTravelFormat=" + timeTravelFormat +
      ", deltaStreamerJobConf=" + deltaStreamerJobConf +
      ", onlineEnabled=" + onlineEnabled +
      '}';
  }
}
