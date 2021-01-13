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
package io.hops.hopsworks.common.provenance.ops;

import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import java.util.EnumSet;
import java.util.logging.Level;

public class ProvOps {
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
    FILE_OPERATION(Fields.INODE_OPERATION, new FileOpValParser()),
    PARTITION_ID(ProvParser.AuxField.PARTITION_ID, new ProvParser.LongValParser()),
    TIMESTAMP(Fields.TIMESTAMP, new ProvParser.LongValParser()),
    LOGICAL_TIME(Fields.LOGICAL_TIME, new ProvParser.IntValParser()),
    R_TIMESTAMP(Fields.R_TIMESTAMP, new ProvParser.StringValParser()),
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
      if (o instanceof String) {
        try {
          return Provenance.FileOps.valueOf((String) o);
        } catch (NullPointerException | IllegalArgumentException e) {
          String msg = "expected string-ified version of File Ops found " + o.getClass();
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg, msg, e);
        }
      } else if (o instanceof Provenance.FileOps) {
        return (Provenance.FileOps) o;
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
}
