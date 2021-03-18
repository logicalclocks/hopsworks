/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance.ops;

import com.google.common.primitives.Longs;
import io.hops.hopsworks.api.dataset.DatasetBuilder;
import io.hops.hopsworks.api.dataset.DatasetDTO;
import io.hops.hopsworks.api.jobs.JobDTO;
import io.hops.hopsworks.api.jobs.JobsBuilder;
import io.hops.hopsworks.api.jobs.executions.ExecutionDTO;
import io.hops.hopsworks.api.jobs.executions.ExecutionsBuilder;
import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.ops.ProvOps;
import io.hops.hopsworks.common.provenance.ops.ProvOpsAggregations;
import io.hops.hopsworks.common.provenance.ops.ProvOpsParamBuilder;
import io.hops.hopsworks.common.provenance.ops.ProvOpsReturnType;
import io.hops.hopsworks.common.provenance.ops.ProvUsageType;
import io.hops.hopsworks.api.provenance.ops.dto.ProvArtifactUsageParentDTO;
import io.hops.hopsworks.api.provenance.ops.dto.ProvArtifactUsageDTO;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvUsageBuilder {
  @EJB
  private HdfsUsersFacade hdfsUserFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private ProvOpsBuilder opsBuilder;
  @EJB
  private AccessController accessController;
  @EJB
  private JobsBuilder jobsBuilder;
  @EJB
  private ExecutionsBuilder executionsBuilder;
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private DatasetBuilder datasetBuilder;
  
  public ProvArtifactUsageParentDTO buildAccessible(UriInfo uriInfo, Project userProject, Dataset targetEndpoint,
    String artifactId, Set<ProvUsageType> type)
    throws ProvenanceException, GenericException {
    if(!accessController.hasAccess(userProject, targetEndpoint)) {
      throw new GenericException(RESTCodes.GenericErrorCode.NOT_AUTHORIZED_TO_ACCESS, Level.FINE);
    }
  
    ProvArtifactUsageParentDTO usage = new ProvArtifactUsageParentDTO();
    usage.setArtifactId(artifactId);
    usage.setDataset(getDataset(uriInfo, targetEndpoint.getId()));
    usage.setProjectId(targetEndpoint.getProject().getId());
    usage.setProjectName(targetEndpoint.getProject().getName());
    
    ProvOpsParamBuilder params = getBasicUsageOpsParams(targetEndpoint, artifactId);
    ProvOpsDTO ops = opsBuilder.build(targetEndpoint.getProject(), params, ProvOpsReturnType.AGGREGATIONS);
    Optional<ProvOpsDTO> aggregation = ops.getItems().stream()
      .filter(agg -> agg.getAggregation() != null
        && agg.getAggregation().equals(ProvOpsAggregations.APP_USAGE.toString()))
      .findFirst();
    if(!aggregation.isPresent()) {
      return usage;
    }
    Optional<ProvOpsDTO> artifact = aggregation.get().getItems().stream()
      .filter(art -> art.getMlId().equals(artifactId))
      .findFirst();
    if(!artifact.isPresent()) {
      return usage;
    }
    
    for(ProvUsageType t : type) {
      switch(t) {
        case READ_CURRENT:
          usage.setReadCurrent(usage(uriInfo, artifact.get(), Provenance.FileOps.ACCESS_DATA, true));
          break;
        case WRITE_CURRENT:
          usage.setWriteCurrent(usage(uriInfo, artifact.get(), Provenance.FileOps.MODIFY_DATA, true));
          break;
        case READ_LAST:
          lastUsage(uriInfo, artifact.get(), Provenance.FileOps.ACCESS_DATA).ifPresent(usage::setReadLast);
          break;
        case WRITE_LAST:
          lastUsage(uriInfo, artifact.get(), Provenance.FileOps.MODIFY_DATA).ifPresent(usage::setWriteLast);
          break;
        case READ_HISTORY:
          usage.setReadHistory(usage(uriInfo, artifact.get(), Provenance.FileOps.ACCESS_DATA, false));
          break;
        case WRITE_HISTORY:
          usage.setWriteHistory(usage(uriInfo, artifact.get(), Provenance.FileOps.MODIFY_DATA, false));
          break;
      }
    }
    return usage;
  }
  
  private ProvOpsParamBuilder getBasicUsageOpsParams(Dataset targetEndpoint, String artifactId)
    throws ProvenanceException {
    return new ProvOpsParamBuilder()
      .filterByField(ProvOps.FieldsP.ML_ID, artifactId)
      .filterByField(ProvOps.FieldsP.DATASET_I_ID, targetEndpoint.getInode().getId())
      .withAggregation(ProvOpsAggregations.APP_USAGE);
  }
  
  //ops are artifacts -> apps -> ops
  private List<ProvArtifactUsageDTO> usage(UriInfo uriInfo, ProvOpsDTO artifact, Provenance.FileOps opType,
    boolean runningJob) {
    return artifact.getItems().stream()
      .filter( //filter only running application
        app -> {
          if (runningJob) {
            Optional<Execution> ex = executionFacade.findByAppId(app.getAppId());
            return ex.isPresent() && !ex.get().getState().isFinalState();
          } else {
            return true;
          }
        })
      .flatMap(app -> app.getItems().stream().map(op -> Pair.with(app, op)))
      .filter(entry -> entry.getValue1().getInodeOperation().equals(opType))
      .map(entry -> {
        ProvArtifactUsageDTO usage = new ProvArtifactUsageDTO();
        usage.setExecution(getExecution(uriInfo, entry.getValue0().getAppId()));
        usage.setJob(getJob(uriInfo, entry.getValue0().getAppId()));
        usage.setUser(getUser(uriInfo, entry.getValue0().getUserId()));
        usage.setTimestamp(entry.getValue1().getTimestamp());
        usage.setReadableTimestamp(entry.getValue1().getReadableTimestamp());
        return usage;
      })
      .collect(Collectors.toList());
  }
  
  private Optional<ProvArtifactUsageDTO> lastUsage(UriInfo uriInfo, ProvOpsDTO ops, Provenance.FileOps opType) {
    List<ProvArtifactUsageDTO> aux = usage(uriInfo, ops, opType, false);
    if(!aux.isEmpty()) {
      aux.sort(timestampComparator());
      return Optional.of(aux.get(0));
    }
    return Optional.empty();
  }
  
  private DatasetDTO getDataset(UriInfo uriInfo, Integer datasetId) {
    Dataset dataset = datasetFacade.find(datasetId);
    if (dataset != null) {
      return datasetBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.DATASETS), dataset);
    }
    return null;
  }
  
  private ExecutionDTO getExecution(UriInfo uriInfo, String appId) {
    if(appId != null && !appId.equals("none")) {
      Optional<Execution> execOptional = executionFacade.findByAppId(appId);
      if (execOptional.isPresent()) {
        return executionsBuilder
            .build(uriInfo, new ResourceRequest(ResourceRequest.Name.EXECUTIONS), execOptional.get());
      }
    }
    return null;
  }
  
  private JobDTO getJob(UriInfo uriInfo, String appId) {
    if(appId != null && !appId.equals("none")) {
      Optional<Execution> execOptional = executionFacade.findByAppId(appId);
      if (execOptional.isPresent()) {
        return jobsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.JOBS), execOptional.get().getJob());
      }
    }
    return null;
  }
  
  private UserDTO getUser(UriInfo uriInfo, Integer hdfsUserId) {
    HdfsUsers hdfsUser = hdfsUserFacade.findById(hdfsUserId);
    if (hdfsUser != null) {
      Users user = userFacade.findByUsername(hdfsUser.getUsername());
      if (user != null) {
        return usersBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.USERS), user);
      }
    }
    return null;
  }
  
  private Comparator<ProvArtifactUsageDTO> timestampComparator() {
    return (o1, o2) -> Longs.compare(o1.getTimestamp(), o2.getTimestamp());
  }
}
