/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import org.apache.calcite.sql.JoinType;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class JoinDTO {

  private QueryDTO query;

  private List<FeatureGroupFeatureDTO> on;
  private List<FeatureGroupFeatureDTO> leftOn;
  private List<FeatureGroupFeatureDTO> rightOn;

  private JoinType type = JoinType.INNER;

  public JoinDTO() {
  }

  public JoinDTO(QueryDTO query, List<FeatureGroupFeatureDTO> on, JoinType type) {
    this.query = query;
    this.on = on;
    this.type = type;
  }

  public JoinDTO(QueryDTO query, List<FeatureGroupFeatureDTO> leftOn,
                 List<FeatureGroupFeatureDTO> rightOn, JoinType type) {
    this.query = query;
    this.leftOn = leftOn;
    this.rightOn = rightOn;
    this.type = type;
  }

  public QueryDTO getQuery() {
    return query;
  }

  public void setQuery(QueryDTO query) {
    this.query = query;
  }

  public List<FeatureGroupFeatureDTO> getOn() {
    return on;
  }

  public void setOn(List<FeatureGroupFeatureDTO> on) {
    this.on = on;
  }

  public List<FeatureGroupFeatureDTO> getLeftOn() {
    return leftOn;
  }

  public void setLeftOn(List<FeatureGroupFeatureDTO> leftOn) {
    this.leftOn = leftOn;
  }

  public List<FeatureGroupFeatureDTO> getRightOn() {
    return rightOn;
  }

  public void setRightOn(List<FeatureGroupFeatureDTO> rightOn) {
    this.rightOn = rightOn;
  }

  public JoinType getType() {
    return type;
  }

  public void setType(JoinType type) {
    this.type = type;
  }
}
