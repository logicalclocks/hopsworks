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

package io.hops.hopsworks.api.featurestore.datavalidation.validations;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupValidationFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupValidationsController;
import io.hops.hopsworks.common.featurestore.featuregroup.ExpectationResult;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupValidationBuilder {

  @EJB
  private InodeController inodeController;
  @EJB
  private FeatureGroupValidationsController featureGroupValidationsController;
  @EJB
  private FeatureGroupValidationFacade featureGroupValidationFacade;

  public FeatureGroupValidationDTO uri(FeatureGroupValidationDTO dto, UriInfo uriInfo, Project project,
    Featuregroup featuregroup) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString())
      .path(Integer.toString(featuregroup.getFeaturestore().getId()))
      .path(ResourceRequest.Name.FEATUREGROUPS.toString())
      .path(Integer.toString(featuregroup.getId()))
      .path(ResourceRequest.Name.VALIDATIONS.toString())
      .build());
    return dto;
  }
  
  public FeatureGroupValidationDTO uri(FeatureGroupValidationDTO dto, UriInfo uriInfo, Project project,
    Featuregroup featuregroup, Integer validationId) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString())
      .path(Integer.toString(featuregroup.getFeaturestore().getId()))
      .path(ResourceRequest.Name.FEATUREGROUPS.toString())
      .path(Integer.toString(featuregroup.getId()))
      .path(ResourceRequest.Name.VALIDATIONS.toString())
      .path(validationId.toString())
      .build());
    return dto;
  }
  
  public FeatureGroupValidationDTO expand(FeatureGroupValidationDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.VALIDATIONS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public FeatureGroupValidationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, Project project,
    Featuregroup featureGroup) throws FeaturestoreException {
    // Get all validations
    FeatureGroupValidationDTO dto = new FeatureGroupValidationDTO();
    uri(dto, uriInfo, project, featureGroup);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      CollectionInfo collectionInfo = featureGroupValidationFacade.findByFeatureGroup(
        resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        resourceRequest.getFilter(),
        resourceRequest.getSort(),
        featureGroup);
      
      List<FeatureGroupValidation> featureGroupValidationsList = collectionInfo.getItems();
      
      for (FeatureGroupValidation featureGroupValidation : featureGroupValidationsList) {
        // Get validations
        List<ExpectationResult> validations = featureGroupValidationsController
          .getFeatureGroupValidationResults(user, project, featureGroup, featureGroupValidation.getValidationTime())
          .getValue1();
        dto.addItem(build(uriInfo, resourceRequest, project, featureGroup, featureGroupValidation, validations));
      }
      if (dto.getItems() == null) {
        dto.setCount(0L);
      } else {
        dto.setCount((long) dto.getItems().size());
      }
      
    }
    return dto;
  }
  
  public FeatureGroupValidationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, Project project,
                                         Featuregroup featureGroup, Integer featureGroupValidationId)
          throws FeaturestoreException {
    FeatureGroupValidation featureGroupValidation = featureGroupValidationFacade.findById(featureGroupValidationId);
    return build(uriInfo, resourceRequest, user, project, featureGroup, featureGroupValidation);
  }

  public FeatureGroupValidationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, Project project,
    Featuregroup featureGroup, FeatureGroupValidation featureGroupValidation) throws FeaturestoreException {
    Pair<FeatureGroupValidation, List<ExpectationResult>> pair =
      featureGroupValidationsController.getFeatureGroupValidationResults(user, project,
        featureGroup, featureGroupValidation.getValidationTime());
    return build(uriInfo, resourceRequest, project, featureGroup, featureGroupValidation, pair.getValue1());
  }

  public FeatureGroupValidationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                         Featuregroup featureGroup,
                                         FeatureGroupValidation featureGroupValidation,
                                         List<ExpectationResult> results) {
    FeatureGroupValidationDTO dto = new FeatureGroupValidationDTO();
    uri(dto, uriInfo, project, featureGroup, featureGroupValidation.getId());
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      if (resourceRequest.getField() != null && !resourceRequest.getField().isEmpty()) {
        for (String field : resourceRequest.getField()) {
          switch (field) {
            case "id":
              dto.setValidationId(featureGroupValidation.getId());
              break;
            case "validation_path":
              dto.setValidationPath(inodeController.getPath(featureGroupValidation.getValidationsPath()));
              break;
            case "status":
              dto.setStatus(featureGroupValidation.getStatus());
              break;
            case "validation_time":
              dto.setValidationTime(featureGroupValidation.getValidationTime().getTime());
              break;
            case "commit_time":
              // If  feature group is time-travel enabled, then get commit time
              if (featureGroup.getCachedFeaturegroup() != null
                      && featureGroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) {
                dto.setCommitTime(featureGroupValidationsController.getCommitTime(featureGroupValidation));
              }
              break;
            case "expectation_results":
              dto.setExpectationResults(results);
              break;
            default:
              break;
          }
        }
      } else {
        dto.setValidationId(featureGroupValidation.getId());
        dto.setValidationPath(inodeController.getPath(featureGroupValidation.getValidationsPath()));
        dto.setStatus(featureGroupValidation.getStatus());
        dto.setValidationTime(featureGroupValidation.getValidationTime().getTime());
        // If  feature group is time-travel enabled, then get commit time
        if (featureGroup.getCachedFeaturegroup() != null
                && featureGroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) {
          dto.setCommitTime(featureGroupValidationsController.getCommitTime(featureGroupValidation));
        }
        dto.setExpectationResults(results);
      }
    }
    return dto;
  }
  
}

