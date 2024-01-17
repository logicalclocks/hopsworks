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

package io.hops.hopsworks.common.featurestore.statistics;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.hops.hopsworks.common.api.RestDTO;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class StatisticsDTO extends RestDTO<StatisticsDTO> {
  @JsonAlias("commitTime") // backwards compatibility
  private Long computationTime;
  private Float rowPercentage;
  private Collection<FeatureDescriptiveStatisticsDTO> featureDescriptiveStatistics;
  private String content; // backwards compatibility
  
  // feature group
  private Integer featureGroupId;
  private Long windowStartCommitTime;
  @JsonAlias("featureGroupCommitId") // backwards compatibility
  private Long windowEndCommitTime;
  
  // training dataset
  private String featureViewName;
  private Integer featureViewVersion;
  private Integer trainingDatasetVersion;
  private List<SplitStatisticsDTO> splitStatistics;
  @JsonAlias("forTransformation")  // backwards compatibility
  private Boolean beforeTransformation;
}
