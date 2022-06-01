/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.featurestore.datavalidationv2.greatexpectations;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.GreatExpectation;

public class GreatExpectationDTO extends RestDTO<GreatExpectationDTO> {
  private String expectationType;
  private String kwargsTemplate;
  private Integer id;

  public GreatExpectationDTO() {}

  public GreatExpectationDTO(GreatExpectation greatExpectation) {
    this.id = greatExpectation.getId();
    this.expectationType = greatExpectation.getExpectationType();
    this.kwargsTemplate = greatExpectation.getKwargsTemplate();
  }

  public Integer getId() { return id; }

  public void setId(Integer id) { this.id = id; }

  public String getExpectationType() { return expectationType; }

  public void setExpectationType(String expectationType) { this.expectationType = expectationType; }

  public String getKwargsTemplate() { return kwargsTemplate; }

  public void setKwargsTemplate(String kwargsTemplate) { this.kwargsTemplate = kwargsTemplate; }

}
