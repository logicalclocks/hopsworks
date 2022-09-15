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
import io.hops.hopsworks.persistence.entity.serving.DockerResourcesConfiguration;
import io.hops.hopsworks.persistence.entity.serving.InferenceLogging;
import io.hops.hopsworks.persistence.entity.serving.ModelFramework;
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
   * @param servingWrapper
   *   contains the user-data to validate
   * @param project
   *   the project where the serving resides
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
    validateModelFramework(serving);
    validateModelServer(serving, dbServing);
    validateModelName(serving);
    validateServingTool(serving);
    validateArtifact(project, serving);
    validatePredictor(project, serving);
    validateTransformer(project, serving);
    validateKafkaTopicSchema(project, serving, servingWrapper.getKafkaTopicDTO());
    validateInferenceLogging(serving, servingWrapper.getKafkaTopicDTO());
    validateInstances(serving);
    validateResources(serving);
    validateBatchingConfiguration(serving);
  }
  
  private void validateBatchingConfiguration(Serving serving) throws ServingException {
    BatchingConfiguration batchingConfiguration = serving.getBatchingConfiguration();
    if (batchingConfiguration != null && batchingConfiguration.isBatchingEnabled()) {
      if (serving.getModelServer() == ModelServer.PYTHON && serving.getServingTool() != ServingTool.KSERVE) {
        throw new ServingException(RESTCodes.ServingErrorCode.REQUEST_BATCHING_NOT_SUPPORTED, Level.SEVERE, "Request " +
          "batching is not supported in Python deployments without KServe");
      } else if (serving.getServingTool() != ServingTool.KSERVE && (batchingConfiguration.getMaxBatchSize() != null
        || batchingConfiguration.getMaxLatency() != null || batchingConfiguration.getTimeout() != null)) {
        throw new ServingException(RESTCodes.ServingErrorCode.REQUEST_BATCHING_NOT_SUPPORTED, Level.FINE,
          "Fine-grained request batching is only supported in KServe deployments");
      }
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
      throw new ServingException(RESTCodes.ServingErrorCode.DUPLICATED_ENTRY, Level.FINE);
    }
    
    // Check serving name follows allowed regex as required by the InferenceResource to use it as a
    // REST endpoint
    Pattern urlPattern = Pattern.compile("[a-zA-Z0-9]+");
    Matcher urlMatcher = urlPattern.matcher(serving.getName());
    if (!urlMatcher.matches()) {
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
  
  private void validateModelFramework(Serving serving) {
    if (serving.getModelFramework() == null) {
      throw new IllegalArgumentException("Model framework not provided");
    }
    if (serving.getModelFramework() == ModelFramework.TENSORFLOW
      && serving.getModelServer() != ModelServer.TENSORFLOW_SERVING) {
      throw new IllegalArgumentException("Tensorflow models require Tensorflow Serving server");
    } else if (serving.getModelFramework() == ModelFramework.TORCH && serving.getModelServer() != ModelServer.PYTHON) {
      throw new IllegalArgumentException("Torch models require Python model server");
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
    if (serving.getServingTool() == ServingTool.KSERVE) {
      validateKServeUserInput(serving);
    }
  }
  
  private void validateInstances(Serving serving) {
    if (serving.getInstances() == null) {
      throw new IllegalArgumentException("Number of instances not provided");
    }
    if (serving.getTransformer() == null && serving.getTransformerInstances() != null) {
      throw new IllegalArgumentException("Number of transformer instances cannot be provided without a transformer");
    }
    if (serving.getTransformer() != null && serving.getTransformerInstances() == null) {
      throw new IllegalArgumentException("Number of transformer instances must be provided with a transformer");
    }
    if (settings.getKubeServingMaxNumInstances() > -1) {
      if (serving.getInstances() > settings.getKubeServingMaxNumInstances()) {
        throw new IllegalArgumentException(String.format("Number of instances exceeds maximum number of instances " +
          "allowed of %s instances", settings.getKubeServingMaxNumInstances()));
      }
      if (serving.getTransformerInstances() != null &&
        serving.getTransformerInstances() > settings.getKubeServingMaxNumInstances()) {
        throw new IllegalArgumentException(String.format("Number of transformer instances exceeds maximum number of " +
          "instances allowed of %s instances", settings.getKubeServingMaxNumInstances()));
      }
    }
    if (settings.getKubeServingMinNumInstances() == 0 && serving.getServingTool() == ServingTool.KSERVE) {
      if (serving.getInstances() != 0) {
        throw new IllegalArgumentException(String.format("Scale-to-zero is required for KServe deployments in this " +
          "cluster. Please, set the number of instances to 0."));
      }
      if (serving.getTransformerInstances() != null && serving.getTransformerInstances() != 0) {
        throw new IllegalArgumentException(String.format("Scale-to-zero is required for KServe deployments in this " +
          "cluster. Please, set the number of transformer instances to 0."));
      }
    }
  }
  
  private void validateResources(Serving serving) {
    if (!settings.getKubeInstalled()) {
      return; // validate resources only in kubernetes deployments
    }
    DockerResourcesConfiguration requests = serving.getPredictorResources().getRequests();
    DockerResourcesConfiguration limits = serving.getPredictorResources().getLimits();
    double maxCores = settings.getKubeServingMaxCoresAllocation();
    int maxMemory = settings.getKubeServingMaxMemoryAllocation();
    int maxGpus = settings.getKubeServingMaxGpusAllocation();
    
    if (requests != null) {
      if (maxCores > -1 && (requests.getCores() < 0 || requests.getCores() > maxCores)) {
        throw new IllegalArgumentException(String.format("Configured cores allocation of %s exceeds maximum core " +
          "allocation allowed of %s cores", requests.getCores(), maxCores));
      }
      if (maxMemory > -1 && (requests.getMemory() < 0 || requests.getMemory() > maxMemory)) {
        throw new IllegalArgumentException(String.format("Configured memory allocation of %s exceeds maximum memory " +
          "allocation allowed of %s MB", requests.getMemory(), maxMemory));
      }
      if (maxGpus > -1 && (requests.getGpus() < 0 || requests.getGpus() > maxGpus)) {
        throw new IllegalArgumentException(String.format("Configured cores allocation of %s exceeds maximum gpu " +
          "allocation allowed of %s gpus", requests.getGpus(), maxGpus));
      }
    }
    if (limits != null) {
      if (maxCores > -1 && (limits.getCores() < 0 || limits.getCores() > maxCores)) {
        throw new IllegalArgumentException(String.format("Configured cores allocation of %s exceeds maximum core " +
          "allocation allowed of %s cores", limits.getCores(), maxCores));
      }
      if (maxMemory > -1 && (limits.getMemory() < 0 || limits.getMemory() > maxMemory)) {
        throw new IllegalArgumentException(String.format("Configured memory allocation of %s exceeds maximum memory " +
          "allocation allowed of %s MB", limits.getMemory(), maxMemory));
      }
      if (maxGpus > -1 && (limits.getGpus() < 0 || limits.getGpus() > maxGpus)) {
        throw new IllegalArgumentException(String.format("Configured gpus allocation of %s exceeds maximum gpu " +
          "allocation allowed of %s gpus", limits.getGpus(), maxGpus));
      }
    }
  
    if (requests != null && limits != null) {
      // compare requests with limits
      if (limits.getCores() > -1 && (requests.getCores() < 0 || requests.getCores() > limits.getCores())) {
        throw new IllegalArgumentException(String.format("Requested cores allocation of %s exceeds maximum core " +
          "allocation of %s cores", requests.getCores(), limits.getCores()));
      }
      if (limits.getMemory() > -1 && (requests.getMemory() < 0 || requests.getMemory() > limits.getMemory())) {
        throw new IllegalArgumentException(String.format("Requested memory allocation of %s exceeds maximum memory " +
          "allocation of %s MB", requests.getMemory(), limits.getMemory()));
      }
      if (limits.getGpus() > -1 && (requests.getGpus() < 0 || requests.getGpus() > limits.getGpus())) {
        throw new IllegalArgumentException(String.format("Requested gpus allocation of %s exceeds maximum gpu " +
          "allocation of %s gpus", requests.getGpus(), limits.getGpus()));
      }
    }
  }
  
  private void validatePythonScript(Project project, String scriptPath, List<String> extensions, String component)
    throws ServingException, UnsupportedEncodingException {
    
    // Check that the script name is valid and exists
    String scriptName = Utils.getFileName(scriptPath);
    if (extensions.stream().noneMatch(scriptName::endsWith)) {
      throw new IllegalArgumentException(StringUtils.capitalize(component) + " script should have a valid extension: "
        + String.join(", ", extensions));
    }
    //Remove hdfs:// if it is in the path
    String hdfsPath = Utils.prepPath(scriptPath);
    if (!inodeController.existsPath(hdfsPath)) {
      throw new ServingException(RESTCodes.ServingErrorCode.SCRIPT_NOT_FOUND, Level.SEVERE,
        StringUtils.capitalize(component) + " script does not exist");
    }
    
    //Check that python environment is activated
    if (project.getPythonEnvironment() == null) {
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
      if (modelFiles > 1) {
        if (serving.getServingTool() == ServingTool.KSERVE && serving.getPredictor() == null) {
          // if more than one model file in a KServe deployment without predictor
          throw new ServingException(RESTCodes.ServingErrorCode.MODEL_FILES_STRUCTURE_NOT_VALID, Level.FINE, "Model " +
            "path cannot contain more than one model file (i.e., joblib or pickle files)");
        }
      } else if (modelFiles == 0) {
        // if no model files found
        if (children.stream().noneMatch(c -> c.getInodePK().getName().endsWith(".py"))) {
          // and no python script
          throw new ServingException(RESTCodes.ServingErrorCode.MODEL_FILES_STRUCTURE_NOT_VALID, Level.FINE, "Model" +
            " path requires either a python script or model file (i.e., joblib or pickle files)");
        }
        if (serving.getServingTool() == ServingTool.KSERVE && serving.getPredictor() == null) {
          // and KServe without predictor script selected
          throw new ServingException(RESTCodes.ServingErrorCode.MODEL_FILES_STRUCTURE_NOT_VALID, Level.FINE,
            "KServe deployments without predictor script require a model file");
        }
      }
    } catch (FileNotFoundException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.MODEL_PATH_NOT_FOUND, Level.FINE, null);
    }
  }
  
  private void validateKServeUserInput(Serving serving)
    throws ServingException {
    if (!settings.getKubeInstalled()) {
      throw new ServingException(RESTCodes.ServingErrorCode.KUBERNETES_NOT_INSTALLED, Level.SEVERE, "Serving tool not" +
        " supported. KServe requires Kubernetes to be installed");
    }
    
    if (!settings.getKubeKServeInstalled()) {
      throw new ServingException(RESTCodes.ServingErrorCode.KSERVE_NOT_ENABLED, Level.SEVERE, "Serving tool not " +
        "supported");
    }
    
    // Service name is used as DNS subdomain. It must consist of lower case alphanumeric characters, '-' or '.', and
    // must start and end with an alphanumeric character. (e.g. 'example.com', regex used for validation is '[a-z0-9]
    // ([-a-z0-9]*[a-z0-9])?(\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*').
    Pattern namePattern = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*");
    Matcher nameMatcher = namePattern.matcher(serving.getName());
    if (!nameMatcher.matches()) {
      throw new IllegalArgumentException("Serving name must consist of lower case alphanumeric characters, '-' or '" +
        ".', and start and end with an alphanumeric character");
    }
  }
  
  private void validateArtifact(Project project, Serving serving) throws ServingException {
    if (!settings.getKubeInstalled()) {
      // If Kubernetes is not installed
      if (serving.getArtifactVersion() != null) {
        // and artifact version is not null
        if (serving.getArtifactVersion() == -1) {
          // if CREATE, set artifact version to null
          serving.setArtifactVersion(null);
        } else {
          // if != -1, not supported
          throw new ServingException(RESTCodes.ServingErrorCode.KUBERNETES_NOT_INSTALLED, Level.SEVERE, "Artifacts " +
            "only supported in Kubernetes deployments");
        }
      }
      return; // no more validations needed
    }
    
    if (serving.getServingTool() == ServingTool.DEFAULT && serving.getModelServer() == ModelServer.TENSORFLOW_SERVING
      && serving.getArtifactVersion() != null && serving.getArtifactVersion() > 0) {
      throw new ServingException(RESTCodes.ServingErrorCode.MODEL_ARTIFACT_NOT_VALID, Level.FINE, "Default " +
        "deployments with Tensorflow Serving only support MODEL-ONLY artifacts");
    }
    
    // get artifact input combination code
    int artifactInputCode = getArtifactInputCode(serving);
    
    boolean newArtifact = serving.getArtifactVersion() == null || serving.getArtifactVersion() == -1;
    if (serving.getId() == null) { // new serving
      if (!newArtifact) { // with existing artifact
        if (serving.getArtifactVersion() == 0) { // MODEL-ONLY artifact version
          if (artifactInputCode != 0) {
            throw new ServingException(RESTCodes.ServingErrorCode.MODEL_ARTIFACT_OPERATION_ERROR, Level.FINE,
              "Predictors and transformers cannot be used in MODEL-ONLY artifacts");
          }
        } else { // existing artifact version
          if (artifactInputCode == 0) {
            throw new ServingException(RESTCodes.ServingErrorCode.MODEL_ARTIFACT_OPERATION_ERROR, Level.FINE,
              "Other than MODEL-ONLY artifacts require a predictor or transformer");
          }
          if (artifactInputCode != 2) {
            throw new ServingException(RESTCodes.ServingErrorCode.MODEL_ARTIFACT_OPERATION_ERROR, Level.FINE,
              "Existing artifacts cannot be modified. To change predictors or transformers, create a new artifact");
          }
        }
      }
    } else { // update serving
      if (newArtifact) { // CREATE artifact
        if (artifactInputCode == 3) {
          // if one of predictor or transformer is new and the other existing, use the absolute path of the
          // existing one. This is needed to be able to copy the script into the new artifact.
          setArtifactAbsolutePaths(project, serving);
        }
      } else {
        if (serving.getArtifactVersion() == 0) { // MODEL-ONLY artifact
          if (artifactInputCode != 0) {
            throw new ServingException(RESTCodes.ServingErrorCode.MODEL_ARTIFACT_OPERATION_ERROR, Level.FINE,
              "Predictors and transformers cannot be used in MODEL-ONLY artifacts");
          }
        } else { // existing artifact
          if (artifactInputCode != 2) {
            throw new ServingException(RESTCodes.ServingErrorCode.MODEL_ARTIFACT_OPERATION_ERROR, Level.FINE,
              "Existing artifacts cannot be modified. To change predictors or transformers, create a new artifact");
          }
        }
      }
    }
  }
  
  private int getArtifactInputCode(Serving serving) {
    // 0: no pr/tr
    // 1: at least one new, the rest new or null
    // 2: at least one existing, the rest existing or null
    // 3: mixed, one new and the other existing
    int artifactInputCode = 0;
    if (serving.getPredictor() == null) {
      if (serving.getTransformer() != null) {
        artifactInputCode = serving.getTransformer().contains("/") ? 1 : 2;
      }
    } else {
      artifactInputCode = serving.getPredictor().contains("/") ? 1 : 2;
      if (serving.getTransformer() != null) {
        if (serving.getTransformer().contains("/")) {
          artifactInputCode = artifactInputCode == 1 ? 1 : 3;
        } else {
          artifactInputCode = artifactInputCode == 1 ? 3 : 2;
        }
      }
    }
    return artifactInputCode;
  }
  
  private void setArtifactAbsolutePaths(Project project, Serving serving) {
    Serving dbServing = servingFacade.findByProjectAndName(project, serving.getName());
    if (!serving.getPredictor().contains("/")) {
      Path newPath = Paths.get(dbServing.getArtifactVersionPath(), "predictor-" + serving.getPredictor());
      serving.setPredictor(newPath.toString());
    }
    if (!serving.getTransformer().contains("/")) {
      Path newPath = Paths.get(dbServing.getArtifactVersionPath(), "transformer-" + serving.getTransformer());
      serving.setTransformer(newPath.toString());
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
      // build predictor script path
      String path = "";
      if (serving.getArtifactVersion() != null && serving.getArtifactVersion() > 0) {
        // if existent artifact
        path += serving.getArtifactVersionPath() + "/" + "predictor-";
      }
      path += serving.getPredictor();
      validatePythonScript(project, path, Arrays.asList(".py"), "predictor");
    } else {
      // if no predictor selected
      if (serving.getServingTool() == ServingTool.DEFAULT && serving.getModelServer() == ModelServer.PYTHON) {
        // and python default deployment)
        throw new ServingException(RESTCodes.ServingErrorCode.MODEL_ARTIFACT_NOT_VALID, Level.FINE, "Default " +
          "deployments require a predictor");
      }
      if (serving.getModelFramework() == ModelFramework.PYTHON) {
        throw new IllegalArgumentException("Predictor scripts are required in custom python deployments");
      } else if (serving.getModelFramework() == ModelFramework.TORCH) {
        throw new IllegalArgumentException("Predictor scripts are required in torch deployments");
      }
    }
  }
  
  private void validateTransformer(Project project, Serving serving)
    throws UnsupportedEncodingException, ServingException {
    if (serving.getTransformer() == null) {
      return;
    }
    
    if (serving.getServingTool() != ServingTool.KSERVE) {
      throw new ServingException(RESTCodes.ServingErrorCode.TRANSFORMER_NOT_SUPPORTED, Level.FINE, "Transformers " +
        "are only supported on KServe deployments");
    }
    String path = "";
    if (serving.getArtifactVersion() != null && serving.getArtifactVersion() > 0) {
      // if existent artifact
      path += serving.getArtifactVersionPath() + "/" + "transformer-";
    }
    path += serving.getTransformer();
    validatePythonScript(project, path, Arrays.asList(".py", ".ipynb"), "transformer");
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
      if (serving.getServingTool() == ServingTool.DEFAULT && subjects.getVersion() >= 4) {
        throw new ServingException(RESTCodes.ServingErrorCode.KAFKA_TOPIC_NOT_VALID, Level.FINE, "Inference logging" +
          " in default deployments requires schema version 3 or lower");
      }
      if (serving.getServingTool() == ServingTool.KSERVE && subjects.getVersion() < 4) {
        throw new ServingException(RESTCodes.ServingErrorCode.KAFKA_TOPIC_NOT_VALID, Level.FINE, "Inference logging" +
          " in KServe deployments requires schema version 4 or greater");
      }
    }
  }
  
  private void validateInferenceLogging(Serving serving, TopicDTO topic) throws ServingException {
    boolean withKafkaTopic = topic != null && !topic.getName().equals("NONE");
    
    if (!withKafkaTopic) {
      if (serving.getInferenceLogging() != null) {
        throw new IllegalArgumentException("Inference logger mode cannot be provided without a Kafka topic");
      }
      return; // no more validations needed
    }
    
    if (serving.getInferenceLogging() == null) {
      throw new IllegalArgumentException("A valid inference logger mode must be provided with a Kafka topic");
    }
    
    if (serving.getServingTool() != ServingTool.KSERVE) {
      if (serving.getInferenceLogging() != InferenceLogging.ALL) {
        throw new ServingException(RESTCodes.ServingErrorCode.FINEGRAINED_INF_LOGGING_NOT_SUPPORTED, Level.FINE,
          "Fine-grained inference logging only supported in KServe deployments");
      }
    }
  }
  
  private void setDefaultRequestBatching(Serving serving) {
    BatchingConfiguration batchingConfiguration = serving.getBatchingConfiguration();
    if (batchingConfiguration == null) {
      batchingConfiguration = new BatchingConfiguration();
      batchingConfiguration.setBatchingEnabled(false);
      serving.setBatchingConfiguration(batchingConfiguration);
    } else if (serving.getServingTool() == ServingTool.KSERVE && (batchingConfiguration.getTimeout() != null
      || batchingConfiguration.getMaxBatchSize() != null || batchingConfiguration.getMaxLatency() != null)) {
      //enable the batching configuration if any numbers are set
      batchingConfiguration.setBatchingEnabled(true);
      serving.setBatchingConfiguration(batchingConfiguration);
    } else if (batchingConfiguration.isBatchingEnabled() == null && (batchingConfiguration.getTimeout() == null
      || batchingConfiguration.getMaxBatchSize() == null || batchingConfiguration.getMaxLatency() == null)) {
      batchingConfiguration.setBatchingEnabled(false);
    }
  }
  
  private void setDefaultModelName(Serving serving) {
    if (serving.getModelName() != null) {
      return;
    }
    String modelPath = serving.getModelPath();
    String[] split = modelPath.split("/");
    serving.setModelName(split[4]);
  }
}
