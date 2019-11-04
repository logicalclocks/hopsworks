/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaParseException;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class KafkaFacade {

  private static final  Logger LOGGER = Logger.getLogger(KafkaFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  protected EntityManager getEntityManager() {
    return em;
  }

  public KafkaFacade() {
  }
  
  public void validateSchema(SchemaDTO schemaDTO) throws KafkaException {
    if(schemaDTO == null){
      throw new IllegalArgumentException("No schema provided");
    }
    validateSchemaNameAgainstBlacklist(schemaDTO.getName(), RESTCodes.KafkaErrorCode.CREATE_SCHEMA_RESERVED_NAME);
  }
  
  public void validateSchemaNameAgainstBlacklist(String schemaName, RESTCodes.KafkaErrorCode restCode)
    throws KafkaException {
    if(Settings.KAFKA_SCHEMA_BLACKLIST.contains(schemaName)){
      throw new KafkaException(restCode, Level.FINE);
    }
  }
  
  
  public SchemaCompatiblityCheck schemaBackwardCompatibility(SchemaDTO schemaDto) {
    
    String schemaContent = schemaDto.getContents();

    SchemaCompatibility.SchemaPairCompatibility schemaCompatibility;
    Schema writer;

    TypedQuery<SchemaTopics> query = em.createNamedQuery(
        "SchemaTopics.findByName", SchemaTopics.class);
    query.setParameter("name", schemaDto.getName());

    try {
      Schema reader = new Schema.Parser().parse(schemaContent);

      for (SchemaTopics schemaTopic : query.getResultList()) {

        writer = new Schema.Parser().parse(schemaTopic.getContents());

        schemaCompatibility = SchemaCompatibility.
            checkReaderWriterCompatibility(reader, writer);

        switch (schemaCompatibility.getType()) {

          case COMPATIBLE:
            break;
          case INCOMPATIBLE:
            return SchemaCompatiblityCheck.INCOMPATIBLE;
          case RECURSION_IN_PROGRESS:
            break;
        }
      }
    } catch (SchemaParseException ex) {
      return SchemaCompatiblityCheck.INVALID;
    }
    return SchemaCompatiblityCheck.COMPATIBLE;
  }

  //if schema exists, increment it if not start version from 1
  public void addSchemaForTopics(SchemaDTO schemaDto) {

    int newVersion = 1;

    TypedQuery<SchemaTopics> query = em.createNamedQuery(
        "SchemaTopics.findByName", SchemaTopics.class);
    query.setParameter("name", schemaDto.getName());

    List<SchemaTopics> schemaTopics = query.getResultList();

    if (schemaTopics != null && !schemaTopics.isEmpty()) {
      for (SchemaTopics schemaTopic : schemaTopics) {

        int schemaVersion = schemaTopic.getSchemaTopicsPK().getVersion();
        if (newVersion < schemaVersion) {
          newVersion = schemaVersion;
        }
      }
      newVersion++;
    }
    SchemaTopics schema = new SchemaTopics(schemaDto.getName(), newVersion, schemaDto.getContents(), new Date());

    em.persist(schema);
    em.flush();
  }
  
  public SchemaDTO getSchemaForTopic(String topicName) {
    
    List<ProjectTopics> topics = em.createNamedQuery(
      "ProjectTopics.findByTopicName", ProjectTopics.class)
      .setParameter("topicName", topicName).getResultList();
    if (topics != null && !topics.isEmpty()) {
      ProjectTopics topic = topics.get(0);
      
      SchemaTopics schema = em.find(SchemaTopics.class,
        new SchemaTopicsPK(
          topic.getSchemaTopics().getSchemaTopicsPK().getName(),
          topic.getSchemaTopics().getSchemaTopicsPK().getVersion()));
      
      if (schema != null) {
        return new SchemaDTO(schema.getContents());
      }
    }
    return null;
  }

  public SchemaTopics getSchema(String schemaName, Integer schemaVersion) {
    return em.createNamedQuery("SchemaTopics.findByNameAndVersion", SchemaTopics.class)
        .setParameter("name", schemaName)
        .setParameter("version", schemaVersion)
        .getSingleResult();
  }

  public List<SchemaDTO> listSchemasForTopics() {
    //get all schemas, and return the DTO
    Map<String, List<Integer>> schemas = new HashMap<>();
    List<SchemaDTO> schemaDtos = new ArrayList<>();
    String schemaName;

    TypedQuery<SchemaTopics> query = em.createNamedQuery(
        "SchemaTopics.findAll", SchemaTopics.class);

    for (SchemaTopics schema : query.getResultList()) {
      schemaName = schema.getSchemaTopicsPK().getName();
      schemas.computeIfAbsent(schemaName, k -> new ArrayList<>());
      schemas.get(schemaName).add(schema.getSchemaTopicsPK().getVersion());
    }
    for (Map.Entry<String, List<Integer>> schema : schemas.entrySet()) {
      schemaDtos.add(new SchemaDTO(schema.getKey(), schema.getValue()));
    }

    return schemaDtos;
  }

  public SchemaDTO getSchemaContent(String schemaName, Integer schemaVersion) throws KafkaException {
    SchemaTopics schemaTopic = em.find(SchemaTopics.class,
        new SchemaTopicsPK(schemaName, schemaVersion));
    if (schemaTopic == null) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_NOT_FOUND, Level.FINE, "Schema: " + schemaName);
    }
    return new SchemaDTO(schemaTopic.getContents());
  }
  
  public void deleteSchema(String schemaName, Integer version) throws KafkaException {
    validateSchemaNameAgainstBlacklist(schemaName, RESTCodes.KafkaErrorCode.DELETE_RESERVED_SCHEMA);
    //Check if schema is currently used by a topic.
    List<ProjectTopics> topics = em.createNamedQuery(
      "ProjectTopics.findBySchemaVersion", ProjectTopics.class)
      .setParameter("schema_name", schemaName)
      .setParameter("schema_version", version)
      .getResultList();
    if (topics != null && !topics.isEmpty()) {
      //Create a list of topic names to display to user
      throw new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_IN_USE, Level.FINE);
    } else {
      //get the bean and remove it
      SchemaTopics schema = em.find(SchemaTopics.class, new SchemaTopicsPK(schemaName, version));
      em.remove(schema);
      em.flush();
    }
  }
}
