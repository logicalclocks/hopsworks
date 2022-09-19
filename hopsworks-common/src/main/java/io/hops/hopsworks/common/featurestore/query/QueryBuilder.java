/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.query;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterDTO;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogicDTO;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.join.JoinDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class QueryBuilder {

  private static class FeatureSignature {

    public String featureName;
    public Integer featureGroupId;
    public Integer featureGroupVersion;

    public FeatureSignature(Feature feature) {
      this.featureName = feature.getName();
      this.featureGroupId = feature.getFeatureGroup().getId();
      this.featureGroupVersion = feature.getFeatureGroup().getVersion();
    }

    public FeatureSignature(FeatureGroupFeatureDTO feature, Integer version) {
      this.featureName = feature.getName();
      this.featureGroupId = feature.getFeatureGroupId();
      this.featureGroupVersion = version;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FeatureSignature that = (FeatureSignature) o;
      return featureName.equals(that.featureName) && featureGroupId.equals(that.featureGroupId) &&
          featureGroupVersion.equals(that.featureGroupVersion);
    }

    @Override
    public int hashCode() {
      return Objects.hash(featureName, featureGroupId, featureGroupVersion);
    }
  }

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeaturestoreController featurestoreController;

  public QueryBuilder() {
  }

  public QueryDTO build(Query query, Featurestore featurestore, Project project, Users user)
      throws FeaturestoreException, ServiceException {
    if (query.getDeletedFeatureGroups() != null && !query.getDeletedFeatureGroups().isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND,
          Level.SEVERE, String.format("Cannot construct the query. " +
          "Parent feature groups of the following features are not available anymore: " +
          "%s", String.join(", ", query.getDeletedFeatureGroups())));
    }
    // featureToDTO and allJoinedFeatures are set only once at the top level query.
    Map<FeatureSignature, FeatureGroupFeatureDTO> featureToDTO = makeFeatureToFeatureDTOMap(query, project, user);

    List<FeatureGroupFeatureDTO> allJoinedFeatures = Lists.newArrayList();
    for (Feature feature: query.getFeatures()) {
      allJoinedFeatures.add(convertToFeatureDTO(feature, featureToDTO));
    }
    return build(query, featurestore, project, user, featureToDTO, allJoinedFeatures);
  }

  private QueryDTO build(Query query, Featurestore featurestore, Project project, Users user,
      Map<FeatureSignature, FeatureGroupFeatureDTO> featureToDTO, List<FeatureGroupFeatureDTO> allJoinedFeatures)
      throws FeaturestoreException, ServiceException {

    QueryDTO queryDTO = new QueryDTO();
    String featureStoreName = query.getFeatureStore();
    // featureStoreId has to match with featureStoreName.
    // Query's featureStoreName may not be the same as current feature store name in the case of shared project.
    try {
      Integer featureStoreId = featurestoreController.getFeaturestoreForProjectWithName(project, featureStoreName)
          .getFeaturestoreId();
      queryDTO.setFeatureStoreId(featureStoreId);
    } catch (FeaturestoreException e) {
      if (RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND.equals(e.getErrorCode())) {
        // This can happen when accessing an unshared feature store.
        queryDTO.setFeatureStoreId(null);
      } else {
        throw e;
      }
    }
    FeaturegroupDTO leftFeatureGroup =
        featuregroupController.convertFeaturegrouptoDTO(query.getFeaturegroup(), project, user);
    leftFeatureGroup.setFeatures(featuregroupController.getFeatures(query.getFeaturegroup(), project, user));
    Long leftFeatureGroupStartTime = query.getLeftFeatureGroupStartTimestamp();
    Long leftFeatureGroupEndTime = query.getLeftFeatureGroupEndTimestamp();
    FilterLogicDTO filter = convertToFilterLogicDTO(query.getFilter(), featureToDTO);
    Boolean hiveEngine = query.getHiveEngine();
    List<JoinDTO> joins = convertToJoinDTOs(
        query.getJoins(), featurestore, project, user, featureToDTO, allJoinedFeatures);
    queryDTO.setFeatureStoreName(featureStoreName);
    queryDTO.setLeftFeatureGroup(leftFeatureGroup);
    queryDTO.setLeftFeatures(
        allJoinedFeatures.stream()
            // Select features which belong to the same feature group as the current query only.
            .filter(feature -> feature.getFeatureGroupId().equals(query.getFeaturegroup().getId()))
            .collect(Collectors.toList())
    );
    queryDTO.setLeftFeatureGroupStartTime(leftFeatureGroupStartTime);
    queryDTO.setLeftFeatureGroupEndTime(leftFeatureGroupEndTime);
    queryDTO.setFilter(filter);
    queryDTO.setHiveEngine(hiveEngine);
    queryDTO.setJoins(joins);
    return queryDTO;
  }

  Map<FeatureSignature, FeatureGroupFeatureDTO> makeFeatureToFeatureDTOMap(Query query, Project project, Users user)
      throws FeaturestoreException {
    Map<FeatureSignature, FeatureGroupFeatureDTO> featureToDTO = Maps.newHashMap();
    // Cannot use set because Featuregroup.equals create infinite loop when comparing statisticsConfig
    Map<Integer, Featuregroup> featuregroups = query.getJoins()
        .stream().map(join -> join.getRightQuery().getFeaturegroup())
        .collect(Collectors.toMap(Featuregroup::getId, fg -> fg, (f1, f2) -> f1));
    featuregroups.put(query.getFeaturegroup().getId(), query.getFeaturegroup());
    for (Featuregroup featuregroup : featuregroups.values()) {
      featuregroupController
          .getFeatures(featuregroup, project, user)
          .forEach(featureDto ->
              featureToDTO.put(new FeatureSignature(featureDto, featuregroup.getVersion()), featureDto));
    }
    return featureToDTO;
  }

  FilterLogicDTO convertToFilterLogicDTO(FilterLogic filterLogic,
      Map<FeatureSignature, FeatureGroupFeatureDTO> featureToDTO) throws FeaturestoreException {
    if (filterLogic == null) {
      return null;
    }
    FilterLogicDTO filterLogicDTO = new FilterLogicDTO(filterLogic.getType());
    filterLogicDTO.setLeftFilter(convertToFilterDTO(filterLogic.getLeftFilter(), featureToDTO));
    filterLogicDTO.setRightFilter(convertToFilterDTO(filterLogic.getRightFilter(), featureToDTO));
    filterLogicDTO.setLeftLogic(convertToFilterLogicDTO(filterLogic.getLeftLogic(), featureToDTO));
    filterLogicDTO.setRightLogic(convertToFilterLogicDTO(filterLogic.getRightLogic(), featureToDTO));
    return filterLogicDTO;
  }

  FilterDTO convertToFilterDTO(Filter filter, Map<FeatureSignature, FeatureGroupFeatureDTO> featureToDTO)
      throws FeaturestoreException {
    if (filter == null) {
      return null;
    }
    FeatureGroupFeatureDTO feature = convertToFeatureDTO(filter.getFeatures().get(0), featureToDTO);
    SqlCondition condition = filter.getCondition();
    String value = filter.getValue().getValue();
    return new FilterDTO(feature, condition, value);
  }

  FeatureGroupFeatureDTO convertToFeatureDTO(Feature feature,
      Map<FeatureSignature, FeatureGroupFeatureDTO> featureToDTO) throws FeaturestoreException {
    FeatureGroupFeatureDTO featureDTO = featureToDTO.get(new FeatureSignature(feature));
    if (featureDTO == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_NOT_FOUND,
          Level.SEVERE, feature.getName() + " is not found");
    }
    return featureDTO;
  }

  List<JoinDTO> convertToJoinDTOs(List<Join> joins, Featurestore featurestore, Project project, Users user,
      Map<FeatureSignature, FeatureGroupFeatureDTO> featureToDTO, List<FeatureGroupFeatureDTO> allJoinedFeatures)
      throws FeaturestoreException, ServiceException {
    if (joins == null || joins.isEmpty()) {
      return null;
    }
    List<JoinDTO> joinDTOS = Lists.newArrayList();
    for (Join join : joins) {
      joinDTOS.add(convertToJoinDTO(join, featurestore, project, user, featureToDTO, allJoinedFeatures));
    }
    return joinDTOS;
  }

  JoinDTO convertToJoinDTO(Join join, Featurestore featurestore, Project project, Users user,
      Map<FeatureSignature, FeatureGroupFeatureDTO> featureToDTO, List<FeatureGroupFeatureDTO> allJoinedFeatures)
      throws FeaturestoreException, ServiceException {
    JoinDTO joinDTO = new JoinDTO();
    QueryDTO queryDTO = build(join.getRightQuery(), featurestore, project, user, featureToDTO, allJoinedFeatures);
    List<FeatureGroupFeatureDTO> rightOn = join.getRightOn()
        .stream().map(feature -> {
          // Features in join condition only contain name.
          FeatureGroupFeatureDTO featureDTO = new FeatureGroupFeatureDTO();
          featureDTO.setName(feature.getName());
          return featureDTO;
        }).collect(Collectors.toList());
    List<FeatureGroupFeatureDTO> leftOn = join.getLeftOn()
        .stream().map(feature -> {
          FeatureGroupFeatureDTO featureDTO = new FeatureGroupFeatureDTO();
          featureDTO.setName(feature.getName());
          return featureDTO;
        }).collect(Collectors.toList());
    joinDTO.setQuery(queryDTO);
    joinDTO.setRightOn(rightOn);
    joinDTO.setLeftOn(leftOn);
    joinDTO.setType(join.getJoinType());
    joinDTO.setPrefix(join.getPrefix());
    return joinDTO;
  }
}
