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
package io.hops.hopsworks.common.kafka;

import io.hops.hopsworks.persistence.entity.kafka.schemas.Schemas;
import io.hops.hopsworks.common.dao.kafka.schemas.SchemasFacade;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectDTO;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SchemasController {
  
  private final static Logger LOGGER = Logger.getLogger(SchemasController.class.getName());
  @EJB
  private SchemasFacade schemasFacade;
  
  public Schemas addNewSchema(Project project, String schemaString) throws SchemaException {
    Schema schema = validateSchema(project, schemaString);
    Optional<Schemas> current = schemasFacade.findBySchema(project, schema.toString());
    if (current.isPresent()) {
      return current.get();
    } else {
      Schemas s = new Schemas(schema.toString(), project);
      schemasFacade.save(s);
      return s;
    }
  }
  
  private Schema validateSchema(Project project, String schema) throws SchemaException {
    if(schema == null){
      throw new IllegalArgumentException("No schema provided");
    }
    try {
      Schema s = new Schema.Parser().parse(schema);
      return s;
    } catch (SchemaParseException e) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INVALID_AVRO_SCHEMA, Level.FINE,
        "project=" + project.getName() + ", schema=" + schema);
    }
  }
  
  public SubjectDTO findSchemaById(Project project, Integer id) throws SchemaException {
    Schemas schema = schemasFacade.findSchemaById(project, id).orElseThrow(() ->
      new SchemaException(RESTCodes.SchemaRegistryErrorCode.SCHEMA_NOT_FOUND, Level.FINE,
        "project=" + project.getName() + ", schema_id=" + id));
    return new SubjectDTO(schema.getSchema());
  }
}
