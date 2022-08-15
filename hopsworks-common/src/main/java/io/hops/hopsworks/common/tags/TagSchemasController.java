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

package io.hops.hopsworks.common.tags;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.common.integrations.CommunityStereotype;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.featurestore.tag.TagSchemas;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@CommunityStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TagSchemasController implements TagSchemasControllerIface {
  @Override
  public void create(String name, String schema) throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  @Override
  public void delete(String name) throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  @Override
  public void delete(TagSchemas tag) throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  @Override
  public Map<String, String> getAll() throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  
  @Override
  public boolean schemaHasNestedTypes(String schema) throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
  
  @Override
  public boolean schemaHasAdditionalRules(String name, String schema, ObjectMapper objectMapper)
    throws GenericException {
    throw new GenericException(RESTCodes.GenericErrorCode.ENTERPRISE_FEATURE, Level.INFO);
  }
}
