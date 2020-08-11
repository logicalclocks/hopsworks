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
package io.hops.hopsworks.common.provenance.state;

import com.lambdista.util.Try;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.elastic.BasicElasticHit;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to translate Rest Endpoint query params into Elastic field names (or Filters to be more accurate)
 * This uses the State Prov indices for elastic field names.
 */
public class ProvStateParser {
  private final static Logger LOGGER = Logger.getLogger(ProvStateParser.class.getName());
  
  public interface Field extends ProvParser.Field {
  }
  
  public enum Fields implements ProvParser.ElasticField {
    CREATE_TIMESTAMP,
    R_CREATE_TIMESTAMP;
    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
  
  /** Rest Endpoint Fields - < field, parser >
   * Without an actual filter defined, it defaults to EXACT */
  public enum FieldsP implements Field {
    PROJECT_I_ID(ProvParser.Fields.PROJECT_I_ID, new ProvParser.LongValParser()),
    DATASET_I_ID(ProvParser.Fields.DATASET_I_ID, new ProvParser.LongValParser()),
    PARENT_I_ID(ProvParser.Fields.PARENT_I_ID, new ProvParser.LongValParser()),
    FILE_I_ID(ProvParser.Fields.INODE_ID, new ProvParser.LongValParser()),
    FILE_NAME(ProvParser.Fields.INODE_NAME, new ProvParser.StringValParser()),
    PROJECT_NAME(ProvParser.AuxField.PROJECT_NAME, new ProvParser.StringValParser()),
    USER_ID(ProvParser.Fields.USER_ID, new ProvParser.IntValParser()),
    APP_ID(ProvParser.Fields.APP_ID, new ProvParser.StringValParser()),
    ML_TYPE(ProvParser.Fields.ML_TYPE, new ProvParser.MLTypeValParser()),
    ML_ID(ProvParser.Fields.ML_ID, new ProvParser.StringValParser()),
    PARTITION_ID(ProvParser.AuxField.PARTITION_ID, new ProvParser.LongValParser()),
    CREATE_TIMESTAMP(ProvStateParser.Fields.CREATE_TIMESTAMP, new ProvParser.LongValParser()),
    R_CREATE_TIMESTAMP(ProvStateParser.Fields.R_CREATE_TIMESTAMP, new ProvParser.StringValParser()),
    ENTRY_TYPE(ProvParser.Fields.ENTRY_TYPE, new ProvParser.StringValParser());
  
    ProvParser.ElasticField elasticField;
    ProvParser.ValParser<?> filterValParser;
    
    FieldsP(ProvParser.ElasticField elasticField, ProvParser.ValParser<?> filterValParser) {
      this.elasticField = elasticField;
      this.filterValParser = filterValParser;
    }
    
    @Override
    public String elasticFieldName() {
      return elasticField.toString().toLowerCase();
    }
    
    @Override
    public String queryFieldName() {
      return name().toLowerCase();
    }
    
    @Override
    public ProvParser.FilterType filterType() {
      return ProvParser.FilterType.EXACT;
    }
    
    @Override
    public ProvParser.ValParser filterValParser() {
      return filterValParser;
    }
  }
  
  /** Rest Endpoint Fields - < field, parser, filter type > */
  public enum FieldsPF implements Field {
    FILE_NAME_LIKE(FieldsP.FILE_NAME, ProvParser.FilterType.LIKE),
    CREATE_TIMESTAMP_LT(FieldsP.CREATE_TIMESTAMP, ProvParser.FilterType.RANGE_LT),
    CREATE_TIMESTAMP_LTE(FieldsP.CREATE_TIMESTAMP, ProvParser.FilterType.RANGE_LTE),
    CREATE_TIMESTAMP_GT(FieldsP.CREATE_TIMESTAMP, ProvParser.FilterType.RANGE_GT),
    CREATE_TIMESTAMP_GTE(FieldsP.CREATE_TIMESTAMP, ProvParser.FilterType.RANGE_GTE);
    
    FieldsP base;
    ProvParser.FilterType filterType;
    
    FieldsPF(FieldsP base, ProvParser.FilterType filterType) {
      this.base = base;
      this.filterType = filterType;
    }
    
    @Override
    public String elasticFieldName() {
      return base.elasticFieldName();
    }
    
    @Override
    public String queryFieldName() {
      return base.elasticFieldName();
    }
    
    @Override
    public ProvParser.FilterType filterType() {
      return filterType;
    }
    
    @Override
    public ProvParser.ValParser filterValParser() {
      return base.filterValParser();
    }
  }
  
  private static <V> V onlyAllowed() throws ProvenanceException {
    String msg = "allowed fields:" + EnumSet.allOf(ProvStateParser.FieldsP.class) +
      " or " + EnumSet.allOf(ProvStateParser.FieldsPF.class);
    throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
  }
  
  public static Field extractField(String val) throws ProvenanceException {
    try {
      return FieldsP.valueOf(val.toUpperCase());
    } catch(NullPointerException | IllegalArgumentException e) {
    }
    try {
      return FieldsPF.valueOf(val.toUpperCase());
    } catch(NullPointerException | IllegalArgumentException e) {
    }
    return onlyAllowed();
  }
  
  public static Pair<Field, Object> extractFilter(String param) throws ProvenanceException {
    String rawFilter;
    String rawVal;
    if (param.contains(":")) {
      int aux = param.indexOf(':');
      rawFilter = param.substring(0, aux);
      rawVal = param.substring(aux+1);
    } else {
      rawFilter = param;
      rawVal = "true";
    }
    Field field = extractField(rawFilter);
    Object parsedVal = field.filterValParser().apply(rawVal);
    return Pair.with(field, parsedVal);
  }
  
  public static Pair<ProvParser.Field, SortOrder> extractSort(String param) throws ProvenanceException {
    String rawSortField;
    String rawSortOrder;
    if (param.contains(":")) {
      int aux = param.indexOf(':');
      rawSortField = param.substring(0, aux);
      rawSortOrder = param.substring(aux+1);
    } else {
      rawSortField = param;
      rawSortOrder = "ASC";
    }
    Field field = extractField(rawSortField);
    SortOrder sortOrder = ProvParser.extractSortOrder(rawSortOrder);
    return Pair.with(field, sortOrder);
  }
  
  public static ProvStateDTO instance(BasicElasticHit hit) throws ProvenanceException {
    ProvStateDTO result = new ProvStateDTO();
    result.setId(hit.getId());
    result.setScore(Float.isNaN(hit.getScore()) ? 0 : hit.getScore());
    return instance(result, new HashMap<>(hit.getSource()));
  }
  
  public static Try<ProvStateDTO> tryInstance(BasicElasticHit hit) {
    return Try.apply(() -> instance(hit));
  }
  
  private static ProvStateDTO instance(ProvStateDTO result, Map<String, Object> auxMap)
    throws ProvenanceException {
    try {
      result.setProjectInodeId(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.PROJECT_I_ID));
      result.setInodeId(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.FILE_I_ID));
      result.setAppId(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.APP_ID));
      result.setUserId(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.USER_ID));
      result.setInodeName(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.FILE_NAME));
      result.setCreateTime(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.CREATE_TIMESTAMP));
      result.setMlType(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.ML_TYPE));
      result.setMlId(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.ML_ID));
      result.setDatasetInodeId(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.DATASET_I_ID));
      result.setParentInodeId(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.PARENT_I_ID));
      result.setPartitionId(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.PARTITION_ID));
      result.setProjectName(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.PROJECT_NAME));
      result.setReadableCreateTime(ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.R_CREATE_TIMESTAMP));
      ProvHelper.extractElasticField(auxMap, ProvStateParser.FieldsP.ENTRY_TYPE);
      result.setXattrs(ProvHelper.extractElasticField(auxMap,
        ProvParser.XAttrField.XATTR_PROV, ProvHelper.asXAttrMap(), true));
      if (result.getXattrs() != null && result.getXattrs().containsKey(ProvStateParser.FieldsP.APP_ID.toString())) {
        //update if we have a value - useful if tls is disabled and out appId is set to none
        result.setAppId(ProvHelper.extractElasticField(result.getXattrs().get(ProvParser.Fields.APP_ID.toString())));
      }
      if(!auxMap.isEmpty()) {
        LOGGER.log(Level.FINE, "fields:{0} not managed in file state return", auxMap.keySet());
      }
    } catch(ClassCastException e) {
      String msg = "mismatch between DTO class and ProvSParser field types (elastic)";
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING, msg, msg, e);
    }
    return result;
  }
}
