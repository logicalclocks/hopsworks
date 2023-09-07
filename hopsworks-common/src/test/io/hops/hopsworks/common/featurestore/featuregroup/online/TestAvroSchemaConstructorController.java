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
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
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
    String hiveType = "decimal(10,6)";
    Schema result = avroSchemaConstructorController.toAvroPrimitiveType(hiveType);
    Assert.assertEquals("{\"type\":\"bytes\",\"logicalType\":\"decimal\",\"precision\":10,\"scale\":6}",
      result.toString());
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
    Assert.assertEquals("[\"null\",{\"type\":\"record\",\"name\":\"r471026777\",\"fields\":[{\"name\":\"label\"," +
      "\"type\":[\"null\",\"int\"]},{\"name\":\"value\",\"type\":[\"null\",{\"type\":\"array\",\"items\":[\"null\"," +
      "\"int\"]}]}]}]", result.toString());
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
    Project project = new Project();
    project.setName("project_name");

    Featurestore featureStore = new Featurestore();
    featureStore.setProject(project);

    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setName("fg");
    featuregroup.setVersion(1);
    featuregroup.setFeaturestore(featureStore);

    List<FeatureGroupFeatureDTO> schema = new ArrayList<>();
    schema.add(new FeatureGroupFeatureDTO("feature0", "int", ""));
    schema.add(new FeatureGroupFeatureDTO("feature1", "map<string,array<int>>", ""));
    schema.add(new FeatureGroupFeatureDTO("feature2", "struct<label:int,value:binary>", ""));
    String result = avroSchemaConstructorController.constructSchema(featuregroup, schema);
    Assert.assertEquals("{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"fg_1\",\n" +
      "  \"namespace\" : \"project_name_featurestore.db\",\n" +
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
      "      \"name\" : \"r933962134\",\n" +
      "      \"namespace\" : \"feature2\",\n" +
      "      \"fields\" : [ {\n" +
      "        \"name\" : \"label\",\n" +
      "        \"type\" : [ \"null\", \"int\" ]\n" +
      "      }, {\n" +
      "        \"name\" : \"value\",\n" +
      "        \"type\" : [ \"null\", \"bytes\" ]\n" +
      "      } ]\n" +
      "    } ]\n" +
      "  } ]\n" +
      "}",
      result);
  }

  @Test
  public void testConstructSchemaOverlyComplexFeatures() throws Exception {
    Project project = new Project();
    project.setName("project_name");

    Featurestore featureStore = new Featurestore();
    featureStore.setProject(project);

    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setName("fg");
    featuregroup.setVersion(1);
    featuregroup.setFeaturestore(featureStore);

    List<FeatureGroupFeatureDTO> schema = new ArrayList<>();
    schema.add(new FeatureGroupFeatureDTO("feature3", "struct<label1:string,value:struct<label2:string,value2:int>>",
      ""));
    schema.add(new FeatureGroupFeatureDTO("feature4", "struct<label3:string,value3:array<string>>", ""));
    schema.add(new FeatureGroupFeatureDTO("feature5", "struct<key:string,item1:struct<subkey:string," +
      "item2:array<int>>>", ""));
    schema.add(new FeatureGroupFeatureDTO("feature6", "array<struct<test:string,val:int>>", ""));
    String result = avroSchemaConstructorController.constructSchema(featuregroup, schema);
    Assert.assertEquals("{\n" +
        "  \"type\" : \"record\",\n" +
        "  \"name\" : \"fg_1\",\n" +
        "  \"namespace\" : \"project_name_featurestore.db\",\n" +
        "  \"fields\" : [ {\n" +
        "    \"name\" : \"feature3\",\n" +
        "    \"type\" : [ \"null\", {\n" +
        "      \"type\" : \"record\",\n" +
        "      \"name\" : \"r221276846\",\n" +
        "      \"namespace\" : \"feature3\",\n" +
        "      \"fields\" : [ {\n" +
        "        \"name\" : \"label1\",\n" +
        "        \"type\" : [ \"null\", \"string\" ]\n" +
        "      }, {\n" +
        "        \"name\" : \"value\",\n" +
        "        \"type\" : [ \"null\", {\n" +
        "          \"type\" : \"record\",\n" +
        "          \"name\" : \"r773256226\",\n" +
        "          \"namespace\" : \"feature3.r221276846\",\n" +
        "          \"fields\" : [ {\n" +
        "            \"name\" : \"label2\",\n" +
        "            \"type\" : [ \"null\", \"string\" ]\n" +
        "          }, {\n" +
        "            \"name\" : \"value2\",\n" +
        "            \"type\" : [ \"null\", \"int\" ]\n" +
        "          } ]\n" +
        "        } ]\n" +
        "      } ]\n" +
        "    } ]\n" +
        "  }, {\n" +
        "    \"name\" : \"feature4\",\n" +
        "    \"type\" : [ \"null\", {\n" +
        "      \"type\" : \"record\",\n" +
        "      \"name\" : \"r1179734341\",\n" +
        "      \"namespace\" : \"feature4\",\n" +
        "      \"fields\" : [ {\n" +
        "        \"name\" : \"label3\",\n" +
        "        \"type\" : [ \"null\", \"string\" ]\n" +
        "      }, {\n" +
        "        \"name\" : \"value3\",\n" +
        "        \"type\" : [ \"null\", {\n" +
        "          \"type\" : \"array\",\n" +
        "          \"items\" : [ \"null\", \"string\" ]\n" +
        "        } ]\n" +
        "      } ]\n" +
        "    } ]\n" +
        "  }, {\n" +
        "    \"name\" : \"feature5\",\n" +
        "    \"type\" : [ \"null\", {\n" +
        "      \"type\" : \"record\",\n" +
        "      \"name\" : \"r37053473\",\n" +
        "      \"namespace\" : \"feature5\",\n" +
        "      \"fields\" : [ {\n" +
        "        \"name\" : \"key\",\n" +
        "        \"type\" : [ \"null\", \"string\" ]\n" +
        "      }, {\n" +
        "        \"name\" : \"item1\",\n" +
        "        \"type\" : [ \"null\", {\n" +
        "          \"type\" : \"record\",\n" +
        "          \"name\" : \"r866074742\",\n" +
        "          \"namespace\" : \"feature5.r37053473\",\n" +
        "          \"fields\" : [ {\n" +
        "            \"name\" : \"subkey\",\n" +
        "            \"type\" : [ \"null\", \"string\" ]\n" +
        "          }, {\n" +
        "            \"name\" : \"item2\",\n" +
        "            \"type\" : [ \"null\", {\n" +
        "              \"type\" : \"array\",\n" +
        "              \"items\" : [ \"null\", \"int\" ]\n" +
        "            } ]\n" +
        "          } ]\n" +
        "        } ]\n" +
        "      } ]\n" +
        "    } ]\n" +
        "  }, {\n" +
        "    \"name\" : \"feature6\",\n" +
        "    \"type\" : [ \"null\", {\n" +
        "      \"type\" : \"array\",\n" +
        "      \"items\" : [ \"null\", {\n" +
        "        \"type\" : \"record\",\n" +
        "        \"name\" : \"r983056190\",\n" +
        "        \"namespace\" : \"feature6\",\n" +
        "        \"fields\" : [ {\n" +
        "          \"name\" : \"test\",\n" +
        "          \"type\" : [ \"null\", \"string\" ]\n" +
        "        }, {\n" +
        "          \"name\" : \"val\",\n" +
        "          \"type\" : [ \"null\", \"int\" ]\n" +
        "        } ]\n" +
        "      } ]\n" +
        "    } ]\n" +
        "  } ]\n" +
        "}",
      result);
  }
}
