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

import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.schemas.CompatibilityCheckDto;
import io.hops.hopsworks.common.dao.kafka.schemas.SchemaCompatibility;
import io.hops.hopsworks.common.dao.kafka.schemas.Schemas;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectDto;
import io.hops.hopsworks.common.dao.kafka.schemas.Subjects;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectsFacade;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectsCompatibility;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectsCompatibilityFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;
import org.apache.avro.SchemaValidationException;
import org.apache.avro.SchemaValidator;
import org.apache.avro.SchemaValidatorBuilder;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SubjectsController {
  
  private final static Logger LOGGER = Logger.getLogger(SubjectsController.class.getName());
  @EJB
  private SubjectsFacade subjectsFacade;
  @EJB
  private SubjectsCompatibilityFacade subjectsCompatibilityFacade;
  @EJB
  private SchemasController schemasController;
  @EJB
  private ProjectTopicsFacade projectTopicsFacade;
  
  public List<String> getSubjects(Project project) {
    return subjectsFacade.getListOfSubjects(project);
  }
  
  public List<Integer> getSubjectVersions(Project project, String subject) throws SchemaException{
    List<Integer> versions = subjectsFacade.findSubjectByName(project, subject)
      .stream()
      .map(Subjects::getVersion)
      .sorted()
      .collect(Collectors.toList());
    if (versions.isEmpty()) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.FINE, "subject=" +
        subject);
    }
    return versions;
  }
  
  public SubjectDto getSubjectDetails(Project project, String subject, String version) throws SchemaException {
    validateVersion(version);
    if (subjectsFacade.findSubjectByName(project, subject).isEmpty()) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.FINE, "subject=" +
        subject);
    }
  
    Optional<Subjects> optional;
    if (version.equals("latest")) {
      optional = subjectsFacade.findSubjectLatestVersion(project, subject);
    } else {
      optional = subjectsFacade.findSubjectByNameAndVersion(project, subject, Integer.valueOf(version));
    }
    
    if (optional.isPresent()) {
      Subjects res = optional.get();
      return new SubjectDto(res.getSchema().getId(),
        res.getSubjectsPK().getSubject(),
        res.getVersion(),
        res.getSchema().getSchema());
    }
    else {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.VERSION_NOT_FOUND, Level.FINE, "subject=" +
        subject + ", version=" + version);
    }
  }
  
  public SubjectDto registerNewSubject(Project project, String subject, String schemaContent,
    boolean isEnablingKafkaService) throws KafkaException, SchemaException {
    validateSubject(subject, isEnablingKafkaService);
    Schema schema;
    try {
      schema = new Schema.Parser().parse(schemaContent);
    } catch (SchemaParseException e) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INVALID_AVRO_SCHEMA, Level.FINE,
        "schema=" + schemaContent);
    }
    //check if schema exists - return current id
    Optional<Subjects> optionalSubject =
      subjectsFacade.findSubjectByNameAndSchema(project, subject, schema.toString());
    if (optionalSubject.isPresent()) {
      return new SubjectDto(optionalSubject.get().getSchema().getId());
    }
    //check if schema compatible - return 409 of not
    if(!isCompatible(project, subject, schema)) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INCOMPATIBLE_AVRO_SCHEMA, Level.FINE,
        "Subject=" + subject + ", project=" + project.getName());
    }
    Integer latestVersion = subjectsFacade.findSubjectByName(project, subject)
      .stream()
      .map(Subjects::getVersion)
      .max(Integer::compareTo)
      .orElse(0);
  
    Schemas schemas = schemasController.addNewSchema(project, schema.toString());
    Integer id = subjectsFacade.insertNewSubject(project, subject, schemas, latestVersion + 1);
    return new SubjectDto(id);
  }
  
  public void validateSubject(String subject, boolean isEnablingKafkaService) throws KafkaException {
    validateSubjectNameAgainstBlacklist(subject, isEnablingKafkaService);
  }
  
  private void validateSubjectNameAgainstBlacklist(String subject, boolean isEnablingKafkaService)
    throws KafkaException {
    if(Settings.KAFKA_SUBJECT_BLACKLIST.contains(subject) &&
      // checks if the request doesn't come from ProjectController enabling Kafka Service
      !(subject.equals(Settings.INFERENCE_SCHEMANAME) && isEnablingKafkaService)){
      
      throw new KafkaException(RESTCodes.KafkaErrorCode.CREATE_SUBJECT_RESERVED_NAME, Level.FINE, "subject=" + subject);
    }
  }
  
  private boolean isCompatible(Project project, String subject, Schema schema) throws SchemaException {
    SchemaCompatibility sc = getSubjectOrProjectCompatibility(project, subject);
    if (sc.equals(SchemaCompatibility.NONE)) {
      return true;
    }
    SchemaValidator validator = getSchemaValidator(sc);
    List<Schema> previousSchemas = subjectsFacade.findSubjectByName(project, subject)
      .stream()
      .sorted(Comparator.comparing(Subjects::getVersion).reversed())
      .map(s -> new Schema.Parser().parse(s.getSchema().getSchema()))
      .collect(Collectors.toList());
    try {
      
      validator.validate(schema, previousSchemas);
    } catch (SchemaValidationException e) {
      return false;
    }
    
    return true;
  }
  
  private boolean isCompatible(Schema previousSchema, Schema schemaToTest, SchemaCompatibility sc) {
    if (sc == SchemaCompatibility.NONE) {
      return true;
    }
    
    SchemaValidator validator = getSchemaValidator(sc);
    try {
      validator.validate(schemaToTest, Collections.singleton(previousSchema));
    } catch (SchemaValidationException e) {
      return false;
    }
    return true;
  }
  
  private SchemaCompatibility getSubjectOrProjectCompatibility(Project project, String subject) throws SchemaException {
    Optional<SubjectsCompatibility> optional = subjectsCompatibilityFacade.getSubjectCompatibility(project, subject);
    if (optional.isPresent()) {
      return optional.get().getCompatibility();
    } else {
      return subjectsCompatibilityFacade.getProjectCompatibility(project).orElseThrow(() ->
        new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.FINE,
          "Project compatibility not found for project " + project.getName()))
        .getCompatibility();
    }
  }
  
  private SchemaValidator getSchemaValidator(SchemaCompatibility sc) {
    switch(sc) {
      case BACKWARD:
        return new SchemaValidatorBuilder().canReadStrategy().validateLatest();
      case BACKWARD_TRANSITIVE:
        new SchemaValidatorBuilder().canReadStrategy().validateAll();
      case FORWARD:
        return new SchemaValidatorBuilder().canBeReadStrategy().validateLatest();
      case FORWARD_TRANSITIVE:
        return new SchemaValidatorBuilder().canBeReadStrategy().validateAll();
      case FULL:
        return new SchemaValidatorBuilder().mutualReadStrategy().validateLatest();
      case FULL_TRANSITIVE:
        return new SchemaValidatorBuilder().mutualReadStrategy().validateAll();
      default:
        throw new IllegalArgumentException("Unknown schema compatibility " + sc.toString());
    }
  }
  
  public SubjectDto checkIfSchemaRegistered(Project project, String subject, String schemaContent) throws
    SchemaException {
    if (!subjectsFacade.getListOfSubjects(project).contains(subject)) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.FINE, "subject=" +
        subject);
    }
    Schema schema;
    try {
      schema = new Schema.Parser().parse(schemaContent);
    } catch (SchemaParseException e) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INVALID_AVRO_SCHEMA, Level.FINE,
        "schema=" + schemaContent);
    }
    Optional<Subjects> optional = subjectsFacade.findSubjectByNameAndSchema(project, subject, schema.toString());
    if (!optional.isPresent()) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.SCHEMA_NOT_FOUND, Level.FINE,
        "schema=" + schema.toString());
    }
    return new SubjectDto(optional.get());
  }
  
  public List<Integer> deleteSubject(Project project, String subject) throws SchemaException, KafkaException {
    validateSubject(subject, false);
    if (!projectTopicsFacade.findTopicsBySubject(project, subject).isEmpty()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_IN_USE, Level.FINE, "project=" + project.getName() +
        ", subject=" + subject);
    }
    List<Integer> versions = getSubjectVersions(project, subject);
    Integer deleted = subjectsFacade.deleteSubject(project, subject);
    if (versions.size() != deleted) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INTERNAL_SERVER_ERROR, Level.FINE, "error deleting " +
        "subject. versions=" + Arrays.toString(versions.toArray()) + ", but deleted " + deleted + " items.");
    }
    
    subjectsCompatibilityFacade
      .getSubjectCompatibility(project, subject)
      .ifPresent(sc -> subjectsCompatibilityFacade.remove(sc));
    return versions;
  }
  
  public CompatibilityCheckDto checkIfSchemaCompatible(Project project, String subject, String version,
    String schemaToTest) throws SchemaException {
    
    validateVersion(version);
    Schema schema;
    try {
      schema = new Schema.Parser().parse(schemaToTest);
    } catch (SchemaParseException e) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INVALID_AVRO_SCHEMA, Level.FINE,
        "schema=" + schemaToTest);
    }
    if (!subjectsFacade.getListOfSubjects(project).contains(subject)) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.FINE, "subject=" +
        subject);
    }
    SchemaCompatibility sc = getSubjectOrProjectCompatibility(project, subject);
    Optional<Subjects> optional;
    if (version.equals("latest")) {
      optional = subjectsFacade.findSubjectLatestVersion(project, subject);
    } else {
      optional = subjectsFacade.findSubjectByNameAndVersion(project, subject, Integer.valueOf(version));
    }
    
    if (!optional.isPresent()) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.VERSION_NOT_FOUND, Level.FINE, "project=" + project
        .getName() + ", subject=" + subject + ", version=" + version);
    }
    
    boolean isCompatible = isCompatible(new Schema.Parser().parse(optional.get().getSchema().getSchema()), schema, sc);
    return new CompatibilityCheckDto(isCompatible);
  }
  
  public Integer deleteSubjectsVersion(Project project, String subject, String version)
    throws SchemaException, KafkaException {
    validateSubject(subject, false);
    validateVersion(version);
    if (!subjectsFacade.getListOfSubjects(project).contains(subject)) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.SUBJECT_NOT_FOUND, Level.FINE, "subject=" +
        subject);
    }
    Optional<Subjects> optional;
    if (version.equals("latest")) {
      optional = subjectsFacade.findSubjectLatestVersion(project, subject);
    } else {
      optional = subjectsFacade.findSubjectByNameAndVersion(project, subject, Integer.valueOf(version));
    }
  
    if (!optional.isPresent()) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.VERSION_NOT_FOUND, Level.FINE, "project=" + project
        .getName() + ", subject=" + subject + ", version=" + version);
    }
    
    Integer versionToDelete = optional.get().getVersion();
    if (!projectTopicsFacade.findTopiscBySubjectAndVersion(project, subject, versionToDelete).isEmpty()) {
      throw new KafkaException(RESTCodes.KafkaErrorCode.SCHEMA_IN_USE, Level.FINE, "project=" + project.getName() +
        ", subject=" + subject + ", version=" + versionToDelete);
    }
    subjectsFacade.remove(optional.get());
    subjectsCompatibilityFacade.getSubjectCompatibility(project, subject)
      .ifPresent(sc -> subjectsCompatibilityFacade.remove(sc));
    return versionToDelete;
  }
  
  private void validateVersion(String version) throws SchemaException {
    // check if version is "latest"
    if (version.equals("latest")) {
      return;
    }
    
    //check if version is a number
    try {
      new BigInteger(version);
    } catch (NumberFormatException e) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INVALID_VERSION, Level.FINE,
        "version=" + version);
    }
    
    //check if version is between 0 and 2^31-1
    if (new BigInteger(version).compareTo(BigInteger.ONE) < 0 || new BigInteger(version)
      .compareTo(new BigInteger(String.valueOf(Integer.MAX_VALUE))) > 0) {
      throw new SchemaException(RESTCodes.SchemaRegistryErrorCode.INVALID_VERSION, Level.FINE,
        "version=" + version);
    }
  }
}
