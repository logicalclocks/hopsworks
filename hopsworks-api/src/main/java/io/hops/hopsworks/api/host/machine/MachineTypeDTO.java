/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.host.machine;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.python.MachineType;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MachineTypeDTO extends RestDTO<MachineTypeDTO> {

  private MachineType machineType;
  private Integer numMachines;
  
  public MachineType getMachineType() {
    return machineType;
  }

  public void setMachineType(MachineType machineType) {
    this.machineType = machineType;
  }
  
  public Integer getNumMachines() {
    return numMachines;
  }
  
  public void setNumMachines(Integer numMachines) {
    this.numMachines = numMachines;
  }
  
  public enum Operation {
    types
  }
}
