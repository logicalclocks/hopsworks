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

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import org.apache.avro.Schema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

public class TestAvroSchemaConstructorController {
  
  private AvroSchemaConstructorController avroSchemaConstructorController;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    avroSchemaConstructorController = new AvroSchemaConstructorController();
  }
  
  @Test
  public void testGetStructField() {
    String[] structField = avroSchemaConstructorController.getStructField("label:COMPLEXTYPE<PRIMITIVETYPE>");
    Assert.assertEquals(2, structField.length);
    Assert.assertEquals("label", structField[0]);
    Assert.assertEquals("COMPLEXTYPE<PRIMITIVETYPE>", structField[1]);
  }
  
  @Test
  public void testParseStructFields() throws Exception {
    String structFields = "key1:TYPE1<TYPE<Type>>,key2:TYPE2<TYPE>";
    List<String> result = avroSchemaConstructorController.parseStructFields(structFields);
    Assert.assertEquals(result.get(0), "key1:TYPE1<TYPE<Type>>");
    Assert.assertEquals(result.get(1), "key2:TYPE2<TYPE>");
  }
  
  @Test
  public void testParseStructFieldsException() throws Exception {
    String structFields = "key1:TYPE1<TYPE<Type>>,key2:TYPE2<TYPE>>";
    thrown.expect(FeaturestoreException.class);
    avroSchemaConstructorController.parseStructFields(structFields);
  
    structFields = "key1:TYPE1<TYPE<Type>>,key2:TYPE2<<TYPE>";
    thrown.expect(FeaturestoreException.class);
    avroSchemaConstructorController.parseStructFields(structFields);
  }
  
  @Test
  public void testGetMapTypes() {
    String mapTypes = "STRING,ARRAY<INT>";
    String[] result = avroSchemaConstructorController.getMapTypes(mapTypes);
    Assert.assertEquals("STRING", result[0]);
    Assert.assertEquals("ARRAY<INT>", result[1]);
  }
  
  @Test
  public void testGetInnerType() throws Exception {
    String completeType = "ARRAY<MAP<STRING,INT>>";
    String result = avroSchemaConstructorController.getInnerType(completeType, 5);
    Assert.assertEquals(result, "MAP<STRING,INT>");
  }
  
  @Test
  public void testGetInnerTypeException() throws Exception {
    String completeType = "ARRAY MAP<STRING,INT>";
    thrown.expect(FeaturestoreException.class);
    avroSchemaConstructorController.getInnerType(completeType, 5);
  }
  
  @Test
  public void testToPrimitiveType() throws Exception {
    String hiveType = "int";
    Schema result = avroSchemaConstructorController.toAvroPrimitiveType(hiveType);
    Assert.assertEquals("\"int\"", result.toString());
  }
  
  @Test
  public void testToPrimitiveTypeLogicalDate() throws Exception {
    String hiveType = "date";
    Schema result = avroSchemaConstructorController.toAvroPrimitiveType(hiveType);
    Assert.assertEquals("{\"type\":\"int\",\"logicalType\":\"date\"}", result.toString());
  }
  
  @Test
  public void testToPrimitiveTypeLogicalTimeStamp() throws Exception {
    String hiveType = "timestamp";
    Schema result = avroSchemaConstructorController.toAvroPrimitiveType(hiveType);
    Assert.assertEquals("{\"type\":\"long\",\"logicalType\":\"timestamp-micros\"}", result.toString());
  }
  
  @Test
  public void testToPrimitiveTypeLogicalDecimal() throws Exception {
    String hiveType = "decimal";
    Schema result = avroSchemaConstructorController.toAvroPrimitiveType(hiveType);
    Assert.assertEquals("{\"type\":\"bytes\",\"logicalType\":\"decimal\",\"precision\":38,\"scale\":0}", result.toString());
  }
  
  @Test
  public void testToAvroArray() throws Exception {
    String hiveType = "array<int>";
    Schema result = avroSchemaConstructorController.toAvro(hiveType, true, "");
    Assert.assertEquals("[\"null\",{\"type\":\"array\",\"items\":[\"null\",\"int\"]}]", result.toString());
  }
  
  @Test
  public void testToAvroMap() throws Exception {
    String hiveType = "map<string,int>";
    Schema result = avroSchemaConstructorController.toAvro(hiveType, true, "");
    Assert.assertEquals("[\"null\",{\"type\":\"map\",\"values\":[\"null\",\"int\"]}]", result.toString());
  }
  
  @Test
  public void testToAvroStruct() throws Exception {
    String hiveType = "struct<label:int,value:array<int>>";
    Schema result = avroSchemaConstructorController.toAvro(hiveType, true, "");
    Assert.assertEquals("[\"null\",{\"type\":\"record\",\"name\":\"r335499193\",\"fields\":[{\"name\":\"LABEL\",\"type\":[\"null\",\"int\"]},{\"name\":\"VALUE\",\"type\":[\"null\",{\"type\":\"array\",\"items\":[\"null\",\"int\"]}]}]}]", result.toString());
  }
  
  @Test
  public void testToAvroMapException() throws Exception {
    String hiveType = "map<int,int>";
    thrown.expect(FeaturestoreException.class);
    avroSchemaConstructorController.toAvro(hiveType, true, "");
  }
  
  @Test
  public void testToAvroStructException() throws Exception {
    String hiveType = "struct<int,int>";
    thrown.expect(FeaturestoreException.class);
    avroSchemaConstructorController.toAvro(hiveType, true, "");
  }
  
  @Test
  public void testConstructSchema() throws Exception {
    List<FeatureGroupFeatureDTO> schema = new ArrayList<>();
    schema.add(new FeatureGroupFeatureDTO("feature0", "INT", ""));
    schema.add(new FeatureGroupFeatureDTO("feature1", "MAP<STRING,ARRAY<INT>>", ""));
    schema.add(new FeatureGroupFeatureDTO("feature2", "STRUCT<label:INT,value:BINARY>", ""));
    String result = avroSchemaConstructorController.constructSchema("fg", "fs", schema);
    Assert.assertEquals("{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"fg\",\n" +
      "  \"namespace\" : \"fs\",\n" +
      "  \"fields\" : [ {\n" +
      "    \"name\" : \"feature0\",\n" +
      "    \"type\" : [ \"null\", \"int\" ]\n" +
      "  }, {\n" +
      "    \"name\" : \"feature1\",\n" +
      "    \"type\" : [ \"null\", {\n" +
      "      \"type\" : \"map\",\n" +
      "      \"values\" : [ \"null\", {\n" +
      "        \"type\" : \"array\",\n" +
      "        \"items\" : [ \"null\", \"int\" ]\n" +
      "      } ]\n" +
      "    } ]\n" +
      "  }, {\n" +
      "    \"name\" : \"feature2\",\n" +
      "    \"type\" : [ \"null\", {\n" +
      "      \"type\" : \"record\",\n" +
      "      \"name\" : \"r1656394250\",\n" +
      "      \"namespace\" : \"feature2\",\n" +
      "      \"fields\" : [ {\n" +
      "        \"name\" : \"LABEL\",\n" +
      "        \"type\" : [ \"null\", \"int\" ]\n" +
      "      }, {\n" +
      "        \"name\" : \"VALUE\",\n" +
      "        \"type\" : [ \"null\", \"bytes\" ]\n" +
      "      } ]\n" +
      "    } ]\n" +
      "  } ]\n" +
      "}", result);
  }
}
