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
package io.hops.hopsworks.api.metadata;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class XAttrsBuilder {
  
  public XAttrDTO uri(XAttrDTO dto, UriInfo uriInfo, Project project,
      String path, String name) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(
        ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.XATTRS.toString().toLowerCase())
        .path(path)
        .queryParam("name", name)
        .build());
    return dto;
  }
  
  public XAttrDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
      Project project, String path, String name) {
    XAttrDTO dto = new XAttrDTO();
    uri(dto, uriInfo, project, path, name);
    dto.setName(name);
    return dto;
  }
  
  public XAttrDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
      Project project, String path, String name, String value) {
    XAttrDTO dto = new XAttrDTO();
    uri(dto, uriInfo, project, path, name);
    dto.setName(name);
    dto.setValue(value);
    return dto;
  }
  
  public XAttrDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
      Project project, String path, Map<String, String> xattrs) {
    XAttrDTO dto = new XAttrDTO();
    xattrs.forEach((k, v) -> dto.addItem(build(uriInfo, resourceRequest,
        project, path, k, v)));
    return dto;
  }
  
  public XAttrDTO uri(XAttrDTO dto, UriInfo uriInfo, Project project,
      Integer featurestoreId, Integer featuregroupId, String name) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(
        ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestoreId))
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(featuregroupId))
        .path(ResourceRequest.Name.XATTRS.toString().toLowerCase())
        .path(name)
        .build());
    return dto;
  }
  
  public XAttrDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
      Project project, Integer featurestoreId,
      Integer featuregroupId, String name) {
    XAttrDTO dto = new XAttrDTO();
    uri(dto, uriInfo, project, featurestoreId, featuregroupId, name);
    dto.setName(name);
    return dto;
  }
  
  public XAttrDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
      Project project, Integer featurestoreId, Integer featuregroupId,
      Map<String, String> xattrs) {
    XAttrDTO dto = new XAttrDTO();
    xattrs.forEach((k, v) -> dto.addItem(build(uriInfo, resourceRequest,
        project, featurestoreId, featuregroupId, k, v)));
    return dto;
  }
  
  public XAttrDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
      Project project, Integer featurestoreId, Integer featuregroupId, String name, String value) {
    XAttrDTO dto = new XAttrDTO();
    uri(dto, uriInfo, project, featurestoreId, featuregroupId, name);
    dto.setName(name);
    dto.setValue(value);
    return dto;
  }
}
