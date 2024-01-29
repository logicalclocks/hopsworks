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

package io.hops.hopsworks.common.arrowflight;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
public class ArrowFlightQueryDTO {

  @Getter
  @Setter
  @JsonProperty(value = "query_string")
  private String queryString;
  @Getter
  @Setter
  @JsonProperty(value = "features")
  private Map<String, List<String>> features;
  @Getter
  @Setter
  @JsonProperty(value = "filters")
  private String filters;
  @Getter
  @Setter
  @JsonProperty(value = "connectors")
  private Map<String, ArrowFlightConnectorDTO> connectors;
    
}
