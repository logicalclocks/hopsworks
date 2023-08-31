/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.tags;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.SchemaException;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Random;

public class TestSchematizedTags {
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Test
  public void testMalfomedJSONForSchema() {
    String schemaS = "{";
    
    thrown.expect(JSONException.class);
    SchematizedTagHelper.rawValidateSchema(schemaS);
  }
  
  @Test
  public void testMalformedSchema() {
    String schemaS = "{";
    schemaS += "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "title : \"Test Schema\",";
    schemaS += "properties : {";
    schemaS += "id : {";
    schemaS += "type : \"blabla\"";
    schemaS += "}";
    schemaS += "}";
    schemaS += "}";
  
    thrown.expect(SchemaException.class);
    thrown.expectMessage("unknown type: [blabla]");
    SchematizedTagHelper.rawValidateSchema(schemaS);
  }
  
  @Test
  public void testMalformedJSONTag() throws FeatureStoreMetadataException {
    String schemaS = "{";
    schemaS +=  "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "title : \"Test Schema\",";
    schemaS += "properties : {";
    schemaS += "id : {";
    schemaS += "type : \"integer\"";
    schemaS += "}";
    schemaS += "},";
    schemaS += "required : [ \"id\" ]";
    schemaS += "}";
    
    String val = "{";
    
    thrown.expect(JSONException.class);
    Schema schema = SchematizedTagHelper.validateSchema(schemaS);
    schema.validate(new JSONObject(val));
  }
  
  @Test
  public void testMalformedTag() throws FeatureStoreMetadataException {
    Random rand = new Random(123);
    
    String schemaS = "{";
    schemaS +=  "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "title : \"Test Schema\",";
    schemaS += "properties : {";
    schemaS += "id : {";
    schemaS += "type : \"integer\"";
    schemaS += "}";
    schemaS += "},";
    schemaS += "required : [ \"id\" ]";
    schemaS += "}";
  
    String val = "{";
    val += "not_id : " + rand.nextInt(100000);
    val += "}";
  
    thrown.expect(ValidationException.class);
    thrown.expectMessage("required key [id] not found");
    Schema schema = SchematizedTagHelper.validateSchema(schemaS);
    schema.validate(new JSONObject(val));
  }
  
  private String arrayFieldSchema1(String elementType) {
    String schemaS = "{";
    schemaS +=  "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "\"title\" : \"Test Schema\",";
    schemaS += "\"type\" : \"object\",";
    schemaS += "\"properties\" : {";
    schemaS += "\"f\" : {";
    schemaS += "\"type\" : \"array\",";
    schemaS += "\"items\": {";
    schemaS += "\"type\": \"" + elementType + "\"";
    schemaS += "}";
    schemaS += "}";
    schemaS += "},";
    schemaS += "required : [ \"f\" ]";
    schemaS += "}";
    return schemaS;
  }

  @Test
  public void testPropertyTypeStringArray() throws FeatureStoreMetadataException {
    Random rand = new Random(123);
    String schemaS = arrayFieldSchema1("string");
    SchematizedTagHelper.validateSchema(schemaS);
    String val = "{";
    val += "f : " + "[" + "test_" + rand.nextInt(100000) + "," + "test_" + rand.nextInt(100000) + "]";
    val += "}";
    SchematizedTagHelper.validateTag(schemaS, val);
  }
  
  @Test
  public void testPropertyTypeIntArray() throws FeatureStoreMetadataException {
    Random rand = new Random(123);
    String schemaS = arrayFieldSchema1("integer");
    SchematizedTagHelper.validateSchema(schemaS);
    String val = "{";
    val += "\"f\":" + "[" + rand.nextInt(100000) + "," + rand.nextInt(100000) + "]";
    val += "}";
    SchematizedTagHelper.validateTag(schemaS, val);
  }
  
  @Test
  public void testPropertyTypeBadIntArray1() throws FeatureStoreMetadataException {
    Random rand = new Random(123);
    String schemaS = arrayFieldSchema1("integer");
    Schema schema = SchematizedTagHelper.validateSchema(schemaS);
    String val = "{";
    val += "\"f\":" + "[" + "a" + "," + rand.nextInt(100000) + "," + rand.nextInt(100000) + "]";
    val += "}";
    thrown.expect(ValidationException.class);
    thrown.expectMessage("expected type: Number, found: String");
    schema.validate(new JSONObject(val));
  }
  
  @Test
  public void testPropertyTypeBadIntArray2() throws FeatureStoreMetadataException {
    Random rand = new Random(123);
    String schemaS = arrayFieldSchema1("integer");
    Schema schema = SchematizedTagHelper.validateSchema(schemaS);
    String val = "{";
    val += "\"f\":" + "[" + rand.nextInt(10000) + "," + rand.nextFloat() + "]";
    val += "}";
    thrown.expect(ValidationException.class);
    thrown.expectMessage("expected type: Integer, found: Double");
    schema.validate(new JSONObject(val));
  }
  
  @Test
  public void testPropertyTypeFloatArray() throws FeatureStoreMetadataException {
    Random rand = new Random(123);
    String schemaS = arrayFieldSchema1("number");
    SchematizedTagHelper.validateSchema(schemaS);
    String val = "{";
    val += "\"f\":" + "[" + rand.nextFloat() + "," + rand.nextFloat() + "]";
    val += "}";
    SchematizedTagHelper.validateTag(schemaS, val);
  }
  
  @Test
  public void testPropertyTypeBadFloatArray() throws FeatureStoreMetadataException {
    Random rand = new Random(123);
    String schemaS = arrayFieldSchema1("number");
    Schema schema = SchematizedTagHelper.validateSchema(schemaS);
    String val = "{";
    val += "\"f\":" + "[" + "\"test\"" + "," + rand.nextFloat() + "]";
    val += "}";
    thrown.expect(ValidationException.class);
    thrown.expectMessage("expected type: Number, found: String");
    schema.validate(new JSONObject(val));
  }
  
  @Test
  public void testPropertyTypeBoolArray() throws FeatureStoreMetadataException {
    String schemaS = arrayFieldSchema1("boolean");
    SchematizedTagHelper.validateSchema(schemaS);
    String val = "{";
    val += "\"f\":" + "[" + true + "]";
    val += "}";
    SchematizedTagHelper.validateTag(schemaS, val);
  }
  
  @Test
  public void testSchemaTypePrimitiveString() throws FeatureStoreMetadataException {
    String schemaS = "{";
    schemaS +=  "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "\"type\" : \"string\"";
    schemaS += "}";
  
    SchematizedTagHelper.validateSchema(schemaS);
    String val = "stringValue";
    SchematizedTagHelper.validateTag(schemaS, val);
  }
  
  @Test
  public void testSchemaTypePrimitiveInteger() throws FeatureStoreMetadataException {
    String schemaS = "{";
    schemaS +=  "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "\"type\" : \"integer\"";
    schemaS += "}";
    
    SchematizedTagHelper.validateSchema(schemaS);
    SchematizedTagHelper.validateTag(schemaS, "123");
  }
  
  @Test
  public void testSchemaTypePrimitiveIntegerBad() throws FeatureStoreMetadataException {
    String schemaS = "{";
    schemaS +=  "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "\"type\" : \"integer\"";
    schemaS += "}";
    
    Schema schema = SchematizedTagHelper.validateSchema(schemaS);
    String val = "stringValue";
  
    thrown.expect(ValidationException.class);
    thrown.expectMessage("expected type: Number, found: String");
    schema.validate(val);
  }
  
  @Test
  public void testSchemaTypePrimitiveFloat() throws FeatureStoreMetadataException {
    String schemaS = "{";
    schemaS +=  "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "\"type\" : \"number\"";
    schemaS += "}";
    
    SchematizedTagHelper.validateSchema(schemaS);
    SchematizedTagHelper.validateTag(schemaS, "123.2");
  }
  
  @Test
  public void testSchemaTypePrimitiveBoolean() throws FeatureStoreMetadataException {
    String schemaS = "{";
    schemaS += "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "\"type\" : \"boolean\"";
    schemaS += "}";
  
    SchematizedTagHelper.validateSchema(schemaS);
    SchematizedTagHelper.validateTag(schemaS, "true");
  }
  
  @Test
  public void testSchemaTypePrimitiveStringArray() throws FeatureStoreMetadataException {
    String schemaS = "{";
    schemaS += "$schema : \"http://json-schema.org/draft-07/schema#\",";
    schemaS += "$id : \"http://example.com/product.schema.json\",";
    schemaS += "\"type\" : \"array\",";
    schemaS += "\"items\": {";
    schemaS += "\"type\": \"string\"";
    schemaS += "}";
    schemaS += "}";
    
    SchematizedTagHelper.validateSchema(schemaS);
    String val = "[" + "\"test1\"" + "," + "\"test2\"" + "]";
    SchematizedTagHelper.validateTag(schemaS, val);
  }
  
  @Test
  public void testComplexSuccess() throws FeatureStoreMetadataException {
    String schemaS ="{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"$id\":\"http://heap.com/test.schema.json\",\"allOf\":[{\"$ref\":\"#/definitions/disease\"},{\"$ref\":\"#/definitions/cancer\"},{\"$ref\":\"#/definitions/other\"}],\"definitions\":{\"disease\":{\"properties\":{\"disease_type\":{\"type\":\"string\"}}},\"cancer\":{\"properties\":{\"cancer_type\":{\"type\":\"string\"}}},\"other\":{\"properties\":{\"other_field\":{\"type\":\"string\"}}}},\"required\":[\"disease\"],\"if\":{\"properties\":{\"disease\":{\"const\":\"Cancer\"}}},\"then\":{\"required\":[\"cancer_type\"]}}";
    SchematizedTagHelper.validateSchema(schemaS);
    String val = "{\"disease\":\"Diabetes\"}";
    SchematizedTagHelper.validateTag(schemaS, val);
  }
  
  @Test
  public void testComplexFail1() throws FeatureStoreMetadataException {
    String schemaS ="{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"$id\":\"http://heap.com/test.schema.json\",\"allOf\":[{\"$ref\":\"#/definitions/disease\"},{\"$ref\":\"#/definitions/cancer\"},{\"$ref\":\"#/definitions/other\"}],\"definitions\":{\"disease\":{\"properties\":{\"disease_type\":{\"type\":\"string\"}}},\"cancer\":{\"properties\":{\"cancer_type\":{\"type\":\"string\"}}},\"other\":{\"properties\":{\"other_field\":{\"type\":\"string\"}}}},\"required\":[\"disease\"],\"if\":{\"properties\":{\"disease\":{\"const\":\"Cancer\"}}},\"then\":{\"required\":[\"cancer_type\"]}}";
    SchematizedTagHelper.validateSchema(schemaS);
    String val = "{\"disease\":\"Cancer\"}";
    thrown.expect(FeatureStoreMetadataException.class);
    thrown.expectMessage("#: #: only 2 subschema matches out of 3");
    SchematizedTagHelper.validateTag(schemaS, val);
  }
  
  @Test
  public void testComplexSuccess2() throws FeatureStoreMetadataException {
    String schemaS ="{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"$id\":\"http://heap.com/test.schema.json\",\"allOf\":[{\"$ref\":\"#/definitions/disease\"},{\"$ref\":\"#/definitions/cancer\"},{\"$ref\":\"#/definitions/other\"}],\"definitions\":{\"disease\":{\"properties\":{\"disease_type\":{\"type\":\"string\"}}},\"cancer\":{\"properties\":{\"cancer_type\":{\"type\":\"string\"}}},\"other\":{\"properties\":{\"other_field\":{\"type\":\"string\"}}}},\"required\":[\"disease\"],\"if\":{\"properties\":{\"disease\":{\"const\":\"Cancer\"}}},\"then\":{\"required\":[\"cancer_type\"]}}";
    SchematizedTagHelper.validateSchema(schemaS);
    String val = "{\"disease\":\"Cancer\", \"cancer_type\":\"Malinoma\"}";
    SchematizedTagHelper.validateTag(schemaS, val);
  }
  
  @Test
  public void testC() throws FeatureStoreMetadataException {
    String schemaS = "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"$id\":\"http://heap.com/test.schema" +
      ".json\",\"allOf\":[{\"$ref\":\"#/definitions/hpv_sampling\"},{\"$ref\":\"#/definitions/cancer_properties\"}],\"definitions\":{\"hpv_sampling\":{\"properties\":{\"label_id\":{\"type\":\"string\"},\"gender\":{\"type\":\"string\",\"enum\":[\"Female\",\"Male\",\"Other\"]},\"Disease\":{\"type\":\"string\"},\"ICD_10\":{\"type\":\"string\"},\"HPVDIAG\":{\"type\":\"string\",\"enum\":[\"NEG\",\"POZ\"]},\"sample_taken\":{\"properties\":{\"date\":{\"type\":\"string\",\"format\":\"date\"},\"age\":{\"type\":\"integer\",\"minimum\":20,\"maximum\":100}},\"required\":[\"age\"]}}},\"cancer_properties\":{\"properties\":{\"Cancer type\":{\"type\":\"string\"},\"Cancer status\":{\"type\":\"string\",\"enum\":[\"Control\",\"Case\"]}}}},\"if\":{\"properties\":{\"Disease\":{\"const\":\"Cancer\"}}},\"then\":{\"required\":[\"Cancer type\",\"Cancer status\"]},\"required\":[\"label_id\",\"Disease\",\"sample_taken\"]}";
    SchematizedTagHelper.validateSchema(schemaS);
  }
  
  @Test
  public void testNestedFieldsFlagEmptySchema() throws FeatureStoreMetadataException {
    String schemaS = "{\"type\":\"object\",\"properties\":{\"address\":{\"street\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"}}},\"required\":[],\"additionalProperties\":false}";
    Assert.assertTrue(SchematizedTagHelper.hasNestedTypes(schemaS));
  }
  @Test
  public void testNestedFieldsFlagObjectSchema() throws FeatureStoreMetadataException {
    String schemaS = "{\"type\":\"object\",\"properties\":{\"address\":{\"properties\": {\"street\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"}}}},\"required\":[],\"additionalProperties\":false}";
    Assert.assertTrue(SchematizedTagHelper.hasNestedTypes(schemaS));
  }
  
  @Test
  public void testAdditionalRulesStringPattern() throws FeatureStoreMetadataException, GenericException {
    String schemaS = "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\",\"pattern\":\"^[A-Z]{2}[0-9]{4}$\"}},\"required\":[],\"additionalProperties\":false}";
    Assert.assertTrue(SchematizedTagHelper.hasAdditionalRules("testTag", schemaS, new ObjectMapper()));
  }
  
  @Test
  public void testAdditionalRulesNumberMinMax() throws FeatureStoreMetadataException, GenericException {
    String schemaS = "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":150}},\"required\":[],\"additionalProperties\":false}";
    Assert.assertTrue(SchematizedTagHelper.hasAdditionalRules("testTag", schemaS, new ObjectMapper()));
  }
  
  @Test
  public void testAdditionalRulesFales() throws FeatureStoreMetadataException, GenericException {
    String schemaS = "{\"name\":\"test\",\"description\":\"lala\",\"properties\":{\"field\":{\"type\":\"string\",\"description\":\"dsds\"}},\"required\":[\"field\"]}";
    Assert.assertFalse(SchematizedTagHelper.hasAdditionalRules("testTag", schemaS, new ObjectMapper()));
  }
}
