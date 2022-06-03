/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.serving;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.kafka.ProjectTopics;
import io.hops.hopsworks.persistence.entity.kafka.schemas.Subjects;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.BatchingConfiguration;
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility functions for the serving service, contains common functionality between Localhost and K8 serving
 */
@Stateless
public class ServingUtil {
  
  @EJB
  private ServingFacade servingFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private ProjectTopicsFacade projectTopicsFacade;
  @EJB
  private Settings settings;
  
  private final HashSet<String> MODEL_FILE_EXTS =
    Stream.of(".joblib", ".pkl", ".pickle").collect(Collectors.toCollection(HashSet::new));
  
  /**
   * Validates user input before creating or updating a serving. This method contains the common input validation
   * between different serving types and then delegates to serving-type-specific controllers to do validation specific
   * to the serving type.
   *
   * @param servingWrapper contains the user-data to validate
   * @param project the project where the serving resides
   * @throws ServingException
   * @throws java.io.UnsupportedEncodingException
   */
  public void validateUserInput(ServingWrapper servingWrapper, Project project) throws ServingException,
      UnsupportedEncodingException {

    Serving serving = servingWrapper.getServing();
    Serving dbServing = servingFacade.findByProjectAndName(project, serving.getName());
    
    setDefaultRequestBatching(serving);
    
    // NOTE: order matters
    validateServingName(serving, dbServing);
    validateModelPath(serving);
    validateModelVersion(serving);
    validateModelServer(serving, dbServing);
    validateModelName(serving);
    validateServingTool(serving);
    validatePredictor(project, serving);
    validateKafkaTopicSchema(project, serving, servingWrapper.getKafkaTopicDTO());
    validateInstances(serving);
    validateBatchingConfiguration(serving);
  }

  private void validateBatchingConfiguration(Serving serving) throws ServingException {
    BatchingConfiguration batchingConfiguration = serving.getBatchingConfiguration();
    if (batchingConfiguration != null && batchingConfiguration.isBatchingEnabled() &&
        (batchingConfiguration.getMaxBatchSize() != null
        || batchingConfiguration.getMaxLatency() != null || batchingConfiguration.getTimeout() != null)) {
      throw new ServingException(RESTCodes.ServingErrorCode.REQUEST_BATCHING_NOT_SUPPORTED, Level.FINE,
          "Fine-grained request batching is only supported in KServe deployments");
    }
  }
  
  private void validateServingName(Serving serving, Serving dbServing) throws ServingException {
    if (Strings.isNullOrEmpty(serving.getName())) {
      throw new IllegalArgumentException("Serving name not provided");
    } else if (serving.getName().contains(" ")) {
      throw new IllegalArgumentException("Serving name cannot contain spaces");
    }
  
    // Check for duplicated entries
    if (dbServing != null && !dbServing.getId().equals(serving.getId())) {
      // There is already an entry for this project
      throw new ServingException(RESTCodes.ServingErrorCode.DUPLICATEDENTRY, Level.FINE);
    }
  
    // Check serving name follows allowed regex as required by the InferenceResource to use it as a
    // REST endpoint
    Pattern urlPattern = Pattern.compile("[a-zA-Z0-9]+");
    Matcher urlMatcher = urlPattern.matcher(serving.getName());
    if(!urlMatcher.matches()){
      throw new IllegalArgumentException("Serving name must follow regex: \"[a-zA-Z0-9]+\"");
    }
  }
  
  private void validateModelPath(Serving serving) {
    if (serving.getModelPath() == null) {
      throw new IllegalArgumentException("Model path not provided");
    }
  }
  
  private void validateModelVersion(Serving serving) {
    if (serving.getModelVersion() == null) {
      throw new IllegalArgumentException("Model version not provided");
    }
  }
  
  private void validateModelServer(Serving serving, Serving dbServing) throws ServingException {
    if (serving.getModelServer() == null) {
      throw new IllegalArgumentException("Model server not provided or unsupported");
    }
    if (dbServing != null && dbServing.getModelServer() != serving.getModelServer()) {
      throw new ServingException(RESTCodes.ServingErrorCode.UPDATE_MODEL_SERVER_ERROR, Level.SEVERE);
    }
    if (serving.getModelServer() == ModelServer.TENSORFLOW_SERVING) {
      validateTFServingUserInput(serving); // e.g., tensorflow standard path
    }
    if (serving.getModelServer() == ModelServer.PYTHON) {
      validatePythonUserInput(serving); // e.g., model file, python scripts
    }
  }
  
  private void validateModelName(Serving serving) {
    if (serving.getModelName() == null) {
      // after validating the model path
      setDefaultModelName(serving);
    } else {
      if (!serving.getModelPath().endsWith(serving.getModelName())) {
        throw new IllegalArgumentException("Model name does not match the model path");
      }
    }
  }
  
  private void validateServingTool(Serving serving) throws ServingException {
    // Serving-tool-specific validations
    if (serving.getServingTool() == null) {
      throw new IllegalArgumentException("Serving tool not provided or invalid");
    }
  }
  
  private void validateInstances(Serving serving) {
    if (serving.getInstances() == null) {
      throw new IllegalArgumentException("Number of instances not provided");
    }
  }

  private void validatePythonScript(Project project, String scriptPath, List<String> extensions, String component)
    throws ServingException, UnsupportedEncodingException {
    
    // Check that the script name is valid and exists
    String scriptName = Utils.getFileName(scriptPath);
    if(extensions.stream().noneMatch(scriptName::endsWith)){
      throw new IllegalArgumentException(StringUtils.capitalize(component) + " script should have a valid extension: "
        + String.join(", ", extensions));
    }
    //Remove hdfs:// if it is in the path
    String hdfsPath = Utils.prepPath(scriptPath);
    if(!inodeController.existsPath(hdfsPath)){
      throw new ServingException(RESTCodes.ServingErrorCode.SCRIPT_NOT_FOUND, Level.SEVERE,
        StringUtils.capitalize(component) + " script does not exist");
    }
    
    //Check that python environment is activated
    if(project.getPythonEnvironment() == null){
      throw new ServingException(RESTCodes.ServingErrorCode.PYTHON_ENVIRONMENT_NOT_ENABLED, Level.SEVERE, null);
    }
  }

  private void validateTFServingUserInput(Serving serving) throws ServingException {
    // Check that the model path respects the TensorFlow standard
    try {
      List<Inode> children = inodeController.getChildren(serving.getModelVersionPath());
      if (children.stream().noneMatch(inode -> inode.getInodePK().getName().equals("variables")) ||
        children.stream().noneMatch(inode -> inode.getInodePK().getName().contains(".pb"))) {
        throw new ServingException(RESTCodes.ServingErrorCode.MODEL_FILES_STRUCTURE_NOT_VALID, Level.FINE, "Model " +
          "path does not respect the Tensorflow standard");
      }
    } catch (FileNotFoundException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.MODEL_PATH_NOT_FOUND, Level.FINE, null);
    }
  }
  
  private void validatePythonUserInput(Serving serving) throws IllegalArgumentException, ServingException {
    // Check model files and/or python scripts
    try {
      List<Inode> children = inodeController.getChildren(serving.getModelVersionPath());
      long modelFiles = children.stream().filter(c -> {
        String name = c.getInodePK().getName();
        return MODEL_FILE_EXTS.stream().anyMatch(name::endsWith);
      }).count();
      if (modelFiles == 0) {
        // if no model files found
        if (children.stream().noneMatch(c -> c.getInodePK().getName().endsWith(".py"))) {
          // and no python script
          throw new ServingException(RESTCodes.ServingErrorCode.MODEL_FILES_STRUCTURE_NOT_VALID, Level.FINE, "Model" +
            " path requires either a python script or model file (i.e., joblib or pickle files)");
        }
      }
    } catch (FileNotFoundException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.MODEL_PATH_NOT_FOUND, Level.FINE, null);
    }
  }

  private void validatePredictor(Project project, Serving serving) throws UnsupportedEncodingException,
      ServingException {
    if (serving.getPredictor() != null) {
      // if predictor selected
      if (serving.getModelServer() == ModelServer.TENSORFLOW_SERVING) {
        // and tensorflow serving
        throw new ServingException(RESTCodes.ServingErrorCode.PREDICTOR_NOT_SUPPORTED, Level.FINE, "Predictors are " +
          "not supported with Tensorflow Serving");
      }
      String path = serving.getPredictor();
      validatePythonScript(project, path, Arrays.asList(".py"), "predictor");
    } else {
      // if no predictor selected
      if (serving.getModelServer() == ModelServer.PYTHON) {
        // and python default deployment)
        throw new ServingException(RESTCodes.ServingErrorCode.MODEL_ARTIFACT_NOT_VALID, Level.FINE, "Default " +
          "deployments require a predictor");
      }
    }
  }

    
  private void validateKafkaTopicSchema(Project project, Serving serving, TopicDTO topic) throws ServingException {
    // if an existing topic, check schema
    if (topic != null && !topic.getName().equals("NONE") && !topic.getName().equals("CREATE")) {
      ProjectTopics projectTopic = projectTopicsFacade.findTopicByNameAndProject(project, topic.getName())
        .orElseThrow(() -> new ServingException(RESTCodes.ServingErrorCode.KAFKA_TOPIC_NOT_FOUND, Level.SEVERE, null));
      
      Subjects subjects = projectTopic.getSubjects();
      if (!subjects.getSubject().equalsIgnoreCase(Settings.INFERENCE_SCHEMANAME)) {
        throw new ServingException(RESTCodes.ServingErrorCode.KAFKA_TOPIC_NOT_VALID, Level.FINE, "Inference logging" +
          " requires a Kafka topic with schema '" + Settings.INFERENCE_SCHEMANAME + "'");
      }
    }
  }

  private void setDefaultModelName(Serving serving) {
    if (serving.getModelName() != null) return;
    String modelPath = serving.getModelPath();
    String[] split = modelPath.split("/");
    serving.setModelName(split[4]);
  }

  private void setDefaultRequestBatching(Serving serving) {
    BatchingConfiguration batchingConfiguration = serving.getBatchingConfiguration();
    if (batchingConfiguration == null) {
      batchingConfiguration = new BatchingConfiguration();
      batchingConfiguration.setBatchingEnabled(false);
      serving.setBatchingConfiguration(batchingConfiguration);
    } else if (batchingConfiguration.isBatchingEnabled() == null) {
      batchingConfiguration.setBatchingEnabled(false);
    }

  }

}
