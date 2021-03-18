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

import io.hops.hopsworks.api.models.dto.ModelsEndpointDTO;
import io.hops.hopsworks.api.models.dto.ModelDTO;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.state.ProvStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateParser;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ModelsException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.logging.log4j.core.util.Integers;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private AccessController accessCtrl;
  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private ModelsController modelsController;
  
  public ModelDTO uri(ModelDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.MODELS.toString().toLowerCase())
      .build());
    return dto;
  }
  
  public ModelDTO uri(ModelDTO dto, UriInfo uriInfo, Project queringProject,
    ModelsEndpointDTO sharingEndpoint, ProvStateDTO fileProvenanceHit) {
    UriBuilder uriBuilder = uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(queringProject.getId()))
      .path(ResourceRequest.Name.MODELS.toString().toLowerCase());
    if(sharingEndpoint.getParentProjectId().equals(queringProject.getId())) {
      uriBuilder = uriBuilder
        .path(fileProvenanceHit.getMlId());
    } else {
      uriBuilder = uriBuilder
        .queryParam("filter_by=" + Filters.ENDPOINT_ID + ":" + sharingEndpoint.getParentProjectId())
        .queryParam("filter_by=" + Filters.ID_EQ + ":" + dto.getId());
      
    }
    dto.setHref(uriBuilder.build());
    return dto;
  }

  public ModelDTO expand(ModelDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.MODELS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  //Build collection
  public ModelDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user)
    throws ModelsException, GenericException {
    ModelDTO dto = new ModelDTO();
    uri(dto, uriInfo, project);
    expand(dto, resourceRequest);
    dto.setCount(0l);

    validatePagination(resourceRequest);

    if(dto.isExpand()) {
      ProvStateDTO fileState;
      try {
        Pair<ProvStateParamBuilder, Map<Long, ModelsEndpointDTO>> provFilesParamBuilder
          = buildModelProvenanceParams(user, project, resourceRequest);
        if(provFilesParamBuilder.getValue1().isEmpty()) {
          //no endpoint - no results
          return dto;
        }
        fileState = provenanceController.provFileStateList(project, provFilesParamBuilder.getValue0());
        List<ProvStateDTO> models = fileState.getItems();
        dto.setCount(fileState.getCount());
        for(ProvStateDTO fileProvStateHit: models) {
          ModelDTO modelDTO
            = build(uriInfo, resourceRequest, project, provFilesParamBuilder.getValue1(), fileProvStateHit);
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
      } catch(DatasetException e) {
        throw new ModelsException(RESTCodes.ModelsErrorCode.MODEL_LIST_FAILED, Level.FINE,
          "Unable to list models for project " + project.getName(), e.getMessage(), e);
      }
    }
    return dto;
  }

  //Build specific
  public ModelDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    Map<Long, ModelsEndpointDTO> endpoints, ProvStateDTO fileProvenanceHit)
    throws ModelsException, GenericException {
    
    ModelDTO modelDTO = new ModelDTO();
    ModelsEndpointDTO endpoint = endpoints.get(fileProvenanceHit.getProjectInodeId());
    if(endpoint == null) {
      //no endpoint - no results
      return null;
    }
    uri(modelDTO, uriInfo, project, endpoint, fileProvenanceHit);
    if (expand(modelDTO, resourceRequest).isExpand()) {
      if (fileProvenanceHit.getXattrs() != null
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
        modelDTO.setExperimentProjectName(modelSummary.getExperimentProjectName());
      }
    }
    return modelDTO;
  }
  
  private ModelsEndpointDTO verifyModelsEndpoint(Project userProject, String sEndpointId)
    throws GenericException, DatasetException {
    Integer endpointId;
    try {
      endpointId = Integers.parseInt(sEndpointId);
    } catch(NumberFormatException e) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        "Provided Endpoint Id was malformed - expected a Integer ", e.getMessage(), e);
    }
    Project sharingProject = projectFacade.findById(endpointId)
      .orElseThrow(() -> new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        "Provided project cannot be accessed"));
    Dataset dataset = datasetCtrl.getByName(sharingProject, Settings.HOPS_MODELS_DATASET);
    if(dataset != null && accessCtrl.hasAccess(userProject, dataset)) {
      return ModelsEndpointDTO.fromDataset(dataset);
    }
    throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
      "Provided Endpoint cannot be accessed");
  }
  
  private Pair<ProvStateParamBuilder, Map<Long, ModelsEndpointDTO>> buildFilter(Users user, Project project,
    Set<? extends AbstractFacade.FilterBy> filters)
    throws GenericException, ProvenanceException, DatasetException {
    ProvStateParamBuilder provFilesParamBuilder = new ProvStateParamBuilder();
    Map<Long, ModelsEndpointDTO> selectedEndpoints = new HashMap<>();
    if(filters != null) {
      Users filterUser = null;
      Project filterUserProject = project;
      for (AbstractFacade.FilterBy filterBy : filters) {
        if(filterBy.getParam().compareToIgnoreCase(Filters.ENDPOINT_ID.name()) == 0) {
          ModelsEndpointDTO endpoint = verifyModelsEndpoint(project, filterBy.getValue());
          selectedEndpoints.put(endpoint.getParentProject().getInode().getId(), endpoint);
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.NAME_EQ.name()) == 0) {
          provFilesParamBuilder.filterByXAttr(MODEL_SUMMARY_XATTR_NAME + ".name", filterBy.getValue());
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.NAME_LIKE.name()) == 0) {
          provFilesParamBuilder.filterLikeXAttr(MODEL_SUMMARY_XATTR_NAME + ".name", filterBy.getValue());
        }  else if(filterBy.getParam().compareToIgnoreCase(Filters.VERSION.name()) == 0) {
          provFilesParamBuilder.filterByXAttr(MODEL_SUMMARY_XATTR_NAME + ".version", filterBy.getValue());
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.ID_EQ.name()) == 0) {
          provFilesParamBuilder.filterByXAttr(MODEL_SUMMARY_XATTR_NAME + ".id", filterBy.getValue());
        } else if (filterBy.getParam().compareToIgnoreCase(Filters.USER.name()) == 0) {
          try {
            filterUser = userFacade.find(Integer.parseInt(filterBy.getValue()));
          } catch(NumberFormatException e) {
            throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.INFO,
              "expected int user id, found: " + filterBy.getValue());
          }
        } else if (filterBy.getParam().compareToIgnoreCase(Filters.USER_PROJECT.name()) == 0) {
          try {
            filterUserProject = projectFacade.find(Integer.parseInt(filterBy.getValue()));
          } catch(NumberFormatException e) {
            throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.INFO,
              "expected int user project id, found: " + filterBy.getValue());
          }
        } else {
          throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.INFO,
            "Filter by - found: " + filterBy.getParam() + " expected:" + EnumSet.allOf(Filters.class));
        }
      }
      if(filterUser != null) {
        ProjectTeam member = projectTeamFacade.findByPrimaryKey(filterUserProject, filterUser);
        if(member == null) {
          throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.INFO,
            "Selected user: " + filterUser.getUid() + " is not part of project:" + filterUserProject.getId());
        }
        String hdfsUserStr = hdfsUsersController.getHdfsUserName(filterUserProject, filterUser);
        HdfsUsers hdfsUsers = hdfsUsersFacade.findByName(hdfsUserStr);
        provFilesParamBuilder.filterByField(ProvStateParser.FieldsP.USER_ID, hdfsUsers.getId().toString());
      }
    }
    //an endpoint always has to be selected, if none provided, then all accessible endpoints are used
    if(selectedEndpoints.isEmpty()) {
      for(ModelsEndpointDTO endpoint : modelsController.getModelsEndpoints(project)) {
        selectedEndpoints.put(endpoint.getParentProject().getInode().getId(), endpoint);
      }
    }
    for(ModelsEndpointDTO endpoint : selectedEndpoints.values()) {
      provFilesParamBuilder
        .filterByField(ProvStateParser.FieldsP.PROJECT_I_ID, endpoint.getParentProject().getInode().getId())
        .filterByField(ProvStateParser.FieldsP.DATASET_I_ID, endpoint.getDatasetInodeId());
    }
    return Pair.with(provFilesParamBuilder, selectedEndpoints);
  }
  
  private void buildSortOrder(ProvStateParamBuilder provFilesParamBuilder, Set<? extends AbstractFacade.SortBy> sort) {
    if(sort != null) {
      for(AbstractFacade.SortBy sortBy: sort) {
        if(sortBy.getValue().compareToIgnoreCase(SortBy.NAME.name()) == 0) {
          provFilesParamBuilder.sortByXAttr(MODEL_SUMMARY_XATTR_NAME + ".name",
            SortOrder.valueOf(sortBy.getParam().getValue()));
        } else {
          String sortKeyName = sortBy.getValue();
          String sortKeyOrder = sortBy.getParam().getValue();
          provFilesParamBuilder.sortByXAttr(MODEL_SUMMARY_XATTR_NAME + ".metrics." + sortKeyName,
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
    ENDPOINT_ID,
    NAME_EQ,
    NAME_LIKE,
    VERSION,
    ID_EQ,
    USER,
    USER_PROJECT
  }
  
  private Pair<ProvStateParamBuilder, Map<Long, ModelsEndpointDTO>> buildModelProvenanceParams(Users user,
    Project project, ResourceRequest resourceRequest)
    throws ProvenanceException, GenericException, DatasetException {
    Pair<ProvStateParamBuilder, Map<Long, ModelsEndpointDTO>> builder
      = buildFilter(user, project, resourceRequest.getFilter());
    builder.getValue0()
      .filterByField(ProvStateParser.FieldsP.ML_TYPE, Provenance.MLType.MODEL.name())
      .hasXAttr(MODEL_SUMMARY_XATTR_NAME)
      .paginate(resourceRequest.getOffset(), resourceRequest.getLimit());
    buildSortOrder(builder.getValue0(), resourceRequest.getSort());
    return builder;
  }
}