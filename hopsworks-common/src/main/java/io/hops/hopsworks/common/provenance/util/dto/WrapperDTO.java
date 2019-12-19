/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.util.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WrapperDTO<R extends Object> {
  private R result;
  
  public WrapperDTO() {}
  
  public WrapperDTO(R result) {
    this.result = result;
  }
  
  public R getResult() {
    return result;
  }
  
  public void setResult(R result) {
    this.result = result;
  }
}
