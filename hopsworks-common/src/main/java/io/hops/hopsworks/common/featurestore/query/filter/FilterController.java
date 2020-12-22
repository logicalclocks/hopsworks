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

package io.hops.hopsworks.common.featurestore.query.filter;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FilterController {
  
  @EJB
  private ConstructorController constructorController;
  
  public FilterController() {}
  
  // for testing
  public FilterController(ConstructorController constructorController) {
    this.constructorController = constructorController;
  }
  
  public FilterLogic convertFilterLogic(FilterLogicDTO filterLogicDTO, Map<Integer, Featuregroup> fgLookup,
                                        Map<Integer, List<Feature>> availableFeatureLookup)
      throws FeaturestoreException {
    
    FilterLogic filterLogic = new FilterLogic(filterLogicDTO.getType());
    validateFilterLogicDTO(filterLogicDTO);
    
    if (filterLogicDTO.getLeftFilter() != null) {
      filterLogic.setLeftFilter(convertFilter(filterLogicDTO.getLeftFilter(), fgLookup, availableFeatureLookup));
    }
    if (filterLogicDTO.getRightFilter() != null) {
      filterLogic.setRightFilter(convertFilter(filterLogicDTO.getRightFilter(), fgLookup, availableFeatureLookup));
    }
    if (filterLogicDTO.getLeftLogic() != null) {
      filterLogic.setLeftLogic(convertFilterLogic(filterLogicDTO.getLeftLogic(), fgLookup, availableFeatureLookup));
    }
    if (filterLogicDTO.getRightLogic() != null) {
      filterLogic.setRightLogic(convertFilterLogic(filterLogicDTO.getRightLogic(), fgLookup, availableFeatureLookup));
    }
    return filterLogic;
  }
  
  void validateFilterLogicDTO(FilterLogicDTO filterLogicDTO) throws FeaturestoreException {
    // each side of the tree can either only have a filter or another filterLogic object
    // case 1: both filter and logic are set on left side
    // case 2: both filter and logic are set on right side
    // case 3: Single filters are still wrapped with a FilterLogic of type SINGLE to simplify handling of the DTO
    // case 3.1: the single filter is assumed to be in the leftFilter field, in this case it's missing
    // case 3.2: any of the other three fields is set
    if ((filterLogicDTO.getLeftFilter() != null && filterLogicDTO.getLeftLogic() != null)
      || (filterLogicDTO.getRightFilter() != null && filterLogicDTO.getRightLogic() != null)
      || (filterLogicDTO.getType() == SqlFilterLogic.SINGLE &&
      (filterLogicDTO.getLeftFilter() == null ||
        filterLogicDTO.getLeftLogic() != null ||
        filterLogicDTO.getRightFilter() != null ||
        filterLogicDTO.getRightLogic() != null))) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FILTER_ARGUMENTS, Level.FINE, "The " +
        "provided filters for the Query are malformed, please do not access private attributes, contact maintainers " +
        "with reproducible example.");
    }
  }
  
  Filter convertFilter(FilterDTO filterDTO, Map<Integer, Featuregroup> fgLookup,
    Map<Integer, List<Feature>> availableFeatureLookup) throws FeaturestoreException {
    return new Filter(findFilteredFeature(filterDTO.getFeature(), fgLookup, availableFeatureLookup),
      filterDTO.getCondition(), filterDTO.getValue());
  }
  
  Feature findFilteredFeature(FeatureGroupFeatureDTO featureDTO, Map<Integer, Featuregroup> fgLookup,
    Map<Integer, List<Feature>> availableFeatureLookup) throws FeaturestoreException {
    Optional<Feature> feature = Optional.empty();
    
    if (featureDTO.getFeatureGroupId() != null) {
      // feature group is specified by user or api
      feature = availableFeatureLookup.get(featureDTO.getFeatureGroupId()).stream()
        .filter(af -> af.getName().equals(featureDTO.getName()))
        .findFirst();
    } else {
      // check all feature groups
      for (Map.Entry<Integer, List<Feature>> pair : availableFeatureLookup.entrySet()) {
        feature = pair.getValue().stream()
          .filter(af -> af.getName().equals(featureDTO.getName()))
          .findFirst();
        if (feature.isPresent()) {
          break;
        }
      }
    }
    
    return feature.orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST,
      Level.FINE, "Filtered feature: `" + featureDTO.getName() + "` not found in any of the feature groups."));
  }
  
  public SqlNode generateFilterLogicNode(FilterLogic filterLogic, boolean online) {
    if (filterLogic.getType() == SqlFilterLogic.SINGLE) {
      return generateFilterNode(filterLogic.getLeftFilter(), online);
    } else {
      SqlNode leftNode = filterLogic.getLeftFilter() != null ? generateFilterNode(filterLogic.getLeftFilter(), online) :
        generateFilterLogicNode(filterLogic.getLeftLogic(), online);
      SqlNode rightNode = filterLogic.getRightFilter() != null ?
        generateFilterNode(filterLogic.getRightFilter(), online) :
        generateFilterLogicNode(filterLogic.getRightLogic(), online);
      return filterLogic.getType().operator.createCall(SqlParserPos.ZERO, leftNode, rightNode);
    }
  }
  
  SqlNode generateFilterNode(Filter filter, boolean online) {
    SqlNode feature;
    if (filter.getFeature().getDefaultValue() == null || online) {
      feature = new SqlIdentifier(Arrays.asList("`" + filter.getFeature().getFgAlias() + "`",
        "`" + filter.getFeature().getName() + "`"), SqlParserPos.ZERO);
    } else {
      feature = constructorController.caseWhenDefault(filter.getFeature());
    }
    
    SqlNode filterValue;
    if (filter.getFeature().getType().equalsIgnoreCase("string")) {
      filterValue = SqlLiteral.createCharString(filter.getValue(), SqlParserPos.ZERO);
    } else {
      filterValue = new SqlIdentifier(filter.getValue(), SqlParserPos.ZERO);
    }
    
    return filter.getCondition().operator.createCall(SqlParserPos.ZERO, feature, filterValue);
  }

  public SqlNode buildFilterNode(Query baseQuery, Query query, int i, boolean online) {
    if (i < 0 && query.getFilter() != null) {
      // No more joins to read build the node for the query itself.
      return generateFilterLogicNode(query.getFilter(), online);
    } else if (i >= 0) {
      SqlNode filter = buildFilterNode(baseQuery, baseQuery.getJoins().get(i).getRightQuery(), i - 1, online);
      if (filter != null && query.getFilter() != null) {
        return SqlStdOperatorTable.AND.createCall(SqlParserPos.ZERO, generateFilterLogicNode(query.getFilter(), online),
          filter);
      } else if (filter != null) {
        return filter;
      } else if (query.getFilter() != null) {
        return generateFilterLogicNode(query.getFilter(), online);
      }
    }
    return null;
  }
}
