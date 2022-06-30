/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.tags;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.BooleanSchema;
import org.everit.json.schema.CombinedSchema;
import org.everit.json.schema.EmptySchema;
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

import java.util.Map;
import java.util.logging.Level;

public class SchematizedTagHelper {
  
  public static void validateSchemaName(String name) throws SchematizedTagException {
    if (!FeaturestoreConstants.KEYWORDS_REGEX.matcher(name).matches()) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.INVALID_TAG_NAME, Level.FINE,
        "Tag Schema Names can only contain characters, numbers and underscores and cannot be " +
          "longer than " + FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
    }
  }
  
  public static Schema validateSchema(String schema) throws SchematizedTagException {
    try {
      return rawValidateSchema(schema);
    } catch (SchemaException | JSONException e) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.INVALID_TAG_SCHEMA, Level.FINE,
        "bad schema", "schema validation issue", e);
    }
  }
  
  static Schema rawValidateSchema(String schema) {
    JSONObject jsonSchema = new JSONObject(new JSONTokener(schema));
    return SchemaLoader.load(jsonSchema);
  }
  
  public static void validateTag(String schemaS, String val) throws SchematizedTagException {
    Schema schema = validateSchema(schemaS);
    try {
      if(schema instanceof ObjectSchema || schema instanceof CombinedSchema || schema instanceof EmptySchema) {
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
            throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.INVALID_TAG_VALUE, Level.FINE,
              "expected(schema) Integer val");
          }
          schema.validate(v);
        } else {
          Float v;
          try {
            v = Float.parseFloat(val);
          } catch (NumberFormatException e) {
            throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.INVALID_TAG_VALUE, Level.FINE,
              "expected(schema) Float val");
          }
          schema.validate(v);
        }
      } else if(schema instanceof BooleanSchema){
        Boolean v;
        if("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
          v = Boolean.parseBoolean(val);
        } else {
          throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.INVALID_TAG_VALUE, Level.FINE,
            "expected(schema) Boolean val");
        }
        schema.validate(v);
      } else if(schema instanceof StringSchema) {
        schema.validate(val);
      } else {
        throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_SCHEMA_NOT_FOUND, Level.FINE,
          "unhandled schema type internally");
      }
    } catch (ValidationException | JSONException e) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.INVALID_TAG_VALUE, Level.FINE,
        "error processing tag value", "schema or json validation issue", e);
    }
  }
  
  public static boolean hasNestedTypes(String value) throws SchematizedTagException {
    Schema schema = validateSchema(value);
    if(schema instanceof ObjectSchema) {
      ObjectSchema oSchema = (ObjectSchema) schema;
      for(Schema childSchema: oSchema.getPropertySchemas().values()) {
        if(childSchema instanceof ObjectSchema || childSchema instanceof CombinedSchema
          || childSchema instanceof EmptySchema) {
          return true;
        }
      }
      return false;
    } else {
      return true;
    }
  }
  
  private static boolean hasAdditionalRulesInt(String schema, ObjectMapper objectMapper,
                                               String... uiParsableAttributes) throws JsonProcessingException {
    JsonNode node = objectMapper.readTree(schema);
    int uiParsable = 0;
    for(String uiParsableAttribute : uiParsableAttributes) {
      if(node.has(uiParsableAttribute)) {
        uiParsable++;
      }
    }
    return node.size() > uiParsable;
  }
  public static boolean hasAdditionalRules(String name, String value, ObjectMapper objectMapper)
    throws SchematizedTagException {
    Schema schema = validateSchema(value);
    if(schema instanceof ObjectSchema) {
      ObjectSchema oSchema = (ObjectSchema) schema;
      for(Map.Entry<String, Schema> childEntry: oSchema.getPropertySchemas().entrySet()) {
        Schema childSchema = childEntry.getValue();
        if(childSchema instanceof NumberSchema || childSchema instanceof StringSchema
          || childSchema instanceof BooleanSchema) {
          try {
            if(hasAdditionalRulesInt(childSchema.toString(), objectMapper, "type", "description")) {
              return true;
            }
          } catch (JsonProcessingException e) {
            String usrMsg = "exception processing tag:" + name;
            String devMsg = usrMsg + " property:" + childEntry.getKey();
            throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.INTERNAL_PROCESSING_ERROR, Level.FINE,
              usrMsg, devMsg, e);
          }
        } else if(childSchema instanceof ArraySchema) {
          try {
            if(hasAdditionalRulesInt(childSchema.toString(), objectMapper, "type", "description", "items")) {
              return true;
            }
            if(hasAdditionalRulesInt(((ArraySchema)childSchema).getAllItemSchema().toString(), objectMapper,
              "type", "description")) {
              return true;
            }
          } catch (JsonProcessingException e) {
            String usrMsg = "exception processing tag:" + name;
            String devMsg = usrMsg + " property:" + childEntry.getKey();
            throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.INTERNAL_PROCESSING_ERROR, Level.FINE,
              usrMsg, devMsg, e);
          }
        }
      }
      return false;
    } else {
      return false;
    }
  }
}
