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
package io.hops.hopsworks.common.provenance.app;

import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.javatuples.Pair;

import java.util.EnumSet;
import java.util.logging.Level;

public class ProvAParser {
  
  public enum BaseField implements ProvParser.ElasticField {
    APP_STATE,
    APP_ID,
    TIMESTAMP,
    APP_NAME,
    APP_USER,
    READABLE_TIMESTAMP;
    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
  
  public enum Field implements ProvParser.Field {
    APP_STATE(BaseField.APP_STATE, new AppStateValParser()),
    APP_ID(BaseField.APP_ID, new ProvParser.StringValParser()),
    TIMESTAMP(BaseField.TIMESTAMP, new ProvParser.LongValParser()),
    APP_NAME(BaseField.APP_NAME, new ProvParser.StringValParser()),
    APP_USER(BaseField.APP_USER, new ProvParser.StringValParser()),
    R_TIMESTAMP(BaseField.READABLE_TIMESTAMP, new ProvParser.StringValParser());
    
    public final BaseField elasticField;
    public final ProvParser.ValParser valParser;
    
    Field(BaseField elasticField, ProvParser.ValParser valParser) {
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
  
  public static class AppStateValParser implements ProvParser.ValParser<Provenance.AppState> {
    @Override
    public Provenance.AppState apply(Object o) throws ProvenanceException {
      if(o instanceof String) {
        try {
          return Provenance.AppState.valueOf((String)o);
        } catch (NullPointerException | IllegalArgumentException e) {
          String msg = "expected string-ified version of AppState - found: " + o;
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg, msg, e);
        }
      } else {
        String msg = "expected string-ified version of AppState - found " + o.getClass();
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
      }
    }
  }
  
  public static Pair<ProvParser.Field, Object> extractFilter(String param) throws ProvenanceException {
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
    ProvParser.Field field = extractField(rawFilter);
    Object val = field.filterValParser().apply(rawVal);
    return Pair.with(field, val);
  }
  
  public static ProvParser.Field extractField(String val) throws ProvenanceException {
    try {
      return Field.valueOf(val.toUpperCase());
    } catch(NullPointerException | IllegalArgumentException e) {
      StringBuilder supported = new StringBuilder();
      supported.append(EnumSet.allOf(Field.class));
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
        "sort param" + val + " not supported - supported:" + supported,
        "exception extracting SortBy param", e);
    }
  }
}
