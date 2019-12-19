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

package io.hops.hopsworks.api.experiments.tensorboard;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoardFacade;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardDTO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TensorBoardBuilder {

  @EJB
  private TensorBoardFacade tensorBoardFacade;

  public TensorBoardDTO uri(TensorBoardDTO dto, UriInfo uriInfo, Project project, String mlId) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.EXPERIMENTS.toString().toLowerCase())
        .path(mlId)
        .path(ResourceRequest.Name.TENSORBOARD.toString().toLowerCase())
        .build());
    return dto;
  }

  public TensorBoardDTO expand(TensorBoardDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TENSORBOARD)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public TensorBoardDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, String mlId) {
    TensorBoardDTO dto = new TensorBoardDTO();
    uri(dto, uriInfo, project, mlId);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      TensorBoard tensorBoard = tensorBoardFacade.findByMlId(mlId);
      dto.setMlId(mlId);
      dto.setEndpoint(tensorBoard.getEndpoint());
      dto.setHdfsLogdir(tensorBoard.getHdfsLogdir());
      dto.setLastAccessed(tensorBoard.getLastAccessed());
    }
    return dto;
  }
}