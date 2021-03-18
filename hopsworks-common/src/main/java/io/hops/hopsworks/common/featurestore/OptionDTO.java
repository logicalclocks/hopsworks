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

package io.hops.hopsworks.common.featurestore;

import javax.xml.bind.annotation.XmlRootElement;

// I'd prefer using a Map<String, String> for options the main issue is that Glassifish
// serializes it in a weird way and I haven't found an easy way of serializing it as
// Jackson does (think python dicts). Glassfish way of serializing it is
// a pain to handle in the client, It's easier to have a list of DTOs with name value fields.
// Also, this DTO is in common because it's needed in other feature store DTOs which are
// in common as well
@XmlRootElement
public class OptionDTO {
  private String name;
  private String value;

  public OptionDTO() {}

  public OptionDTO(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
