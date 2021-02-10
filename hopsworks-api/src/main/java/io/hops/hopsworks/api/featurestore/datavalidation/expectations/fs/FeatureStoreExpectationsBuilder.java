/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.datavalidation.expectations.fs;

import io.hops.hopsworks.api.featurestore.datavalidation.expectations.ExpectationDTO;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupExpectationFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureStoreExpectationFacade;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupExpectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureStoreExpectation;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureStoreExpectationsBuilder {
  
  @EJB
  private FeatureStoreExpectationFacade featureStoreExpectationFacade;
  @EJB
  private FeatureGroupExpectationFacade featureGroupExpectationFacade;

  public ExpectationDTO uri(ExpectationDTO dto, UriInfo uriInfo, Project project, Featurestore featurestore,
                            FeatureStoreExpectation featureStoreExpectation) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString())
      .path(Integer.toString(featurestore.getId()))
      .path(ResourceRequest.Name.EXPECTATIONS.toString())
      .path(featureStoreExpectation.getName())
      .build());
    return dto;
  }
  
  public ExpectationDTO uri(ExpectationDTO dto, UriInfo uriInfo, Project project, Featurestore featurestore) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString())
      .path(Integer.toString(featurestore.getId()))
      .path(ResourceRequest.Name.EXPECTATIONS.toString())
      .build());
    return dto;
  }
  
  public ExpectationDTO uri(ExpectationDTO dto, UriInfo uriInfo, Project project, Featuregroup featuregroup) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString())
      .path(Integer.toString(featuregroup.getFeaturestore().getId()))
      .path(ResourceRequest.Name.FEATUREGROUPS.toString())
      .path(Integer.toString(featuregroup.getId()))
      .path(ResourceRequest.Name.EXPECTATIONS.toString())
      .build());
    return dto;
  }

  public ExpectationDTO uri(ExpectationDTO dto, UriInfo uriInfo, Project project, Featuregroup featuregroup,
                            FeatureStoreExpectation featureStoreExpectation) {
    dto.setHref(uriInfo.getBaseUriBuilder()
            .path(ResourceRequest.Name.PROJECT.toString())
            .path(Integer.toString(project.getId()))
            .path(ResourceRequest.Name.FEATURESTORES.toString())
            .path(Integer.toString(featuregroup.getFeaturestore().getId()))
            .path(ResourceRequest.Name.FEATUREGROUPS.toString())
            .path(Integer.toString(featuregroup.getId()))
            .path(ResourceRequest.Name.EXPECTATIONS.toString())
            .path(featureStoreExpectation.getName())
            .build());
    return dto;
  }
  
  public ExpectationDTO expand(ExpectationDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.EXPECTATIONS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public ExpectationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    Featurestore featureStore, FeatureStoreExpectation featureStoreExpectation) {
    ExpectationDTO dto = new ExpectationDTO();
    uri(dto, uriInfo, project, featureStore, featureStoreExpectation);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      setDtoFields(resourceRequest, dto, featureStoreExpectation);
    }
    return dto;
  }
  
  public ExpectationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    Featuregroup featuregroup, FeatureGroupExpectation featureGroupExpectation) {
    ExpectationDTO dto = new ExpectationDTO();
    uri(dto, uriInfo, project, featuregroup, featureGroupExpectation.getFeatureStoreExpectation());
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      setDtoFields(resourceRequest, dto, featureGroupExpectation.getFeatureStoreExpectation());
    }
    return dto;
  }
  
  public ExpectationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    Featurestore featurestore) {
    ExpectationDTO dto = new ExpectationDTO();
    uri(dto, uriInfo, project, featurestore);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = featureStoreExpectationFacade.findByFeaturestore(
        resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        resourceRequest.getFilter(),
        resourceRequest.getSort(),
        featurestore);
      
      List<FeatureStoreExpectation> featureStoreExpectations = collectionInfo.getItems();
      
      for (FeatureStoreExpectation featureStoreExpectation : featureStoreExpectations) {
        dto.addItem(build(uriInfo, resourceRequest, project, featurestore, featureStoreExpectation));
      }
    }
    if (dto.getItems() == null) {
      dto.setCount(0L);
    } else {
      dto.setCount((long) dto.getItems().size());
    }
    
    return dto;
  }
  
  public ExpectationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    Featuregroup featuregroup) {
    ExpectationDTO dto = new ExpectationDTO();
    uri(dto, uriInfo, project, featuregroup);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = featureGroupExpectationFacade.findByFeaturegroup(
        resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        resourceRequest.getFilter(),
        resourceRequest.getSort(),
        featuregroup);
      
      List<FeatureGroupExpectation> featureGroupExpectations = collectionInfo.getItems();
      
      for (FeatureGroupExpectation featureGroupExpectation : featureGroupExpectations) {
        dto.addItem(build(uriInfo, resourceRequest, project, featuregroup, featureGroupExpectation));
      }
    }
    if (dto.getItems() == null) {
      dto.setCount(0L);
    } else {
      dto.setCount((long) dto.getItems().size());
    }
    
    return dto;
  }

  private void setDtoFields(ResourceRequest resourceRequest, ExpectationDTO dto,
                            FeatureStoreExpectation featureStoreExpectation) {
    if (resourceRequest.getField() != null && !resourceRequest.getField().isEmpty()) {
      for (String field : resourceRequest.getField()) {
        switch (field) {
          case "name":
            dto.setName(featureStoreExpectation.getName());
            break;
          case "description":
            dto.setDescription(featureStoreExpectation.getDescription());
            break;
          case "features":
            dto.setFeatures(featureStoreExpectation.getExpectation().getFeatures());
            break;
          case "rules":
            dto.setRules(featureStoreExpectation.getExpectation().getRules());
            break;
          default:
            break;
        }
      }
    } else {
      dto.setName(featureStoreExpectation.getName());
      dto.setDescription(featureStoreExpectation.getDescription());
      dto.setFeatures(featureStoreExpectation.getExpectation().getFeatures());
      dto.setRules(featureStoreExpectation.getExpectation().getRules());
    }
  }
}

