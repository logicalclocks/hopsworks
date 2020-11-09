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
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class is used to translate Rest Endpoint query params into Elastic field names (or Filters to be more accurate)
 * This uses the State and Ops Prov indices for elastic field names.
 */
public class ProvLinks {
  public interface Field extends ProvParser.Field{
  }
  
  /**Only for Internal use
   * Rest Endpoint Fields - < field, parser >
   * Without an actual filter defined, it defaults to EXACT */
  private enum IntFieldsP implements Field {
    ML_TYPE(ProvParser.Fields.ML_TYPE, new MLLinksTypeValParser());
    
    ProvParser.ElasticField elasticField;
    ProvParser.ValParser valParser;
  
    IntFieldsP(ProvParser.ElasticField elasticField, ProvParser.ValParser valParser) {
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
    APP_ID(ProvOps.FieldsP.APP_ID, ProvParser.FilterType.EXACT),
    IN_ARTIFACT(ProvOps.FieldsP.ML_ID, ProvParser.FilterType.EXACT),
    IN_TYPE(IntFieldsP.ML_TYPE, ProvParser.FilterType.EXACT),
    OUT_ARTIFACT(ProvOps.FieldsP.ML_ID, ProvParser.FilterType.EXACT),
    OUT_TYPE(IntFieldsP.ML_TYPE, ProvParser.FilterType.EXACT),
    ONLY_APPS(ProvOps.FieldsP.APP_ID, ProvParser.FilterType.NOT);
    
    ProvParser.Field base;
    ProvParser.FilterType filterType;
  
    FieldsPF(ProvParser.Field base, ProvParser.FilterType filterType) {
      this.base = base;
      this.filterType = filterType;
    }
    
    @Override
    public String elasticFieldName() {
      return base.elasticFieldName();
    }
    
    @Override
    public String queryFieldName() {
      return name().toLowerCase();
    }
    
    @Override
    public ProvParser.FilterType filterType() {
      return filterType;
    }
    
    @Override
    public ProvParser.ValParser<?> filterValParser() {
      return base.filterValParser();
    }
  }
  
  private static <R> R onlyAllowed() throws ProvenanceException {
    String msg = "allowed fields:" + EnumSet.allOf(FieldsPF.class);
    throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
  }
  
  public static Field extractField(String val) throws ProvenanceException {
    try {
      return FieldsPF.valueOf(val.toUpperCase());
    } catch (NullPointerException | IllegalArgumentException e) {
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
  
  /**
   * we double check that user provided fields are supported
   */
  public static void filterBySanityCheck(Map<ProvParser.Field, ProvParser.FilterVal> filterBy,
    Field newField) throws ProvenanceException {
    extractField(newField.queryFieldName());
    if((filterBy.containsKey(FieldsPF.IN_ARTIFACT) && newField.equals(FieldsPF.OUT_ARTIFACT)) ||
      (filterBy.containsKey(FieldsPF.OUT_ARTIFACT) && newField.equals(FieldsPF.IN_ARTIFACT))) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
        "filterBy does not support both IN_ARTIFACT and OUT_ARTIFACT at the same time",
        "exception extracting <full prov links> filterBy params");
    }
  }
  
  public static class MLLinksTypeValParser implements ProvParser.ValParser<List<String>> {
    @Override
    public List<String> apply(Object o) throws ProvenanceException {
      try {
        if(o instanceof String) {
          ProvParser.DocSubType val = ProvParser.DocSubType.valueOf((String)o);
          List<String> result = new ArrayList<>();
          switch(val) {
            case FEATURE:
            case TRAINING_DATASET:
            case MODEL:
            case EXPERIMENT:{
              result.add(val.toString().toUpperCase());
              result.add(val.getPart().toString().toUpperCase());
              return result;
            }
            default: {
              String msg =  "supports only:"
                + EnumSet.of(ProvParser.DocSubType.FEATURE, ProvParser.DocSubType.TRAINING_DATASET,
                ProvParser.DocSubType.MODEL, ProvParser.DocSubType.EXPERIMENT);
              throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
            }
          }
        } else {
          String msg = "expected string-ified version of MLType found " + o.getClass();
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
        }
      } catch (NullPointerException | IllegalArgumentException e) {
        String msg = "expected string-ified version of MLType found " + o.getClass();
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg, msg, e);
      }
    }
  }
}
