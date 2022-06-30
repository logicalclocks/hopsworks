/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.tags;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.common.dao.featurestore.tag.TagSchemasFacade;
import io.hops.hopsworks.common.tags.TagSchemasControllerIface;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.featurestore.tag.TagSchemas;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TagSchemasEEController implements TagSchemasControllerIface {

  @EJB
  private TagSchemasFacade tagSchemasFacade;
  
  @Override
  public void create(String name, String schema) throws SchematizedTagException {
    SchematizedTagHelper.validateSchemaName(name);
    TagSchemas tag = tagSchemasFacade.findByName(name);
    if (tag != null) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_ALREADY_EXISTS, Level.FINE);
    }
    if(schema == null) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.INVALID_TAG_SCHEMA, Level.FINE);
    }
    SchematizedTagHelper.validateSchema(schema);
    
    tag = new TagSchemas(name, schema);
    tagSchemasFacade.save(tag);
  }

  @Override
  public void delete(String name) {
    TagSchemas tag = tagSchemasFacade.findByName(name);
    delete(tag);
  }

  @Override
  public void delete(TagSchemas tag) {
    if (tag != null) {
      tagSchemasFacade.remove(tag);
    }
  }

  @Override
  public Map<String, String> getAll() {
    Map<String, String> tags = new HashMap<>();
    for(TagSchemas tag : tagSchemasFacade.findAll()) {
      tags.put(tag.getName(), tag.getSchema());
    }
    return tags;
  }
  
  @Override
  public boolean schemaHasNestedTypes(String schema) throws SchematizedTagException {
    return SchematizedTagHelper.hasNestedTypes(schema);
  }
  
  @Override
  public boolean schemaHasAdditionalRules(String name, String schema, ObjectMapper objectMapper)
    throws SchematizedTagException {
    return SchematizedTagHelper.hasAdditionalRules(name, schema, objectMapper);
  }
}
