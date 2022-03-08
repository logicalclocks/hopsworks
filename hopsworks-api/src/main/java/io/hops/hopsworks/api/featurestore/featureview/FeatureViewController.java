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

package io.hops.hopsworks.api.featurestore.featureview;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.QueryParam;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewDTO;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewFacade;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryController;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.pit.PitJoinController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionFacade;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static io.hops.hopsworks.restutils.RESTCodes.FeaturestoreErrorCode.FEATURE_VIEW_NOT_FOUND;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureViewController {

  @EJB
  private FeatureViewFacade featureViewFacade;
  @EJB
  private QueryController queryController;
  @Inject
  private PitJoinController pitJoinController;
  @EJB
  private TransformationFunctionFacade transformationFunctionFacade;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private FeatureViewInputValidator featureViewInputValidator;
  @EJB
  private FeaturestoreUtils featurestoreUtils;

  public FeatureView createFeatureView(FeatureView featureView, Featurestore featurestore)
      throws FeaturestoreException {
    // if version not provided, get latest and increment
    if (featureView.getVersion() == null) {
      // returns ordered list by desc version
      Integer latestVersion = featureViewFacade.findLatestVersion(featureView.getName(), featurestore);
      if (latestVersion != null) {
        featureView.setVersion(latestVersion + 1);
      } else {
        featureView.setVersion(1);
      }
    }

    // Check that feature view doesn't already exists
    if (!featureViewFacade
        .findByNameVersionAndFeaturestore(featureView.getName(), featureView.getVersion(), featurestore)
        .isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_VIEW_ALREADY_EXISTS, Level.FINE,
          "Feature view: " + featureView.getName() + ", version: " + featureView.getVersion());
    }

    featureView = featureViewFacade.update(featureView);
    return featureView;
  }

  public List<FeatureView> getAll() {
    return featureViewFacade.findAll();
  }

  public List<FeatureView> getByFeatureStore(Featurestore featurestore, ResourceRequest resourceRequest) {
    return featureViewFacade.findByFeaturestore(featurestore, convertToQueryParam(resourceRequest));
  }

  public List<FeatureView> getByNameAndFeatureStore(String name, Featurestore featurestore,
      ResourceRequest resourceRequest) {
    return featureViewFacade.findByNameAndFeaturestore(name, featurestore, convertToQueryParam(resourceRequest));
  }

  public List<FeatureView> getByNameVersionAndFeatureStore(String name, Integer version, Featurestore featurestore,
      ResourceRequest resourceRequest) {
    return featureViewFacade.findByNameVersionAndFeaturestore(name, version, featurestore,
        convertToQueryParam(resourceRequest));
  }

  public void delete(Users user, Project project, Featurestore featurestore, String name)
      throws FeaturestoreException {
    List<FeatureView> featureViews = featureViewFacade.findByNameAndFeaturestore(name, featurestore);
    delete(user, project, featurestore, featureViews);
  }

  public void delete(Users user, Project project, Featurestore featurestore, String name, Integer version)
      throws FeaturestoreException {
    List<FeatureView> featureViews = featureViewFacade.findByNameVersionAndFeaturestore(name, version, featurestore);
    delete(user, project, featurestore, featureViews);
  }

  private void delete(Users user, Project project, Featurestore featurestore, List<FeatureView> featureViews)
      throws FeaturestoreException {
    if (featureViews == null || featureViews.isEmpty()) {
      throw new FeaturestoreException(FEATURE_VIEW_NOT_FOUND, Level.FINE, "Provided feature view name or version " +
          "does not exist.");
    }
    for (FeatureView fv: featureViews) {
      featurestoreUtils.verifyUserRole(fv, featurestore, user, project);
    }
    for (FeatureView fv: featureViews) {
      featureViewFacade.remove(fv);
    }
  }

  private QueryParam convertToQueryParam(ResourceRequest resourceRequest) {
    return new QueryParam(
        resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        (Set<AbstractFacade.FilterBy>) resourceRequest.getFilter(),
        (Set<AbstractFacade.SortBy>) resourceRequest.getSort()
    );
  }

  public FeatureView convertFromDTO(Project project, Featurestore featurestore, Users user,
      FeatureViewDTO featureViewDTO) throws FeaturestoreException {
    featureViewInputValidator.validate(featureViewDTO, project, user);
    FeatureView featureView = new FeatureView();
    featureView.setName(featureViewDTO.getName());
    featureView.setFeaturestore(featurestore);
    featureView.setCreated(featureViewDTO.getCreated() == null ? new Date(): featureViewDTO.getCreated());
    featureView.setCreator(user);
    featureView.setVersion(featureViewDTO.getVersion());
    featureView.setDescription(featureViewDTO.getDescription());
    setQuery(project, user, featureViewDTO.getQuery(), featureView, featureViewDTO.getFeatures());
    return featureView;
  }

  public Query makeQuery(FeatureView featureView, Project project, Users user) throws FeaturestoreException {

    List<TrainingDatasetJoin> joins = featureView.getJoins().stream()
        .sorted(Comparator.comparing(TrainingDatasetJoin::getIndex))
        .collect(Collectors.toList());

    Map<Integer, String> fgAliasLookup = trainingDatasetController.getAliasLookupTable(joins);

    List<TrainingDatasetFeature> tdFeatures = featureView.getFeatures().stream()
        .sorted((t1, t2) -> {
          if (t1.getIndex() != null) {
            // compare based on index
            return t1.getIndex().compareTo(t2.getIndex());
          } else {
            // Old training dataset with no index. compare based on name
            return t1.getName().compareTo(t2.getName());
          }
        })
        // drop label features if desired
        .collect(Collectors.toList());

    // Check that all the feature groups still exists, if not throw a reasonable error
    if (tdFeatures.stream().anyMatch(j -> j.getFeatureGroup() == null)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_QUERY_FG_DELETED, Level.FINE);
    }

    // Get available features for all involved feature groups once, and save in map fgId -> availableFeatures
    Map<Integer, List<Feature>> availableFeaturesLookup = new HashMap<>();
    for (TrainingDatasetJoin join : joins) {
      if (!availableFeaturesLookup.containsKey(join.getFeatureGroup().getId())) {
        List<Feature> availableFeatures = featuregroupController.getFeatures(join.getFeatureGroup(), project, user)
            .stream()
            .map(f -> new Feature(f.getName(),
                fgAliasLookup.get(join.getId()), f.getType(), f.getPrimary(), f.getDefaultValue(), join.getPrefix()))
            .collect(Collectors.toList());
        availableFeaturesLookup.put(join.getFeatureGroup().getId(), availableFeatures);
      }
    }

    List<Feature> features = new ArrayList<>();
    for (TrainingDatasetFeature requestedFeature : tdFeatures) {
      features.add(availableFeaturesLookup.get(requestedFeature.getFeatureGroup().getId())
          .stream()
          .filter(af -> af.getName().equals(requestedFeature.getName()))
          // instantiate new feature since alias in available feature is not correct if fg is joined with itself
          .map(af -> new Feature(af.getName(), fgAliasLookup.get(requestedFeature.getTrainingDatasetJoin().getId()),
              af.getType(), af.getDefaultValue(), af.getPrefix(), requestedFeature.getFeatureGroup(),
              requestedFeature.getIndex()))
          .findFirst()
          .orElseThrow(
            () -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST, Level.FINE,
                  "Feature: " + requestedFeature.getName() + " not found in feature group: " +
                      requestedFeature.getFeatureGroup().getName())));
    }

    // Keep a map feature store id -> feature store name
    Map<Integer, String> fsLookup = trainingDatasetController.getFsLookupTableJoins(joins);

    Query query = new Query(
        fsLookup.get(joins.get(0).getFeatureGroup().getFeaturestore().getId()),
        onlineFeaturestoreController
            .getOnlineFeaturestoreDbName(joins.get(0).getFeatureGroup().getFeaturestore().getProject()),
        joins.get(0).getFeatureGroup(),
        fgAliasLookup.get(joins.get(0).getId()),
        features,
        availableFeaturesLookup.get(joins.get(0).getFeatureGroup().getId()),
        false);

    // Set the remaining feature groups as join
    List<Join> queryJoins = new ArrayList<>();
    for (int i = 1; i < joins.size(); i++) {
      // left side of the join stays fixed, the counter starts at 1
      queryJoins.add(trainingDatasetController.getQueryJoin(query, joins.get(i), fgAliasLookup, fsLookup,
          availableFeaturesLookup, false));
    }
    query.setJoins(queryJoins);
    return query;
  }

  public List<TrainingDatasetFeature> getFeaturesSorted(Collection<TrainingDatasetFeature> features) {
    return features.stream()
        .sorted((t1, t2) -> {
          if (t1.getIndex() != null) {
            // compare based on index
            return t1.getIndex().compareTo(t2.getIndex());
          } else {
            // Old training dataset with no index. compare based on name
            return t1.getName().compareTo(t2.getName());
          }
        })
        .collect(Collectors.toList());
  }

  private void setQuery(Project project, Users user, QueryDTO queryDTO, FeatureView featureView,
      List<TrainingDatasetFeatureDTO> featureDTOs)
      throws FeaturestoreException {
    if (queryDTO != null) {
      Query query = queryController.convertQueryDTO(project, user, queryDTO,
          pitJoinController.isPitEnabled(queryDTO));
      List<TrainingDatasetJoin> tdJoins = trainingDatasetController.collectJoins(query, null, featureView);
      featureView.setJoins(tdJoins);
      List<TrainingDatasetFeature> features = trainingDatasetController.collectFeatures(query, featureDTOs,
          null, featureView, 0, tdJoins, 0);
      featureView.setFeatures(features);
    }
  }

}
