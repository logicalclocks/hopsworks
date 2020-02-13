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
package io.hops.hopsworks.api.models;

import io.hops.hopsworks.api.models.dto.ModelDTO;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.state.ProvFileStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateElastic;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateListDTO;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ModelsException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelsBuilder {

  public final static String MODEL_SUMMARY_XATTR_NAME = "model_summary";

  private static final Logger LOGGER = Logger.getLogger(ModelsBuilder.class.getName());
  @EJB
  private ProvStateController provenanceController;
  @EJB
  private ModelConverter modelConverter;
  @EJB
  private Settings settings;

  public ModelDTO uri(ModelDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.MODELS.toString().toLowerCase())
        .build());
    return dto;
  }

  public ModelDTO uri(ModelDTO dto, UriInfo uriInfo, Project project, ProvStateElastic fileProvenanceHit) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.MODELS.toString().toLowerCase())
        .path(fileProvenanceHit.getMlId())
        .build());
    return dto;
  }

  public ModelDTO expand(ModelDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.MODELS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  //Build collection
  public ModelDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project)
      throws ModelsException {
    ModelDTO dto = new ModelDTO();
    uri(dto, uriInfo, project);
    expand(dto, resourceRequest);
    dto.setCount(0l);

    validatePagination(resourceRequest);

    if(dto.isExpand()) {
      ProvStateListDTO fileState = null;
      try {
        ProvFileStateParamBuilder provFilesParamBuilder = buildModelProvenanceParams(project, resourceRequest);
        fileState = provenanceController.provFileStateList(project, provFilesParamBuilder);
        List<ProvStateElastic> models = fileState.getItems();
        dto.setCount(fileState.getCount());
        for(ProvStateElastic fileProvStateHit: models) {
          ModelDTO modelDTO = build(uriInfo, resourceRequest, project, fileProvStateHit);
          if(modelDTO != null) {
            dto.addItem(modelDTO);
          }
        }
      } catch (ProvenanceException e) {
        if (ProvHelper.missingMappingForField( e)) {
          LOGGER.log(Level.WARNING, "Could not find elastic mapping for experiments query", e);
          return dto;
        } else {
          throw new ModelsException(RESTCodes.ModelsErrorCode.MODEL_LIST_FAILED, Level.FINE,
              "Unable to list models for project " + project.getName(), e.getMessage(), e);
        }
      }
    }
    return dto;
  }

  //Build specific
  public ModelDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                        ProvStateElastic fileProvenanceHit) throws ModelsException {

    ModelDTO modelDTO = new ModelDTO();
    uri(modelDTO, uriInfo, project, fileProvenanceHit);
    expand(modelDTO, resourceRequest);

    if (modelDTO.isExpand()) {
      if(fileProvenanceHit.getXattrs() != null
          && fileProvenanceHit.getXattrs().containsKey(MODEL_SUMMARY_XATTR_NAME)) {
        JSONObject summary = new JSONObject(fileProvenanceHit.getXattrs().get(MODEL_SUMMARY_XATTR_NAME));
        ModelDTO modelSummary = modelConverter.unmarshalDescription(summary.toString());
        modelDTO.setId(fileProvenanceHit.getMlId());
        modelDTO.setName(modelSummary.getName());
        modelDTO.setVersion(modelSummary.getVersion());
        modelDTO.setUserFullName(modelSummary.getUserFullName());
        modelDTO.setCreated(fileProvenanceHit.getCreateTime());
        modelDTO.setMetrics(modelSummary.getMetrics());
        modelDTO.setDescription(modelSummary.getDescription());
        modelDTO.setProgram(modelSummary.getProgram());
        modelDTO.setEnvironment(modelSummary.getEnvironment());
        modelDTO.setExperimentId(modelSummary.getExperimentId());
      } else {
        return null;
      }
    }
    return modelDTO;
  }

  private void buildFilter(ProvFileStateParamBuilder provFilesParamBuilder,
                                            Set<? extends AbstractFacade.FilterBy> filters) {
    if(filters != null) {
      for (AbstractFacade.FilterBy filterBy : filters) {
        if(filterBy.getParam().compareToIgnoreCase(Filters.NAME_EQ.name()) == 0) {
          HashMap<String, String> map = new HashMap<>();
          map.put(MODEL_SUMMARY_XATTR_NAME + ".name", filterBy.getValue());
          provFilesParamBuilder.withXAttrs(map);
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.NAME_LIKE.name()) == 0) {
          HashMap<String, String> map = new HashMap<>();
          map.put(MODEL_SUMMARY_XATTR_NAME + ".name", filterBy.getValue());
          provFilesParamBuilder.withXAttrsLike(map);
        }  else if(filterBy.getParam().compareToIgnoreCase(Filters.VERSION.name()) == 0) {
          HashMap<String, String> map = new HashMap<>();
          map.put(MODEL_SUMMARY_XATTR_NAME + ".version", filterBy.getValue());
          provFilesParamBuilder.withXAttrs(map);
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.ID_EQ.name()) == 0) {
          HashMap<String, String> map = new HashMap<>();
          map.put(MODEL_SUMMARY_XATTR_NAME + ".id", filterBy.getValue());
          provFilesParamBuilder.withXAttrs(map);
        } else {
          throw new WebApplicationException("Filter by need to set a valid filter parameter, but found: " +
              filterBy.getParam(), Response.Status.NOT_FOUND);
        }
      }
    }
  }

  private void buildSortOrder(ProvFileStateParamBuilder provFilesParamBuilder, Set<?
      extends AbstractFacade.SortBy> sort) {
    if(sort != null) {
      for(AbstractFacade.SortBy sortBy: sort) {
        if(sortBy.getValue().compareToIgnoreCase(SortBy.NAME.name()) == 0) {
          provFilesParamBuilder.sortBy(MODEL_SUMMARY_XATTR_NAME + ".name",
              SortOrder.valueOf(sortBy.getParam().getValue()));
        } else {
          String sortKeyName = sortBy.getValue();
          String sortKeyOrder = sortBy.getParam().getValue();
          provFilesParamBuilder.sortBy(MODEL_SUMMARY_XATTR_NAME + ".metrics." + sortKeyName,
              SortOrder.valueOf(sortKeyOrder));
        }
      }
    }
  }

  private void validatePagination(ResourceRequest resourceRequest) {
    if(resourceRequest.getLimit() == null || resourceRequest.getLimit() <= 0) {
      resourceRequest.setLimit(settings.getElasticDefaultScrollPageSize());
    }

    if(resourceRequest.getOffset() == null || resourceRequest.getOffset() <= 0) {
      resourceRequest.setOffset(0);
    }
  }

  protected enum SortBy {
    NAME
  }

  protected enum Filters {
    NAME_EQ,
    NAME_LIKE,
    VERSION,
    ID_EQ
  }

  private ProvFileStateParamBuilder buildModelProvenanceParams(Project project, ResourceRequest resourceRequest)
      throws ProvenanceException {

    ProvFileStateParamBuilder builder = new ProvFileStateParamBuilder()
        .withProjectInodeId(project.getInode().getId())
        .withMlType(Provenance.MLType.MODEL.name())
        .withPagination(resourceRequest.getOffset(), resourceRequest.getLimit())
        .filterByHasXAttr(MODEL_SUMMARY_XATTR_NAME);

    buildSortOrder(builder, resourceRequest.getSort());
    buildFilter(builder, resourceRequest.getFilter());
    return builder;
  }
}