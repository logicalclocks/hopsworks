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
package io.hops.hopsworks.common.provenance.util;

import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ProvHelper {
  
  public static <C> C extractElasticField(Map<String, Object> fields, ProvParser.Field field)
    throws ProvenanceException {
    return extractElasticField(fields, field, false);
  }
  
  public static <C> C extractElasticField(Map<String, Object> fields, ProvParser.Field field, boolean soft)
    throws ProvenanceException {
    Object val = fields.remove(field.elasticFieldName());
    if(val == null) {
      if(soft) {
        return null;
      } else {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.MALFORMED_ENTRY, Level.INFO,
          "problem parsing elastic field:" + field + " - found null");
      }
    }
    try {
      return (C)field.filterValParser().apply(val);
    } catch (ProvenanceException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.MALFORMED_ENTRY, Level.INFO,
        "problem parsing elastic field:" + field, "problem parsing elastic field:" + field, e);
    }
  }
  
  public static <C> C extractElasticField(Map<String, Object> fields, ProvParser.ElasticField field,
    CheckedFunction<Object, C, ProvenanceException> parser, boolean soft) throws ProvenanceException {
    Object val = fields.remove(field.toString());
    if(val == null) {
      if(soft) {
        return null;
      } else {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.MALFORMED_ENTRY, Level.INFO,
          "problem parsing elastic field:" + field + " - found null");
      }
    }
    try {
      return parser.apply(val);
    } catch (ProvenanceException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.MALFORMED_ENTRY, Level.INFO,
        "problem parsing elastic field:" + field, "problem parsing elastic field:" + field, e);
    }
  }
  
  public static <C> C extractElasticField(Object val) throws ProvenanceException {
    if(val == null) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.MALFORMED_ENTRY, Level.INFO,
        "expected String, found null");
    }
    return (C)val;
  }
  
  public static CheckedFunction<Object, Map<String, String>, ProvenanceException> asXAttrMap() {
    return (Object o) -> {
      Map<String, String> result = new HashMap<>();
      Map<Object, Object> xattrsMap;
      try {
        xattrsMap = (Map) o;
      } catch (ClassCastException e) {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.MALFORMED_ENTRY, Level.INFO,
          "prov xattr expected map object (1)", e.getMessage(), e);
      }
      for (Map.Entry<Object, Object> entry : xattrsMap.entrySet()) {
        String xattrKey;
        try {
          xattrKey = (String) entry.getKey();
        } catch (ClassCastException e) {
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.MALFORMED_ENTRY, Level.INFO,
            "prov xattr expected map with string keys", e.getMessage(), e);
        }
        String xattrVal;
        if (entry.getValue() instanceof Map) {
          Map<String, Object> xaMap = (Map) entry.getValue();
          if (xaMap.containsKey("raw")) {
            xattrVal = (String) xaMap.get("raw");
          } else {
            throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.MALFORMED_ENTRY, Level.INFO,
              "parsing prov xattr:" + entry.getKey());
          }
        } else if (entry.getValue() instanceof String) {
          xattrVal = (String) entry.getValue();
        } else {
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.MALFORMED_ENTRY, Level.INFO,
            "prov xattr expected map or string");
        }
        result.put(xattrKey, xattrVal);
      }
      return result;
    };
  }
  
  //**********************Exception handling helper methods**********************
  public static ProvenanceException fromElastic(ElasticException e, String userMsg, String devMsg) {
    if(e.getErrorCode().equals(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR)) {
      return new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, userMsg, devMsg, e);
    } else {
      return new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING, userMsg, devMsg, e);
    }
  }
  
  public static boolean missingMappingForField(ProvenanceException e) {
    return e.getErrorCode().equals(RESTCodes.ProvenanceErrorCode.BAD_REQUEST)
      && e.getDevMsg().startsWith("missing mapping for field");
  }
  //*****************************************************************************
}
