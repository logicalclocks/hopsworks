/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.featurestore.tags;

import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.BooleanSchema;
import org.everit.json.schema.CombinedSchema;
import org.everit.json.schema.NumberSchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.SchemaException;
import org.everit.json.schema.StringSchema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.logging.Level;

public class SchematizedTagHelper {
  
  public static Schema validateSchema(String schema) throws FeatureStoreTagException {
    try {
      return rawValidateSchema(schema);
    } catch (SchemaException | JSONException e) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.INVALID_TAG_SCHEMA, Level.FINE,
        "bad schema", "schema validation issue", e);
    }
  }
  
  static Schema rawValidateSchema(String schema) {
    JSONObject jsonSchema = new JSONObject(new JSONTokener(schema));
    return SchemaLoader.load(jsonSchema);
  }
  
  public static void validateTag(String schemaS, String val) throws FeatureStoreTagException {
    Schema schema = validateSchema(schemaS);
    try {
      if(schema instanceof ObjectSchema || schema instanceof CombinedSchema) {
        schema.validate(new JSONObject(val));
      } else if (schema instanceof ArraySchema) {
        schema.validate(new JSONArray(val));
      } else if (schema instanceof NumberSchema) {
        NumberSchema nSchema = (NumberSchema) schema;
        if(nSchema.requiresInteger()) {
          Integer v;
          try {
            v = Integer.parseInt(val);
          } catch (NumberFormatException e) {
            throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.INVALID_TAG_VALUE, Level.FINE,
              "expected(schema) Integer val");
          }
          schema.validate(v);
        } else {
          Float v;
          try {
            v = Float.parseFloat(val);
          } catch (NumberFormatException e) {
            throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.INVALID_TAG_VALUE, Level.FINE,
              "expected(schema) Float val");
          }
          schema.validate(v);
        }
      } else if(schema instanceof BooleanSchema){
        Boolean v;
        if("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
          v = Boolean.parseBoolean(val);
        } else {
          throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.INVALID_TAG_VALUE, Level.FINE,
            "expected(schema) Boolean val");
        }
        schema.validate(v);
      } else if(schema instanceof StringSchema) {
        schema.validate(val);
      } else {
        throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.TAG_SCHEMA_NOT_FOUND, Level.FINE,
          "unhandled schema type internally");
      }
    } catch (ValidationException | JSONException e) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.INVALID_TAG_VALUE, Level.FINE,
        "error processing tag value", "schema or json validation issue", e);
    }
  }
}
