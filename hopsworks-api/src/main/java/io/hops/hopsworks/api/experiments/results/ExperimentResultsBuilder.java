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
package io.hops.hopsworks.api.experiments.results;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.experiments.ExperimentConverter;
import io.hops.hopsworks.api.experiments.dto.results.ExperimentResultSummaryDTO;
import io.hops.hopsworks.api.experiments.dto.results.ExperimentResultsDTO;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ExperimentsException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExperimentResultsBuilder {

  private static final Logger LOGGER = Logger.getLogger(ExperimentResultsBuilder.class.getName());

  private static final int MAX_LIMIT = 1000;

  @EJB
  private DistributedFsService dfs;
  @EJB
  private ExperimentConverter experimentConverter;

  public ExperimentResultSummaryDTO uri(ExperimentResultSummaryDTO dto, UriInfo uriInfo, Project project, String mlId) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.EXPERIMENTS.toString().toLowerCase())
        .path(mlId)
        .path(ResourceRequest.Name.RESULTS.toString().toLowerCase())
        .build());
    return dto;
  }

  public ExperimentResultSummaryDTO expand(ExperimentResultSummaryDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.RESULTS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public ExperimentResultSummaryDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                          String mlId)
      throws ExperimentsException {
    ExperimentResultSummaryDTO dto = new ExperimentResultSummaryDTO();
    uri(dto, uriInfo, project, mlId);
    expand(dto, resourceRequest);
    dto.setCount(0l);
    if (dto.isExpand()) {
      DistributedFileSystemOps dfso = null;
      try {
        dfso = dfs.getDfsOps();
        String summaryPath = Utils.getProjectPath(project.getName()) + Settings.HOPS_EXPERIMENTS_DATASET + "/"
            + mlId + "/.summary.json";
        if (dfso.exists(summaryPath)) {
          String summaryJson = dfso.cat(new Path(summaryPath));
          if(!Strings.isNullOrEmpty(summaryJson)) {
            ExperimentResultsDTO[] results = experimentConverter
                .unmarshalResults(summaryJson).getCombinations();
            if (results != null) {
              dto.setCount((long) results.length);
              results = apply(results, resourceRequest);
              dto.setCombinations(results);
            }
          }
        }
      } catch (Exception e) {
        throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.RESULTS_RETRIEVAL_ERROR, Level.SEVERE,
            "Unable to get results for experiment", e.getMessage(), e);
      } finally {
        if (dfso != null) {
          dfs.closeDfsClient(dfso);
        }
      }
    }
    return dto;
  }

  private ExperimentResultsDTO[] apply(ExperimentResultsDTO[] dto, ResourceRequest resourceRequest) {

    if (dto == null || dto.length == 1) {
      return dto;
    }

    Integer limit = resourceRequest.getLimit();

    if (limit == null) {
      limit = ExperimentResultsBuilder.MAX_LIMIT;
    }

    Integer offset = resourceRequest.getOffset();

    if (offset == null) {
      offset = 0;
    }

    AbstractFacade.SortBy sortByKey = null;
    String sortKeyName = null;
    String sortKeyOrder = null;
    if(resourceRequest.getSort() != null) {
      sortByKey = resourceRequest.getSort().iterator().next();
      sortKeyName = sortByKey.getValue();
      sortKeyOrder = sortByKey.getParam().getValue();
    }

    if(sortByKey != null && sortKeyName != null && sortKeyOrder != null) {
      if(sortKeyOrder.compareToIgnoreCase("ASC") == 0) {
        Arrays.sort(dto, new OptKeyComparator(sortKeyName));
      } else if(sortKeyOrder.compareToIgnoreCase("DESC") == 0) {
        Arrays.sort(dto, Collections.reverseOrder(new OptKeyComparator(sortKeyName)));
      }
    }

    ArrayList<ExperimentResultsDTO> results = new ArrayList<>();

    if (dto.length > 0) {
      for (int i = 0; offset + i < (offset + limit) && (offset + i) < dto.length; i++) {
        results.add(dto[offset + i]);
      }
    } else {
      return dto;
    }

    return results.toArray(new ExperimentResultsDTO[results.size()]);
  }

  public static class OptKeyComparator implements Comparator {
    private String sortKey;

    OptKeyComparator(String sortKey) {
      this.sortKey = sortKey;
    }

    @Override
    public int compare(Object experimentA, Object experimentB) {
      ExperimentResultsDTO firstExperiment = (ExperimentResultsDTO) experimentA;
      ExperimentResultsDTO secondExperiment = (ExperimentResultsDTO) experimentB;
      return getSortValue(firstExperiment, sortKey)
          .compareTo(getSortValue(secondExperiment, sortKey));
    }

    private Comparable getSortValue(ExperimentResultsDTO experiment, String sortKey) {
      for (Map.Entry<QName, String> entry: experiment.getParameters().getAttributes().entrySet()) {
        if (entry.getKey().getLocalPart().compareTo(sortKey) == 0) {
          return getComparable(entry.getValue());
        }
      }
      for (Map.Entry<QName, String> entry : experiment.getOutputs().getAttributes().entrySet()) {
        if (entry.getKey().getLocalPart().compareTo(sortKey) == 0) {
          return getComparable(entry.getValue());
        }
      }
      return 0.0;
    }

    private Comparable getComparable(String value) {
      try {
        return Double.parseDouble(value);
      } catch(Exception e) {
        return value;
      }
    }
  }
}
