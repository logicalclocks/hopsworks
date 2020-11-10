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
package io.hops.hopsworks.api.experiments;

import io.hops.hopsworks.api.experiments.dto.ExperimentDTO;
import io.hops.hopsworks.api.experiments.dto.ExperimentsEndpointDTO;
import io.hops.hopsworks.api.experiments.results.ExperimentResultsBuilder;
import io.hops.hopsworks.api.experiments.tensorboard.TensorBoardBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.state.ProvStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateParser;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ExperimentsException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.InvalidQueryException;
import io.hops.hopsworks.exceptions.MetadataException;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExperimentsBuilder {

  public static final String EXPERIMENT_SUMMARY_XATTR_NAME = "experiment_summary";
  public static final String EXPERIMENT_MODEL_XATTR_NAME = "experiment_model";
  public static final String EXPERIMENT_APP_ID_XATTR_NAME = "app_id";

  private static final Logger LOGGER = Logger.getLogger(ExperimentsBuilder.class.getName());

  @EJB
  private ProvStateController provenanceController;
  @EJB
  private TensorBoardBuilder tensorBoardBuilder;
  @EJB
  private ExperimentResultsBuilder experimentResultsBuilder;
  @EJB
  private ExperimentConverter experimentConverter;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private ExperimentsController experimentsController;
  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private AccessController accessCtrl;
  @EJB
  private DatasetController datasetCtrl;

  public ExperimentDTO uri(ExperimentDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.EXPERIMENTS.toString().toLowerCase())
        .build());
    return dto;
  }
  
  public ExperimentDTO uri(ExperimentDTO dto, UriInfo uriInfo, Project queringProject,
    ExperimentsEndpointDTO sharingEndpoint, ProvStateDTO fileProvenanceHit) {
    UriBuilder uriBuilder = uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(queringProject.getId()))
      .path(ResourceRequest.Name.EXPERIMENTS.toString().toLowerCase());
    
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

  public ExperimentDTO expand(ExperimentDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.EXPERIMENTS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  //Build collection
  public ExperimentDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user)
      throws ExperimentsException {
    ExperimentDTO dto = new ExperimentDTO();
    uri(dto, uriInfo, project);
    expand(dto, resourceRequest);
    dto.setCount(0l);

    validatePagination(resourceRequest);

    if(dto.isExpand()) {
      try {
        Pair<ProvStateParamBuilder, Map<Long, ExperimentsEndpointDTO>> provFilesParamBuilder
          = buildExperimentProvenanceParams(project, resourceRequest);
        if(provFilesParamBuilder.getValue1().isEmpty()) {
          //no endpoint - no results
          return dto;
        }
        ProvStateDTO fileState
          = provenanceController.provFileStateList(project, provFilesParamBuilder.getValue0());
        if (fileState != null) {
          List<ProvStateDTO> experiments = fileState.getItems();
          dto.setCount(fileState.getCount());
          if (experiments != null && !experiments.isEmpty()) {
            for (ProvStateDTO fileProvStateHit : experiments) {
              ExperimentDTO experimentDTO = build(uriInfo, resourceRequest, project, user,
                provFilesParamBuilder.getValue1(), fileProvStateHit);
              if (experimentDTO != null) {
                dto.addItem(experimentDTO);
              }
            }
          }
        }
      } catch (ExperimentsException | DatasetException | ProvenanceException | MetadataException | GenericException e) {
        if (e instanceof ProvenanceException && ProvHelper.missingMappingForField((ProvenanceException) e)) {
          LOGGER.log(Level.WARNING, "Could not find elastic mapping for experiments query", e);
          return dto;
        } else {
          throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.EXPERIMENT_LIST_FAILED, Level.FINE,
              "Unable to list experiments for project " + project.getName(), e.getMessage(), e);
        }
      }
    }

    return dto;
  }
  
  private Pair<ProvStateParamBuilder, Map<Long, ExperimentsEndpointDTO>> buildExperimentProvenanceParams(
    Project project, ResourceRequest resourceRequest)
    throws ProvenanceException, GenericException, DatasetException {
    Pair<ProvStateParamBuilder, Map<Long, ExperimentsEndpointDTO>> builder
      = buildFilter(project, resourceRequest.getFilter());
    builder.getValue0()
      .filterByField(ProvStateParser.FieldsP.ML_TYPE, Provenance.MLType.EXPERIMENT.name())
      .hasXAttr(EXPERIMENT_SUMMARY_XATTR_NAME)
      .withAppExpansion()
      .paginate(resourceRequest.getOffset(), resourceRequest.getLimit());
  
    buildSortOrder(builder.getValue0(), resourceRequest.getSort());
  
    return builder;
  }

  //Build specific
  public ExperimentDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user,
    Map<Long, ExperimentsEndpointDTO> endpoints, ProvStateDTO fileProvenanceHit)
    throws ExperimentsException, DatasetException, ProvenanceException, MetadataException, GenericException {

    ExperimentDTO experimentDTO = new ExperimentDTO();
    ExperimentsEndpointDTO endpoint = endpoints.get(fileProvenanceHit.getProjectInodeId());
    if(endpoint == null) {
      //no endpoint - no results
      return null;
    }
    uri(experimentDTO, uriInfo, project, endpoint ,fileProvenanceHit);
  
    if (expand(experimentDTO, resourceRequest).isExpand()) {
      if(fileProvenanceHit.getXattrs() != null
          && fileProvenanceHit.getXattrs().containsKey(EXPERIMENT_SUMMARY_XATTR_NAME)) {
        JSONObject summary = new JSONObject(fileProvenanceHit.getXattrs().get(EXPERIMENT_SUMMARY_XATTR_NAME));

        ExperimentDTO experimentSummary =
            experimentConverter.unmarshalDescription(summary.toString());

        experimentDTO.setStarted(fileProvenanceHit.getCreateTime());

        boolean updateNeeded = false;

        // update experiment that failed to set experiment status
        if((experimentSummary.getState() == null || runningState(experimentSummary))
          && Provenance.AppState.valueOf(fileProvenanceHit.getAppState().getCurrentState().name()).isFinalState()) {
          updateNeeded = true;
          experimentSummary.setState(fileProvenanceHit.getAppState().getCurrentState().name());
          experimentSummary.setFinished(fileProvenanceHit.getAppState().getFinishTime());
        }
        
        experimentDTO.setState(experimentSummary.getState());
        experimentDTO.setFinished(experimentSummary.getFinished());

        if(updateNeeded) {
          experimentsController.attachExperiment(user, project, fileProvenanceHit.getMlId(), experimentSummary);
        }

        if(fileProvenanceHit.getXattrs().containsKey(EXPERIMENT_MODEL_XATTR_NAME)) {
          ModelXAttr model = experimentConverter.unmarshal(
            fileProvenanceHit.getXattrs().get(EXPERIMENT_MODEL_XATTR_NAME), ModelXAttr.class);
          experimentDTO.setModel(model.getId());
          experimentDTO.setModelProjectName(model.getProjectName());
        }

        experimentDTO.setId(experimentSummary.getId());
        experimentDTO.setName(experimentSummary.getName());
        experimentDTO.setUserFullName(experimentSummary.getUserFullName());
        experimentDTO.setMetric(experimentSummary.getMetric());
        experimentDTO.setDescription(experimentSummary.getDescription());
        experimentDTO.setExperimentType(experimentSummary.getExperimentType());
        experimentDTO.setFunction(experimentSummary.getFunction());
        experimentDTO.setDirection(experimentSummary.getDirection());
        experimentDTO.setOptimizationKey(experimentSummary.getOptimizationKey());
        experimentDTO.setJobName(experimentSummary.getJobName());
        experimentDTO.setAppId(experimentSummary.getAppId());
        experimentDTO.setBestDir(experimentSummary.getBestDir());
        experimentDTO.setEnvironment(experimentSummary.getEnvironment());
        experimentDTO.setProgram(experimentSummary.getProgram());
        experimentDTO.setTensorboard(tensorBoardBuilder.build(uriInfo,
            resourceRequest.get(ResourceRequest.Name.TENSORBOARD), project, fileProvenanceHit.getMlId()));
        experimentDTO.setResults(experimentResultsBuilder.build(uriInfo,
            resourceRequest.get(ResourceRequest.Name.RESULTS), project, fileProvenanceHit.getMlId()));
      } else {
        return null;
      }
    }
    return experimentDTO;
  }
  
  private boolean runningState(ExperimentDTO experimentSummary) {
    return experimentSummary.getState().equals(Provenance.AppState.SUBMITTED.name())
      || experimentSummary.getState().equals(Provenance.AppState.RUNNING.name())
      || experimentSummary.getState().equals(Provenance.AppState.UNKNOWN.name());
  }
  
  private ExperimentsEndpointDTO verifyExperimentsEndpoint(Project userProject, String sEndpointId)
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
    Dataset dataset = datasetCtrl.getByName(sharingProject, Settings.HOPS_EXPERIMENTS_DATASET);
    if(dataset != null && accessCtrl.hasAccess(userProject, dataset)) {
      return ExperimentsEndpointDTO.fromDataset(dataset);
    }
    throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
      "Provided Endpoint cannot be accessed");
  }
  
  private Pair<ProvStateParamBuilder, Map<Long, ExperimentsEndpointDTO>> buildFilter(Project project,
    Set<? extends AbstractFacade.FilterBy> filters)
    throws ProvenanceException, GenericException, DatasetException {
    ProvStateParamBuilder provFilesParamBuilder = new ProvStateParamBuilder();
    Map<Long, ExperimentsEndpointDTO> selectedEndpoints = new HashMap<>();
    if(filters != null) {
      Users filterUser = null;
      Project filterUserProject = project;
      for (AbstractFacade.FilterBy filterBy : filters) {
        if (filterBy.getParam().compareToIgnoreCase(Filters.ENDPOINT_ID.name()) == 0) {
          ExperimentsEndpointDTO endpoint = verifyExperimentsEndpoint(project, filterBy.getValue());
          selectedEndpoints.put(endpoint.getParentProject().getInode().getId(), endpoint);
        } else if (filterBy.getParam().compareToIgnoreCase(Filters.NAME_LIKE.name()) == 0) {
          provFilesParamBuilder.filterLikeXAttr(EXPERIMENT_SUMMARY_XATTR_NAME + ".name", filterBy.getValue());
        } else if (filterBy.getParam().compareToIgnoreCase(Filters.NAME_EQ.name()) == 0) {
          provFilesParamBuilder.filterByXAttr(EXPERIMENT_SUMMARY_XATTR_NAME + ".name", filterBy.getValue());
        } else if (filterBy.getParam().compareToIgnoreCase(Filters.DATE_START_LT.name()) == 0) {
          Long timestamp = getDate(filterBy.getField(), filterBy.getValue()).getTime();
          provFilesParamBuilder.filterByField(ProvStateParser.FieldsPF.CREATE_TIMESTAMP_LT, timestamp);
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.DATE_START_GT.name()) == 0) {
          Long timestamp = getDate(filterBy.getField(), filterBy.getValue()).getTime();
          provFilesParamBuilder.filterByField(ProvStateParser.FieldsPF.CREATE_TIMESTAMP_GT, timestamp);
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
        } else if (filterBy.getParam().compareToIgnoreCase(Filters.STATE.name()) == 0) {
          provFilesParamBuilder.filterLikeXAttr(EXPERIMENT_SUMMARY_XATTR_NAME + ".state", filterBy.getValue());
        } else if (filterBy.getParam().compareToIgnoreCase(Filters.ID_EQ.name()) == 0) {
          provFilesParamBuilder.filterByXAttr(EXPERIMENT_SUMMARY_XATTR_NAME + ".id", filterBy.getValue());
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
      for(ExperimentsEndpointDTO endpoint : experimentsController.getExperimentsEndpoints(project)) {
        selectedEndpoints.put(endpoint.getParentProject().getInode().getId(), endpoint);
      }
    }
    for(ExperimentsEndpointDTO endpoint : selectedEndpoints.values()) {
      provFilesParamBuilder
        .filterByField(ProvStateParser.FieldsP.PROJECT_I_ID, endpoint.getParentProject().getInode().getId())
        .filterByField(ProvStateParser.FieldsP.DATASET_I_ID, endpoint.getDatasetInodeId());
    }
    return Pair.with(provFilesParamBuilder, selectedEndpoints);
  }

  private Date getDate(String field, String value) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    try {
      return formatter.parse(value);
    } catch (ParseException e) {
      throw new InvalidQueryException(
          "Filter value for " + field + " needs to set valid format. Expected:yyyy-mm-dd hh:mm:ss but found: " + value);
    }
  }

  public void buildSortOrder(ProvStateParamBuilder provFilesParamBuilder, Set<? extends AbstractFacade.SortBy> sort)
    throws GenericException {
    if(sort != null) {
      for(AbstractFacade.SortBy sortBy: sort) {
        if(sortBy.getValue().compareToIgnoreCase(SortBy.NAME.name()) == 0) {
          provFilesParamBuilder.sortByXAttr(EXPERIMENT_SUMMARY_XATTR_NAME + ".name",
              SortOrder.valueOf(sortBy.getParam().getValue()));
        } else if(sortBy.getValue().compareToIgnoreCase(SortBy.METRIC.name()) == 0) {
          provFilesParamBuilder.sortByXAttr(EXPERIMENT_SUMMARY_XATTR_NAME + ".metric",
              SortOrder.valueOf(sortBy.getParam().getValue()));
        } else if(sortBy.getValue().compareToIgnoreCase(SortBy.USER.name()) == 0) {
          provFilesParamBuilder.sortByXAttr(EXPERIMENT_SUMMARY_XATTR_NAME + ".userFullName",
              SortOrder.valueOf(sortBy.getParam().getValue()));
        } else if(sortBy.getValue().compareToIgnoreCase(SortBy.START.name()) == 0) {
          provFilesParamBuilder.sortByField(ProvStateParser.FieldsP.CREATE_TIMESTAMP,
              SortOrder.valueOf(sortBy.getParam().getValue()));
        }  else if(sortBy.getValue().compareToIgnoreCase(SortBy.END.name()) == 0) {
          provFilesParamBuilder.sortByXAttr(EXPERIMENT_SUMMARY_XATTR_NAME + ".finished",
              SortOrder.valueOf(sortBy.getParam().getValue()));
        }  else if(sortBy.getValue().compareToIgnoreCase(SortBy.STATE.name()) == 0) {
          provFilesParamBuilder.sortByXAttr(EXPERIMENT_SUMMARY_XATTR_NAME + ".state",
              SortOrder.valueOf(sortBy.getParam().getValue()));
        } else {
          throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.INFO,
            "Sort by - found: " + sortBy.getParam() + " expected:" + EnumSet.allOf(SortBy.class));
        }
      }
    }
  }

  protected enum SortBy {
    NAME,
    METRIC,
    USER,
    START,
    END,
    STATE;
  }

  protected enum Filters {
    ENDPOINT_ID,
    NAME_EQ,
    NAME_LIKE,
    DATE_START_LT,
    DATE_START_GT,
    USER,
    USER_PROJECT,
    STATE,
    ID_EQ
  }

  private void validatePagination(ResourceRequest resourceRequest) {
    if(resourceRequest.getLimit() == null || resourceRequest.getLimit() <= 0) {
      resourceRequest.setLimit(settings.getElasticDefaultScrollPageSize());
    }

    if(resourceRequest.getOffset() == null || resourceRequest.getOffset() <= 0) {
      resourceRequest.setOffset(0);
    }
  }
}