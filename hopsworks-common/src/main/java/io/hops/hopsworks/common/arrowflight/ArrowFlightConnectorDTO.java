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
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
public class ArrowFlightConnectorDTO {

  @Getter
  @Setter
  @JsonProperty(value = "type")
  private String type;
  @Getter
  @Setter
  @JsonProperty(value = "options")
  private Map<String, String> options;
  @Getter
  @Setter
  @JsonProperty(value = "query")
  private String query;
  @Getter
  @Setter
  @JsonProperty(value = "alias")
  private String alias;
  @Getter
  @Setter
  @JsonProperty(value = "filters")
  private String filters;

}
