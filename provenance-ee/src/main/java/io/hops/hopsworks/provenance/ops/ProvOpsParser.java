/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import com.lambdista.util.Try;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.elastic.BasicElasticHit;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class is used to translate Rest Endpoint query params into Elastic field names (or Filters to be more accurate)
 * This uses the Ops Prov indices for elastic field names.
 */
public class ProvOpsParser {
  
  public interface Field extends ProvParser.Field {
  }

  public enum Fields implements ProvParser.ElasticField {
    INODE_OPERATION,
    TIMESTAMP,
    R_TIMESTAMP,
    LOGICAL_TIME;
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
    ML_TYPE(ProvParser.Fields.ML_TYPE, new DocTypeValParser()),
    ML_ID(ProvParser.Fields.ML_ID, new ProvParser.StringValParser()),
    FILE_OPERATION(ProvOpsParser.Fields.INODE_OPERATION, new FileOpValParser()),
    PARTITION_ID(ProvParser.AuxField.PARTITION_ID, new ProvParser.LongValParser()),
    TIMESTAMP(ProvOpsParser.Fields.TIMESTAMP, new ProvParser.LongValParser()),
    LOGICAL_TIME(ProvOpsParser.Fields.LOGICAL_TIME, new ProvParser.IntValParser()),
    R_TIMESTAMP(ProvOpsParser.Fields.R_TIMESTAMP, new ProvParser.StringValParser()),
    ENTRY_TYPE(ProvParser.Fields.ENTRY_TYPE, new ProvParser.StringValParser());
    
    ProvParser.ElasticField elasticField;
    ProvParser.ValParser valParser;
    
    FieldsP(ProvParser.ElasticField elasticField, ProvParser.ValParser valParser) {
      this.elasticField = elasticField;
      this.valParser = valParser;
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
      return valParser;
    }
  }
  
  /** Rest Endpoint Fields - < field, parser, filter type > */
  public enum FieldsPF implements Field {
    FILE_NAME_LIKE(FieldsP.FILE_NAME, ProvParser.FilterType.LIKE),
    TIMESTAMP_LT(FieldsP.TIMESTAMP, ProvParser.FilterType.RANGE_LT),
    TIMESTAMP_LTE(FieldsP.TIMESTAMP, ProvParser.FilterType.RANGE_LTE),
    TIMESTAMP_GT(FieldsP.TIMESTAMP, ProvParser.FilterType.RANGE_GT),
    TIMESTAMP_GTE(FieldsP.TIMESTAMP, ProvParser.FilterType.RANGE_GTE);
    
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
  
  public static class FileOpValParser implements ProvParser.ValParser<Provenance.FileOps> {
    
    @Override
    public Provenance.FileOps apply(Object o) throws ProvenanceException {
      if(o instanceof String) {
        try {
          return Provenance.FileOps.valueOf((String)o);
        } catch (NullPointerException | IllegalArgumentException e) {
          String msg = "expected string-ified version of File Ops found " + o.getClass();
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg, msg, e);
        }
      } else {
        String msg = "expected string-ified version of File Ops found " + o.getClass();
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
      }
    }
  }
  
  private static <R> R onlyAllowed() throws ProvenanceException {
    String msg = "allowed fields:" + EnumSet.allOf(FieldsP.class) + " or " + EnumSet.allOf(FieldsPF.class);
    throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
  }
  
  public static Field extractField(String val)
    throws ProvenanceException {
    try {
      return FieldsP.valueOf(val.toUpperCase());
    } catch (NullPointerException | IllegalArgumentException e) {
      //try next
    }
    try {
      return FieldsPF.valueOf(val.toUpperCase());
    } catch(NullPointerException | IllegalArgumentException ee) {
      //try next
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
    Object val = field.filterValParser().apply(rawVal);
    return Pair.with(field, val);
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
  
  public static class DocTypeValParser implements ProvParser.ValParser<ProvParser.DocSubType> {
    @Override
    public ProvParser.DocSubType apply(Object o) throws ProvenanceException {
      try {
        if(o instanceof String) {
          return ProvParser.DocSubType.valueOf((String)o);
        } else {
          String msg = "expected string-ified version of DocSubType found " + o.getClass();
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
        }
      } catch (NullPointerException | IllegalArgumentException e) {
        String msg = "expected string-ified version of DocSubType found " + o.getClass();
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg, msg, e);
      }
    }
  }
  
  public static ProvOpsDTO instance(BasicElasticHit hit) throws ProvenanceException {
    ProvOpsDTO result = new ProvOpsDTO();
    result.setId(hit.getId());
    result.setScore(Float.isNaN(hit.getScore()) ? 0 : hit.getScore());
    return instance(result, new HashMap<>(hit.getSource()));
  }
  
  public static Try<ProvOpsDTO> tryInstance(BasicElasticHit hit) {
    return Try.apply(() -> instance(hit));
  }
  
  private static ProvOpsDTO instance(ProvOpsDTO result, Map<String, Object> auxMap)
    throws ProvenanceException {
    try {
      result.setProjectInodeId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.PROJECT_I_ID));
      result.setDatasetInodeId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.DATASET_I_ID));
      result.setInodeId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.FILE_I_ID));
      result.setAppId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.APP_ID));
      result.setUserId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.USER_ID));
      result.setInodeName(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.FILE_NAME));
      result.setInodeOperation(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.FILE_OPERATION));
      result.setTimestamp(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.TIMESTAMP));
      result.setParentInodeId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.PARENT_I_ID));
      result.setPartitionId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.PARTITION_ID));
      result.setProjectName(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.PROJECT_NAME));
      result.setMlId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.ML_ID));
      result.setDocSubType(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.ML_TYPE));
      result.setLogicalTime(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.LOGICAL_TIME));
      result.setReadableTimestamp(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.R_TIMESTAMP));
      ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.ENTRY_TYPE);
      Map<String, String> xattrs = ProvHelper.extractElasticField(auxMap,
        ProvParser.XAttrField.XATTR_PROV, ProvHelper.asXAttrMap(), true);
      if(xattrs != null && xattrs.size() == 1) {
        Map.Entry<String, String> e = xattrs.entrySet().iterator().next();
        result.setXattrName(e.getKey());
        result.setXattrVal(e.getValue());
      }
    } catch(ClassCastException e) {
      String msg = "mistmatch between DTO class and ProvOParser field types (elastic)";
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING, msg, msg, e);
    }
    return result;
  }
  
  public static ProvOpsDTO mlInstance(BasicElasticHit hit) throws ProvenanceException {
    Map<String, Object> auxMap = new HashMap<>(hit.getSource());
    ProvOpsDTO result = new ProvOpsDTO();
    try {
      //we are only interested in certain fields
      result.setProjectInodeId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.PROJECT_I_ID));
      result.setDatasetInodeId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.DATASET_I_ID));
      result.setAppId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.APP_ID));
      result.setUserId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.USER_ID));
      result.setInodeOperation(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.FILE_OPERATION));
      result.setTimestamp(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.TIMESTAMP));
      result.setProjectName(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.PROJECT_NAME));
      result.setMlId(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.ML_ID));
      result.setDocSubType(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.ML_TYPE));
      result.setReadableTimestamp(ProvHelper.extractElasticField(auxMap, ProvOpsParser.FieldsP.R_TIMESTAMP));
    } catch(ClassCastException e) {
      String msg = "mistmatch between DTO class and ProvOParser field types (elastic)";
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING, msg, msg, e);
    }
    return result;
  }
  
  public static Try<ProvOpsDTO> tryMLInstance(BasicElasticHit hit) {
    return Try.apply(() -> mlInstance(hit));
  }
}