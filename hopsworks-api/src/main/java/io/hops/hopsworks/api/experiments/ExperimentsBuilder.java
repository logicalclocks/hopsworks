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
import io.hops.hopsworks.api.experiments.results.ExperimentResultsBuilder;
import io.hops.hopsworks.api.experiments.tensorboard.TensorBoardBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.state.ProvStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateParser;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ExperimentsException;
import io.hops.hopsworks.exceptions.InvalidQueryException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

  public ExperimentDTO uri(ExperimentDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.EXPERIMENTS.toString().toLowerCase())
        .build());
    return dto;
  }

  public ExperimentDTO uri(ExperimentDTO dto, UriInfo uriInfo, Project project,
                           ProvStateDTO fileProvenanceHit) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.EXPERIMENTS.toString().toLowerCase())
        .path(fileProvenanceHit.getMlId())
        .build());
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
        ProvStateParamBuilder provFilesParamBuilder = buildExperimentProvenanceParams(project, resourceRequest);
        ProvStateDTO fileState = provenanceController.provFileStateList(project, provFilesParamBuilder);
        if (fileState != null) {
          List<ProvStateDTO> experiments = fileState.getItems();
          dto.setCount(fileState.getCount());
          if (experiments != null && !experiments.isEmpty()) {
            for (ProvStateDTO fileProvStateHit : experiments) {
              ExperimentDTO experimentDTO = build(uriInfo, resourceRequest, project, user, fileProvStateHit);
              if (experimentDTO != null) {
                dto.addItem(experimentDTO);
              }
            }
          }
        }
      } catch (ExperimentsException | DatasetException | ProvenanceException | MetadataException e) {
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

  private ProvStateParamBuilder buildExperimentProvenanceParams(Project project, ResourceRequest resourceRequest)
      throws ProvenanceException {

    ProvStateParamBuilder provFilesParamBuilder = new ProvStateParamBuilder()
        .filterByField(ProvStateParser.FieldsP.PROJECT_I_ID, project.getInode().getId())
        .filterByField(ProvStateParser.FieldsP.ML_TYPE, Provenance.MLType.EXPERIMENT.name())
        .hasXAttr(EXPERIMENT_SUMMARY_XATTR_NAME)
        .withAppExpansion()
        .paginate(resourceRequest.getOffset(), resourceRequest.getLimit());

    buildSortOrder(provFilesParamBuilder, resourceRequest.getSort());
    buildFilter(project, provFilesParamBuilder, resourceRequest.getFilter());
    return provFilesParamBuilder;
  }

  //Build specific
  public ExperimentDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user,
                             ProvStateDTO fileProvenanceHit) throws ExperimentsException, DatasetException,
    ProvenanceException, MetadataException {

    ExperimentDTO experimentDTO = new ExperimentDTO();
    uri(experimentDTO, uriInfo, project, fileProvenanceHit);
    expand(experimentDTO, resourceRequest);

    if (experimentDTO.isExpand()) {
      if(fileProvenanceHit.getXattrs() != null
          && fileProvenanceHit.getXattrs().containsKey(EXPERIMENT_SUMMARY_XATTR_NAME)) {
        JSONObject summary = new JSONObject(fileProvenanceHit.getXattrs().get(EXPERIMENT_SUMMARY_XATTR_NAME));

        ExperimentDTO experimentSummary =
            experimentConverter.unmarshalDescription(summary.toString());

        experimentDTO.setStarted(fileProvenanceHit.getCreateTime());

        boolean updateNeeded = false;

        // update experiment that failed to set experiment status
        if(experimentSummary.getState().equals(Provenance.AppState.RUNNING.name()) &&
            Provenance.AppState.valueOf(fileProvenanceHit.getAppState().getCurrentState().name()).isFinalState()) {
          updateNeeded = true;
          experimentSummary.setState(fileProvenanceHit.getAppState().getCurrentState().name());
          experimentSummary.setFinished(fileProvenanceHit.getAppState().getFinishTime());
        } else {
          experimentSummary.setState(experimentSummary.getState());
          experimentSummary.setFinished(experimentSummary.getFinished());
        }
        
        experimentDTO.setState(experimentSummary.getState());
        experimentDTO.setFinished(experimentSummary.getFinished());

        if(updateNeeded) {
          experimentsController.attachExperiment(fileProvenanceHit.getMlId(), project, user,
              experimentSummary.getUserFullName(), experimentSummary, ExperimentDTO.XAttrSetFlag.REPLACE);
        }

        if(fileProvenanceHit.getXattrs().containsKey(EXPERIMENT_MODEL_XATTR_NAME)) {
          String model = fileProvenanceHit.getXattrs().get(EXPERIMENT_MODEL_XATTR_NAME);
          experimentDTO.setModel(model);
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

  private void buildFilter(Project project, ProvStateParamBuilder provFilesParamBuilder,
                                            Set<? extends AbstractFacade.FilterBy> filters)
      throws ProvenanceException {
    if(filters != null) {
      for (AbstractFacade.FilterBy filterBy : filters) {
        if(filterBy.getParam().compareToIgnoreCase(Filters.NAME_LIKE.name()) == 0) {
          HashMap<String, String> map = new HashMap<>();
          map.put(EXPERIMENT_SUMMARY_XATTR_NAME + ".name", filterBy.getValue());
          provFilesParamBuilder.filterLikeXAttrs(map);
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.NAME_EQ.name()) == 0) {
          HashMap<String, String> map = new HashMap<>();
          map.put(EXPERIMENT_SUMMARY_XATTR_NAME + ".name", filterBy.getValue());
          provFilesParamBuilder.filterByXAttrs(map);
        }else if(filterBy.getParam().compareToIgnoreCase(Filters.DATE_START_LT.name()) == 0) {
          Long timestamp = getDate(filterBy.getField(), filterBy.getValue()).getTime();
          provFilesParamBuilder.filterByField(ProvStateParser.FieldsPF.CREATE_TIMESTAMP_LT, timestamp);
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.DATE_START_GT.name()) == 0) {
          Long timestamp = getDate(filterBy.getField(), filterBy.getValue()).getTime();
          provFilesParamBuilder.filterByField(ProvStateParser.FieldsPF.CREATE_TIMESTAMP_GT, timestamp);
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.USER.name()) == 0) {
          String userId = filterBy.getValue();
          Users user = userFacade.find(Integer.parseInt(userId));
          String hdfsUserStr = hdfsUsersController.getHdfsUserName(project, user);
          HdfsUsers hdfsUsers = hdfsUsersFacade.findByName(hdfsUserStr);
          provFilesParamBuilder.filterByField(ProvStateParser.FieldsP.USER_ID, hdfsUsers.getId().toString());
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.STATE.name()) == 0) {
          HashMap<String, String> map = new HashMap<>();
          map.put(EXPERIMENT_SUMMARY_XATTR_NAME + ".state", filterBy.getValue());
          provFilesParamBuilder.filterLikeXAttrs(map);
        } else if(filterBy.getParam().compareToIgnoreCase(Filters.ID_EQ.name()) == 0) {
          HashMap<String, String> map = new HashMap<>();
          map.put(EXPERIMENT_SUMMARY_XATTR_NAME + ".id", filterBy.getValue());
          provFilesParamBuilder.filterByXAttrs(map);
        } else {
          throw new WebApplicationException("Filter by need to set a valid filter parameter, but found: " +
              filterBy.getParam(), Response.Status.NOT_FOUND);
        }
      }
    }
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

  public void buildSortOrder(ProvStateParamBuilder provFilesParamBuilder,
                             Set<? extends AbstractFacade.SortBy> sort) {
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
          throw new WebApplicationException("Sort by need to set a valid sort parameter, but found: " +
              sortBy.getParam(), Response.Status.NOT_FOUND);
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
    NAME_EQ,
    NAME_LIKE,
    DATE_START_LT,
    DATE_START_GT,
    USER,
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