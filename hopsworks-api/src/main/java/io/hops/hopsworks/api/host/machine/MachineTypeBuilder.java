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

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.persistence.entity.python.MachineType;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;

@Stateless
public class MachineTypeBuilder {

  @EJB
  private HostsFacade hostsFacade;
  
  private MachineTypeDTO url(UriInfo uriInfo, MachineTypeDTO dto) {
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    return dto;
  }
  
  private MachineTypeDTO url(UriInfo uriInfo, MachineTypeDTO dto, MachineType machineType) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .path(machineType.name())
      .build());
    return dto;
  }
  
  private MachineTypeDTO expand(MachineTypeDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.MACHINETYPES)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  private MachineTypeDTO build(UriInfo uriInfo, ResourceRequest resourceProperties,
    MachineType machineType, Integer count) {
    MachineTypeDTO dto = new MachineTypeDTO();
    url(uriInfo, dto, machineType);
    expand(dto, resourceProperties);
    dto.setMachineType(machineType);
    if (dto.isExpand()) {
      dto.setNumMachines(count);
    }
    return dto;
  }
  
  private MachineTypeDTO build(MachineTypeDTO dto, UriInfo uriInfo, ResourceRequest resourceProperties,
    MachineType machineType, Integer count) {
    url(uriInfo, dto);
    expand(dto, resourceProperties);
    dto.setMachineType(machineType);
    if (dto.isExpand()) {
      dto.setNumMachines(count);
    }
    return dto;
  }
  
  public MachineTypeDTO buildItem(UriInfo uriInfo, ResourceRequest resourceProperties,
    MachineType machineType) {
    MachineTypeDTO dto = new MachineTypeDTO();
    return build(dto, uriInfo, resourceProperties, machineType, hostsFacade.getCondaHosts(machineType).size());
  }

  public MachineTypeDTO buildItems(UriInfo uriInfo, ResourceRequest resourceProperties) {
    MachineTypeDTO machineTypeDTO = new MachineTypeDTO();
    url(uriInfo, machineTypeDTO);
    expand(machineTypeDTO, resourceProperties);
    if (machineTypeDTO.isExpand()) {
      return buildItems(machineTypeDTO, uriInfo, resourceProperties, getMachineTypes());
    }
    return machineTypeDTO;
  }
  
  private MachineTypeDTO buildItems(MachineTypeDTO dto, UriInfo uriInfo, ResourceRequest resourceProperties,
    HashMap<MachineType, Integer> machineTypes) {
    if (machineTypes != null && !machineTypes.isEmpty()) {
      machineTypes.forEach((k, v) -> dto.addItem(build(uriInfo, resourceProperties, k, v)));
    }
    return dto;
  }
  
  public HashMap<MachineType, Integer> getMachineTypes() {
    HashMap<MachineType, Integer> machineTypesHashMap = new HashMap<>();
    MachineType [] machineTypes = MachineType.values();
    for(MachineType machineType: machineTypes) {
      machineTypesHashMap.put(machineType, hostsFacade.getCondaHosts(machineType).size());
    }
    return machineTypesHashMap;
  }
  
}
