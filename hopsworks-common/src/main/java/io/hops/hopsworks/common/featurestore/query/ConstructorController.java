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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.dialect.SparkSqlDialect;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ConstructorController {

  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private FeaturegroupController featuregroupController;

  private final static String ALL_FEATURES = "*";

  public ConstructorController() { }

  // For testing
  protected ConstructorController(FeaturegroupController featuregroupController) {
    this.featuregroupController = featuregroupController;
  }

  public String construct(QueryDTO queryDTO) throws FeaturestoreException {
    Query query = convertDTOtoInt(queryDTO);
    validateQuery(query, 0);

    List<FeatureDTO> selectedFeatures = extractSelectedFeatures(query);
    // TODO(Fabio) sort recursive
    List<Join> joins = Arrays.asList(extractJoins(query));

    // Generate SQL
    return generateSQL(selectedFeatures, joins);
  }

  private Query convertDTOtoInt(QueryDTO queryDTO) {
    return new Query(queryDTO.getLeftFeatureGroup(), queryDTO.getLeftFeatures(),
        queryDTO.getQueryDTO() != null ? convertDTOtoInt(queryDTO.getQueryDTO()) : null,
        queryDTO.getRightFeatureGroup(), queryDTO.getRightFeatures(), queryDTO.getOn(),
        queryDTO.getLeftOn(), queryDTO.getRightOn(), queryDTO.getType());
  }

  private void validateQuery(Query query, int fgId) {
    // It should have a leftFeatureGroup set and leftFeatures
    query.setLeftFg(
        validateFeaturegroupDTO(query.getLeftFgDTO(), query.getLeftFeatures()));
    query.setLeftAvailableFeatures(featuregroupController.getFeatures(query.getLeftFg()));
    query.setLeftFgAs("fg" + fgId++);

    if (query.getQuery() != null && query.getRightFgDTO() != null) {
      throw new IllegalArgumentException("Nested join and right feature group specified in the same request");
    }

    if (query.getQuery() != null) {
      validateQuery(query.getQuery(), fgId);
    } else if (query.getRightFg() != null){
      query.setRightFg(
          validateFeaturegroupDTO(query.getRightFgDTO(), query.getRightFeatures()));
      query.setRightAvailableFeatures(featuregroupController.getFeatures(query.getRightFg()));
      query.setRightFgAs("fg" + fgId);
    }
  }

  private Featuregroup validateFeaturegroupDTO(FeaturegroupDTO featuregroupDTO, List<FeatureDTO> features) {
    if (featuregroupDTO == null ||
        features == null || features.isEmpty()) {
      throw new IllegalArgumentException("Feature group not specified or the list of features is empty");
    } else {
      Featuregroup featuregroup = featuregroupFacade.findById(featuregroupDTO.getId());
      if (featuregroup == null) {
        throw new IllegalArgumentException("Could not find feature group with ID"
            + featuregroupDTO.getId());
      }
      return featuregroup;
    }
  }

  protected List<FeatureDTO> extractSelectedFeatures(Query query) {
    List<FeatureDTO> featureList =
        extractSelectedFeatures(query.getLeftFg(), query.getLeftFgAs(),query.getLeftFeatures());

    if (query.getQuery() != null) {
      featureList.addAll(extractSelectedFeatures(query.getQuery()));
    } else if (query.getRightFg() != null) {
      featureList.addAll(extractSelectedFeatures(query.getRightFg(), query.getRightFgAs(), query.getRightFeatures()));
    }

    return featureList;
  }

  protected List<FeatureDTO> extractSelectedFeatures(Featuregroup fg, String as, List<FeatureDTO> requestedFeatures) {
    List<FeatureDTO> featureList = new ArrayList<>();
    List<FeatureDTO> availableFeatures = featuregroupController.getFeatures(fg);

    availableFeatures.forEach(f -> f.setFeaturegroup(as));

    if (requestedFeatures == null || requestedFeatures.isEmpty()) {
      throw new IllegalArgumentException("Invalid requested features");
    } else if (requestedFeatures.size() == 1 && requestedFeatures.get(0).getName().equals(ALL_FEATURES)) {
      // Users can specify * to request all the features in a specific feature group
      featureList.addAll(availableFeatures);
    } else {
      // Check that all the requested features are available in the list, based on the provided name
      for (FeatureDTO requestedFeature : requestedFeatures) {
        featureList.add(availableFeatures.stream().filter(af -> af.getName().equals(requestedFeature.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Feature: " + requestedFeature.getName()
                    + " not found in feature group: " + fg.getName())));
      }
    }

    return featureList;
  }

  private Join extractJoins(Query query) {
    if (query.getOn() != null && !query.getOn().isEmpty()) {
      return extractOn(query);
    } else if (query.getLeftOn() != null && !query.getLeftOn().isEmpty()) {
      return extractLeftRightOn(query);
    } else if (query.getRightFg() != null) {
      // Only if right feature group is present, extract the primary keys for the join
      return extractPrimaryKeysJoin(query);
    } else {
      // Right side not present
      return new Join(featurestoreFacade, query.getLeftFg(), query.getLeftFgAs());
    }
  }

  // TODO(Fabio): investigate type compatibility
  protected Join extractOn(Query query) {
    // Make sure that the joining features are available on both feature groups and have the same type on both
    for (FeatureDTO joinFeature : query.getOn()) {
      FeatureDTO leftFeature = query.getLeftAvailableFeatures().stream()
          .filter(f -> f.getName().equals(joinFeature.getName()))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Could not find Join feature: " + joinFeature.getName() +
              " in feature group: " + query.getLeftFg().getName()));
      // Make sure that the same feature (name and type) is available on the right side as well.
      if (query.getRightAvailableFeatures().stream()
          .noneMatch(f -> (f.getName().equals(joinFeature.getName()) && f.getType().equals(leftFeature.getType())))) {
        throw new IllegalArgumentException("Could not find Join feature " + joinFeature.getName() +
            " in feature group: " + query.getRightFg().getName() + ", or it doesn't have the expected type");
      }
    }

    return new Join(featurestoreFacade, query.getLeftFg(), query.getLeftFgAs(),
        query.getRightFg(), query.getRightFgAs(), query.getOn(), query.getType());
  }

  protected Join extractLeftRightOn(Query query) {
    // Make sure that they 2 list have the same length, and that the respective features have the same type
    if (query.getLeftOn().size() != query.getRightOn().size()) {
      throw new IllegalArgumentException("LeftOn and RightOn have different sizes");
    }
    int i = 0;
    while (i < query.getLeftOn().size()) {
      String leftFeatureName = query.getLeftOn().get(i).getName();
      FeatureDTO leftFeature = query.getLeftAvailableFeatures().stream()
          .filter(f -> f.getName().equals(leftFeatureName))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Could not find Join feature: " + leftFeatureName +
              " in feature group: " + query.getLeftFg().getName()));

      String rightFeatureName = query.getRightOn().get(i).getName();
      if (query.getRightAvailableFeatures().stream()
          .noneMatch(f -> (f.getName().equals(rightFeatureName) && f.getType().equals(leftFeature.getType())))) {
        throw new IllegalArgumentException("Could not find Join feature " + rightFeatureName +
            " in feature group: " + query.getRightFg().getName() + ", or it doesn't have the expected type");
      }
      i++;
    }
    return new Join(featurestoreFacade, query.getLeftFg(), query.getLeftFgAs(), query.getRightFg(),
        query.getRightFgAs(), query.getLeftOn(), query.getRightOn(), query.getType());
  }

  protected Join extractPrimaryKeysJoin(Query query) {
    // Find subset of matching primary keys (same name and type) to be used as join condition
    List<FeatureDTO> joinFeatures = new ArrayList<>();
    query.getLeftAvailableFeatures().stream().filter(FeatureDTO::getPrimary).forEach(lf -> {
      joinFeatures.addAll(query.getRightAvailableFeatures().stream()
          .filter(rf -> rf.getName().equals(lf.getName()) && rf.getType().equals(lf.getType()) && rf.getPrimary())
          .collect(Collectors.toList()));
    });

    if (joinFeatures.isEmpty()) {
      throw new IllegalArgumentException("Could not find any matching feature to join: "
          + query.getLeftFg().getName() + " and: " + query.getRightFg().getName());
    }

    return new Join(featurestoreFacade, query.getLeftFg(), query.getLeftFgAs(), query.getRightFg(),
        query.getRightFgAs(), joinFeatures, query.getType());
  }

  public String generateSQL(List<FeatureDTO> selectedFeatures, List<Join> joins) {
    SqlNodeList selectList = new SqlNodeList(SqlParserPos.ZERO);
    for (FeatureDTO f : selectedFeatures) {
      selectList.add(new SqlIdentifier(Arrays.asList(f.getFeaturegroup(), f.getName()), SqlParserPos.ZERO));
    }

    // TODO(Fabio): account for recursive
    SqlNode joinNode = joins.get(0).getJoinNode();

    SqlSelect select = new SqlSelect(SqlParserPos.ZERO, null, selectList, joinNode,
        null, null, null, null, null, null, null);
    return select.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
  }
}
