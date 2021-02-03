/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.featurestore.tags;

import io.hops.hopsworks.common.dao.featurestore.tag.FeatureStoreTagFacade;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagSchemaControllerIface;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.persistence.entity.featurestore.tag.FeatureStoreTag;
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
public class FeatureStoreTagSchemasController implements FeatureStoreTagSchemaControllerIface {

  @EJB
  private FeatureStoreTagFacade featureStoreTagFacade;

  private void validateName(String name) throws FeatureStoreTagException {
    if (name == null || name.trim().isEmpty() || name.trim().contains(" ")) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.INVALID_TAG_NAME, Level.FINE);
    }
  }

  @Override
  public void create(String name, String schema) throws FeatureStoreTagException {
    validateName(name);
    FeatureStoreTag tag = featureStoreTagFacade.findByName(name);
    if (tag != null) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.TAG_ALREADY_EXISTS, Level.FINE);
    }
    if(schema == null) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.INVALID_TAG_SCHEMA, Level.FINE);
    }
    SchematizedTagHelper.validateSchema(schema);
    
    tag = new FeatureStoreTag(name, schema);
    featureStoreTagFacade.save(tag);
  }

  @Override
  public void delete(String name) {
    FeatureStoreTag tag = featureStoreTagFacade.findByName(name);
    delete(tag);
  }

  @Override
  public void delete(FeatureStoreTag tag) {
    if (tag != null) {
      featureStoreTagFacade.remove(tag);
    }
  }

  @Override
  public Map<String, String> getAll() {
    Map<String, String> tags = new HashMap<>();
    for(FeatureStoreTag tag : featureStoreTagFacade.findAll()) {
      tags.put(tag.getName(), tag.getSchema());
    }
    return tags;
  }
}
