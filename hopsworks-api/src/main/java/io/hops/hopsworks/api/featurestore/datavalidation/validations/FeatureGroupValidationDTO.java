/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.datavalidation.validations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ExpectationResult;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class FeatureGroupValidationDTO extends RestDTO<FeatureGroupValidationDTO> {

  @XmlElement(required = true)
  @Getter @Setter
  private Integer validationId;
  @XmlElement(required = true)
  @Getter @Setter
  private Long validationTime;
  @XmlElement(required = true)
  @Getter @Setter
  private Long commitTime;
  @Getter @Setter
  private List<ExpectationResult> expectationResults;
  @XmlElement(required = true)
  @Getter @Setter
  private String validationPath;
  @XmlElement(required = true)
  @Getter @Setter
  private FeatureGroupValidation.Status status;
  
}
