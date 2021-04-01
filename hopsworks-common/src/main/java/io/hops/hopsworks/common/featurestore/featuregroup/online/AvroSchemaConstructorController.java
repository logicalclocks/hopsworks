/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featuregroup.online;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.hadoop.hive.serde2.thrift.Type;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AvroSchemaConstructorController {
  
  public AvroSchemaConstructorController() {}
  
  public String constructSchema(String featureGroupEntityName, String featureStoreName,
                                List<FeatureGroupFeatureDTO> schema) throws FeaturestoreException {
    SchemaBuilder.TypeBuilder<Schema> avroSchema = SchemaBuilder.builder();
    
    // top level needs to be record
    SchemaBuilder.FieldAssembler<Schema> recordSchema =
      avroSchema.record(featureGroupEntityName).namespace(featureStoreName).fields();
    
    for (FeatureGroupFeatureDTO feature : schema) {
      recordSchema = recordSchema.name(feature.getName())
        .type(toAvro(feature.getType(), true, feature.getName())).noDefault();
    }
    return recordSchema.endRecord().toString(true);
  }
  
  public Schema toAvroPrimitiveType(String hiveType) throws FeaturestoreException {
    SchemaBuilder.TypeBuilder<Schema> avroSchema = SchemaBuilder.builder();
    
    switch (Type.getType(hiveType.toUpperCase())) {
      case INT_TYPE:
      case SMALLINT_TYPE:
        return avroSchema.intType();
      case BOOLEAN_TYPE:
        return avroSchema.booleanType();
      case TINYINT_TYPE:
      case BINARY_TYPE:
        return avroSchema.bytesType();
      case BIGINT_TYPE:
        return avroSchema.longType();
      case FLOAT_TYPE:
        return avroSchema.floatType();
      case DOUBLE_TYPE:
        return avroSchema.doubleType();
      case STRING_TYPE:
        return avroSchema.stringType();
      // logical types
      case DATE_TYPE:
        return LogicalTypes.date().addToSchema(avroSchema.intType());
      case TIMESTAMP_TYPE:
        return LogicalTypes.timestampMicros().addToSchema(avroSchema.longType());
      case DECIMAL_TYPE:
        // is this precision/scale correct?
        return LogicalTypes.decimal(Type.DECIMAL_TYPE.getMaxPrecision(),Type.DECIMAL_TYPE.getMaximumScale())
          .addToSchema(avroSchema.bytesType());
      default:
        // shouldn't happen
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.AVRO_PRIMITIVE_TYPE_NOT_SUPPORTED, Level.FINE,
          "The provided type is not valid or not supported: " + hiveType);
    }
  }
  
  public Schema toAvro(String hiveType, boolean nullable, String nameSpace) throws FeaturestoreException {
    SchemaBuilder.TypeBuilder<Schema> avroSchema = SchemaBuilder.builder();
    String sanetizedHiveType = hiveType.toUpperCase().replaceAll("\\s+", "");
    Schema result;
    
    if (sanetizedHiveType.startsWith(Type.ARRAY_TYPE.getName())) {
      result = avroSchema.array().items(toAvro(getInnerType(sanetizedHiveType, 5), nullable, nameSpace));
    } else if (sanetizedHiveType.startsWith(Type.MAP_TYPE.getName())) {
      String[] mapTypes = getMapTypes(getInnerType(sanetizedHiveType, 3));
      if (!mapTypes[0].equals(Type.STRING_TYPE.getName())) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.AVRO_MAP_STRING_KEY, Level.FINE, "The type of" +
          " Map keys has to be STRING, but found: " + mapTypes[0]);
      }
      result = avroSchema.map().values(toAvro(mapTypes[1], nullable, nameSpace));
    } else if (sanetizedHiveType.startsWith(Type.STRUCT_TYPE.getName())) {
      // use random name, as user has no possibility to provide it and it doesn't matter for de-/serialization
      String recordName = "r" + sanetizedHiveType.hashCode();
      String childNameSpace = !Strings.isNullOrEmpty(nameSpace) ? nameSpace + "." + recordName : recordName;
      SchemaBuilder.FieldAssembler<Schema> record = avroSchema.record(recordName).namespace(nameSpace).fields();
      for (String field : parseStructFields(getInnerType(sanetizedHiveType, 6))) {
        String[] structField = getStructField(field);
        if (structField.length != 2) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.AVRO_MALFORMED_SCHEMA, Level.FINE, "Failed " +
            "to convert STRUCT type: " + structField);
        }
        record.name(structField[0]).type(toAvro(structField[1], nullable, childNameSpace)).noDefault();
      }
      result = record.endRecord();
    } else {
      result = toAvroPrimitiveType(sanetizedHiveType);
    }
    if (nullable) {
      return Schema.createUnion(Schema.create(Schema.Type.NULL), result);
    }
    return result;
  }
  
  public String getInnerType(String hiveType, int offset) throws FeaturestoreException {
    String innerType = hiveType.substring(offset);
    
    if (innerType.startsWith("<") && innerType.endsWith(">")) {
      return innerType.substring(1, innerType.length() - 1);
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.AVRO_MALFORMED_SCHEMA, Level.FINE, "The " +
        "provided type is malformed: " + hiveType);
    }
  }
  
  public String[] getMapTypes(String hiveType) {
    // Map types are defined by a key-type and a map-type, however, AVRO supports only STRING as key-type, therefore
    // this splits on the first comma, as the key-type shouldn't contain another comma
    // for example "STRING,ARRAY<INT>"
    return hiveType.split(",",2);
  }
  
  public List<String> parseStructFields(String structFields) throws FeaturestoreException {
    // takes a comma separated string of struct field key:value pairs
    // for example "key1:TYPE1<TYPE<Type>>,key2:TYPE2<TYPE>"
    Stack<Character> brackets = new Stack<>();
    StringBuilder field = new StringBuilder();
    List<String> fields = new ArrayList<>();
    
    for (Character ch : structFields.toCharArray()) {
      if (ch.equals('<')) {
        brackets.push('<');
        field.append(ch);
      } else if (ch.equals('>')) {
        try {
          brackets.pop();
        } catch (EmptyStackException e) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.AVRO_MALFORMED_SCHEMA, Level.FINE, "The " +
            "provided struct field type is malformed: " + structFields);
        }
        field.append(ch);
      } else if (ch.equals(',') && brackets.empty()) {
        fields.add(field.toString());
        field = new StringBuilder();
      } else {
        field.append(ch);
      }
    }
    fields.add(field.toString());
    if (brackets.empty()) {
      return fields;
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.AVRO_MALFORMED_SCHEMA, Level.FINE, "The " +
        "provided struct field type is malformed: " + structFields);
    }
  }
  
  public String[] getStructField(String structField) {
    // struct field is defined by a key and a value-type: "key:TYPE"
    return structField.split(":");
  }
}
