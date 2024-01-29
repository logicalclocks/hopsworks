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

import io.hops.hopsworks.common.project.AccessCredentialsDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
public class ArrowFlightCredentialDTO {

  @Getter
  @Setter
  @JsonProperty(value = "kstore")
  private String kstore;
  @Getter
  @Setter
  @JsonProperty(value = "tstore")
  private String tstore;
  @Getter
  @Setter
  @JsonProperty(value = "cert_key")
  private String certKey;

  public ArrowFlightCredentialDTO(AccessCredentialsDTO accessCredentialsDTO) {
    this.kstore = accessCredentialsDTO.getkStore();
    this.tstore = accessCredentialsDTO.gettStore();
    this.certKey = accessCredentialsDTO.getPassword();
  }
    
}
