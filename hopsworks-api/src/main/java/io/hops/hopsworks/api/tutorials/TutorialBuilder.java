/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.tutorials;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.tutorials.TutorialFacade;
import io.hops.hopsworks.persistence.entity.tutorials.Tutorial;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TutorialBuilder {

  @EJB
  TutorialFacade tutorialFacade;

  public TutorialDTO uri(TutorialDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.TUTORIALS.toString()).build());
    return dto;
  }

  public TutorialDTO build(UriInfo uriInfo, Tutorial tutorial) {
    TutorialDTO tutorialDTO = new TutorialDTO(tutorial);
    uri(tutorialDTO, uriInfo);
    
    return tutorialDTO;
  }

  public TutorialDTO build(UriInfo uriInfo) {
    TutorialDTO dtos = new TutorialDTO();
    uri(dtos, uriInfo);

    AbstractFacade.CollectionInfo<Tutorial> tutorials = tutorialFacade.findAllTutorials();
    dtos.setItems(tutorials.getItems().stream()
      .map(tutorial -> build(uriInfo, tutorial))
      .collect(Collectors.toList()));
    dtos.setCount(tutorials.getCount());

    return dtos;
  }
}
