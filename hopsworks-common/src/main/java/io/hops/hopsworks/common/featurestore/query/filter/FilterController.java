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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlFilterLogic;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.DateString;
import org.apache.calcite.util.TimestampString;
import org.json.JSONArray;
import org.json.JSONTokener;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FilterController {

  @EJB
  private ConstructorController constructorController;
  @EJB
  private QueryController queryController;
  @EJB
  private FeaturegroupController featuregroupController;
  private ObjectMapper objectMapper = new ObjectMapper();
  private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private DateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

  public FilterController() {
  }

  // for testing
  public FilterController(ConstructorController constructorController) {
    this.constructorController = constructorController;
  }

  public FilterLogic convertFilterLogic(Project project, Users users, Query query, FilterLogicDTO filterLogicDTO)
      throws FeaturestoreException {
    Map<Integer, Featuregroup> fgLookup = queryController.getFeatureGroups(query)
        .stream()
        .collect(Collectors.toMap(Featuregroup::getId, Function.identity()));
    Map<Integer, List<Feature>> availableFeatureLookup = Maps.newHashMap();
    for (Featuregroup featuregroup : fgLookup.values()) {
      availableFeatureLookup.put(
          featuregroup.getId(),
          featuregroupController.getFeatures(featuregroup, project, users)
          .stream()
          .map(f -> new Feature(f, featuregroup))
          .collect(Collectors.toList())
      );
    }
    return convertFilterLogic(filterLogicDTO, availableFeatureLookup);
  }

  public FilterLogic convertFilterLogic(FilterLogicDTO filterLogicDTO,
      Map<Integer, List<Feature>> availableFeatureLookup)
      throws FeaturestoreException {

    FilterLogic filterLogic = new FilterLogic(filterLogicDTO.getType());
    validateFilterLogicDTO(filterLogicDTO);

    if (filterLogicDTO.getLeftFilter() != null) {
      filterLogic.setLeftFilter(convertFilter(filterLogicDTO.getLeftFilter(), availableFeatureLookup));
    }
    if (filterLogicDTO.getRightFilter() != null) {
      filterLogic.setRightFilter(convertFilter(filterLogicDTO.getRightFilter(), availableFeatureLookup));
    }
    if (filterLogicDTO.getLeftLogic() != null) {
      filterLogic.setLeftLogic(convertFilterLogic(filterLogicDTO.getLeftLogic(), availableFeatureLookup));
    }
    if (filterLogicDTO.getRightLogic() != null) {
      filterLogic.setRightLogic(convertFilterLogic(filterLogicDTO.getRightLogic(), availableFeatureLookup));
    }
    return filterLogic;
  }

  public String convertToEventTimeFeatureValue(Feature feature, Date date) throws FeaturestoreException {
    String timeType = feature.getType();
    if ("date".equals(timeType)) {
      return dateFormat.format(date);
    } else if ("timestamp".equals(timeType)) {
      return timestampFormat.format(date);
    } else {
      return String.valueOf(date.getTime());
    }
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
          "provided filters for the Query are malformed, please do not access private attributes, contact maintainers "
          + "with reproducible example.");
    }
  }

  Filter convertFilter(FilterDTO filterDTO,
      Map<Integer, List<Feature>> availableFeatureLookup) throws FeaturestoreException {
    return new Filter(Arrays.asList(findFilteredFeature(filterDTO.getFeature(), availableFeatureLookup)),
        filterDTO.getCondition(), convertFilterValue(filterDTO.getValue(), availableFeatureLookup));
  }

  FilterValue convertFilterValue(String value,
      Map<Integer, List<Feature>> availableFeatureLookup) throws FeaturestoreException {

    // can be null value when it's not a feature object
    if (value == null) {
      return new FilterValue(null);
    }

    try {
      FeatureGroupFeatureDTO featureDto = objectMapper.readValue(value, FeatureGroupFeatureDTO.class);
      // serialized value can be "null"
      if (featureDto == null) {
        return new FilterValue("null");
      }
      Feature feature = findFilteredFeature(featureDto, availableFeatureLookup);
      return new FilterValue(feature.getFeatureGroup().getId(), feature.getFgAlias(), feature.getName());
    } catch (IOException e) {
      return new FilterValue(value);
    }
  }

  Feature findFilteredFeature(FeatureGroupFeatureDTO featureDTO,
      Map<Integer, List<Feature>> availableFeatureLookup) throws FeaturestoreException {
    Optional<Feature> feature = Optional.empty();

    if (featureDTO.getFeatureGroupId() != null) {
      if (!availableFeatureLookup.containsKey(featureDTO.getFeatureGroupId())) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST,
            Level.FINE,
            "Filtered feature : '" + featureDTO.getName() + "' not found in main query.");
      }
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
    return generateFilterLogicNode(filterLogic, online, true);
  }
  
  public SqlNode generateFilterLogicNode(FilterLogic filterLogic, boolean online, boolean checkForInNull) {
    if (filterLogic.getType() == SqlFilterLogic.SINGLE) {
      if (filterLogic.getLeftFilter().getFeatures().size() > 1) {
        // NOTE: this is currently only the case when the Filter is coming from the PreparedStatementBuilder
        // if we want to support user defined filters over multiple features in the future, this needs to be changed,
        // as the ? placeholder is hard coded in `generateFilterNodeList`
        return generateFilterNodeList(filterLogic.getLeftFilter(), online);
      } else {
        return generateFilterNode(filterLogic.getLeftFilter(), online, checkForInNull);
      }
    } else {
      SqlNode leftNode = filterLogic.getLeftFilter() != null ?
        generateFilterNode(filterLogic.getLeftFilter(), online, checkForInNull) :
          generateFilterLogicNode(filterLogic.getLeftLogic(), online);
      SqlNode rightNode = filterLogic.getRightFilter() != null ?
          generateFilterNode(filterLogic.getRightFilter(), online, checkForInNull) :
          generateFilterLogicNode(filterLogic.getRightLogic(), online);
      return filterLogic.getType().operator.createCall(SqlParserPos.ZERO, leftNode, rightNode);
    }
  }
  
  public SqlNode generateFilterNode(Filter filter, boolean online) {
    return generateFilterNode(filter, online, true);
  }

  public SqlNode generateFilterNode(Filter filter, boolean online, boolean checkForInNull) {
    SqlNode sqlNode;
    Feature feature = filter.getFeatures().get(0);
    if (feature.getDefaultValue() == null) {
      if (feature.getFgAlias(false) != null) {
        sqlNode = new SqlIdentifier(Arrays.asList("`" + feature.getFgAlias(false) + "`",
            "`" + feature.getName() + "`"), SqlParserPos.ZERO);
      } else {
        sqlNode = new SqlIdentifier("`" + feature.getName() + "`", SqlParserPos.ZERO);
      }
    } else {
      sqlNode = constructorController.caseWhenDefault(feature);
    }
    
    SqlNode filterValue;
    Object json = null;
    if (filter.getValue().makeSqlValue() != null) {
      json = new JSONTokener(filter.getValue().makeSqlValue()).nextValue();
    }
    if (json instanceof JSONArray) {
      // Array
      List<SqlNode> operandList = new ArrayList<>();
      // Using gson, as casting "json" to JSONArray results in exception
      for (Object item : new Gson().fromJson(filter.getValue().getValue(), List.class)) {
        // if item is null skip generating regular IN statement, but instead wrap in extra conjunction
        if (item == null && checkForInNull) {
          FilterLogic wrappedIn = new FilterLogic(SqlFilterLogic.OR, filter, new Filter(filter.getFeatures(),
            SqlCondition.IS, (String) null));
          return generateFilterLogicNode(wrappedIn, online, false);
        }
        // Item would not be json of FeatureDTO object, so it is ok to create FilterValue from string value of item
        // skip null values since they are handled by adding the OR construct
        if (item != null) {
          operandList.add(getSQLNode(filter.getFeatures().get(0).getType(), new FilterValue(item.toString())));
        }
      }
      filterValue = new SqlNodeList(operandList, SqlParserPos.ZERO);
    } else {
      // Value
      filterValue = getSQLNode(feature.getType(), filter.getValue());
    }

    if (Arrays.asList(SqlCondition.EQUALS, SqlCondition.IS).contains(filter.getCondition())
      && filter.getValue().getValue() == null) {
      return SqlCondition.IS.operator.createCall(SqlParserPos.ZERO, sqlNode);
    }
    return filter.getCondition().operator.createCall(SqlParserPos.ZERO, sqlNode, filterValue);
  }

  protected SqlNode getSQLNode(String type, String value) {
    return getSQLNode(type, new FilterValue(value));
  }

  protected SqlNode getSQLNode(String type, FilterValue value) {
    if (value.isFeatureValue()) {
      return new SqlIdentifier(value.makeSqlValue(), SqlParserPos.ZERO);
    }
    if (value.makeSqlValue() == null) {
      return SqlLiteral.createNull(SqlParserPos.ZERO);
    }
    if (type.equalsIgnoreCase("string")) {
      return SqlLiteral.createCharString(value.makeSqlValue(), SqlParserPos.ZERO);
    } else if (type.equalsIgnoreCase("date")) {
      return SqlLiteral.createDate(new DateString(value.makeSqlValue()), SqlParserPos.ZERO);
    } else if (type.equalsIgnoreCase("timestamp")) {
      // precision 3 should be milliseconds since we don't support more precision in parquet files
      return SqlLiteral.createTimestamp(new TimestampString(value.makeSqlValue()), 3, SqlParserPos.ZERO);
    } else {
      return new SqlIdentifier(value.makeSqlValue(), SqlParserPos.ZERO);
    }
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

  public SqlNode generateFilterNodeList(Filter filter, boolean online) {
    SqlNodeList operandList = new SqlNodeList(SqlParserPos.ZERO);
    for (Feature feature : filter.getFeatures()) {
      if (feature.getDefaultValue() == null) {
        if (feature.getFgAlias(false) != null) {
          operandList.add(new SqlIdentifier(Arrays.asList("`" + feature.getFgAlias(false) + "`",
              "`" + feature.getName() + "`"), SqlParserPos.ZERO));
        } else {
          operandList.add(new SqlIdentifier("`" + feature.getName() + "`", SqlParserPos.ZERO));
        }
      } else {
        operandList.add(constructorController.caseWhenDefault(feature));
      }
    }

    return SqlStdOperatorTable.IN.createCall(SqlParserPos.ZERO, operandList,
        new SqlIdentifier("?", SqlParserPos.ZERO));
  }
}
