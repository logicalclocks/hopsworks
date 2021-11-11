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

package io.hops.hopsworks.api.serving;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.InferenceLogging;
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private Settings settings;
  
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
    
    // Check that the modelName is present
    if (Strings.isNullOrEmpty(serving.getName())) {
      throw new IllegalArgumentException("Serving name not provided");
    } else if (serving.getName().contains(" ")) {
      throw new IllegalArgumentException("Serving name cannot contain spaces");
    }
    // Check that the modelPath is present
    if (Strings.isNullOrEmpty(serving.getModelPath())) {
      throw new IllegalArgumentException("Model path not provided");
    } else {
      // Format model path (e.g remove duplicated '/')
      String formattedModelPath = Paths.get(serving.getModelPath()).toString();
      serving.setModelPath(formattedModelPath);
    }
    if (serving.getModelVersion() == null) {
      throw new IllegalArgumentException("Model version not provided");
    }
    if (serving.getInstances() == null) {
      throw new IllegalArgumentException("Number of instances not provided");
    }
    // Check for duplicated entries
    checkDuplicates(project, servingWrapper);
    //Validate that serving name follows allowed regex as required by the InferenceResource to use it as a
    //Rest Endpoint
    Pattern urlPattern = Pattern.compile("[a-zA-Z0-9]+");
    Matcher urlMatcher = urlPattern.matcher(serving.getName());
    if(!urlMatcher.matches()){
      throw new IllegalArgumentException("Serving name must follow regex: \"[a-zA-Z0-9]+\"");
    }
    // Model-server-specific validations
    if (serving.getModelServer() == null) {
      throw new IllegalArgumentException("Model server not provided or unsupported");
    }
    if (serving.getModelServer() == ModelServer.TENSORFLOW_SERVING) {
      validateTfUserInput(serving);
    }
    if (serving.getModelServer() == ModelServer.FLASK) {
      validatePythonUserInput(project, serving.getModelPath());
    }
  
    // Serving-tool-specific validations
    if (serving.getServingTool() == null) {
      throw new IllegalArgumentException("Serving tool not provided or invalid");
    }
    if (serving.getServingTool() == ServingTool.KFSERVING) {
      validateKFServingUserInput(project, serving);
    }
  
    // Transformer
    if (serving.getTransformer() != null) {
      validateTransformer(project, serving);
    }
    
    // Artifact validations
    validateArtifact(serving);
    
    // Inference logging validations
    validateInferenceLogging(servingWrapper, serving);
    
    // Requested instances
    validateInstances(serving);
  }
  
  /**
   * Validates user data for creating or updating a Python Serving Instance
   *
   * @param project the project to create the serving for
   * @param scriptPath path to python script
   * @throws ServingException if the python environment is not activated for the project
   * @throws java.io.UnsupportedEncodingException
   */
  public void validatePythonUserInput(Project project, String scriptPath) throws ServingException,
    UnsupportedEncodingException {
    validatePythonUserInput(project, scriptPath, Collections.singletonList(".py"));
  }
  
  /**
   * Validates user data for creating or updating a Python Serving Instance
   *
   * @param project the project to create the serving for
   * @param scriptPath path to python script
   * @param extensions file extensions
   * @throws ServingException if the python environment is not activated for the project
   * @throws java.io.UnsupportedEncodingException
   */
  public void validatePythonUserInput(Project project, String scriptPath, List<String> extensions)
    throws ServingException, UnsupportedEncodingException {

    // Check that the script name is valid and exists
    String scriptName = Utils.getFileName(scriptPath);
    if(extensions.stream().noneMatch(scriptName::endsWith)){
      throw new IllegalArgumentException("Script name should be a valid python script name: " + String.join(", ",
        extensions));
    }
    //Remove hdfs:// if it is in the path
    String hdfsPath = Utils.prepPath(scriptPath);
    if(!inodeController.existsPath(hdfsPath)){
      throw new IllegalArgumentException("Python script path does not exist in HDFS");
    }
    
    //Check that python environment is activated
    if(project.getPythonEnvironment() == null){
      throw new ServingException(RESTCodes.ServingErrorCode.PYTHON_ENVIRONMENT_NOT_ENABLED, Level.SEVERE, null);
    }
  }
  
  /**
   * Validates user data for creating or updating a KFServing Serving Instance
   *
   * @param serving the user data
   * @throws ServingException
   */
  public void validateKFServingUserInput(Project project, Serving serving)
    throws ServingException, UnsupportedEncodingException {
    if (!settings.getKubeInstalled()) {
      throw new ServingException(RESTCodes.ServingErrorCode.KUBERNETES_NOT_INSTALLED, Level.SEVERE, "Serving tool not" +
        " supported. KFServing requires Kubernetes to be installed");
    }
  
    if (!settings.getKubeKFServingInstalled()) {
      throw new ServingException(RESTCodes.ServingErrorCode.KFSERVING_NOT_ENABLED, Level.SEVERE, "Serving tool not " +
        "supported");
    }
    
    if (serving.getModelServer() == ModelServer.FLASK) {
      throw new IllegalArgumentException("KFServing not supported for SKLearn models");
    }
    
    if (serving.isBatchingEnabled()) {
      throw new IllegalArgumentException("Request batching is not supported in KFServing deployments");
    }
  
    // Service name is used as DNS subdomain. It must consist of lower case alphanumeric characters, '-' or '.', and
    // must start and end with an alphanumeric character. (e.g. 'example.com', regex used for validation is '[a-z0-9]
    // ([-a-z0-9]*[a-z0-9])?(\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*').
    Pattern namePattern = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*");
    Matcher nameMatcher = namePattern.matcher(serving.getName());
    if(!nameMatcher.matches()){
      throw new IllegalArgumentException("Serving name must consist of lower case alphanumeric characters, '-' or '" +
        ".', and start and end with an alphanumeric character");
    }
  }
  
  /**
   * Validates user data for creating or updating a Tensorflow Serving Instance
   *
   * @param serving the user data
   * @throws ServingException if the python environment is not activated for the project
   */
  public void validateTfUserInput(Serving serving) {
    // Check that the modelPath respects the TensorFlow standard
    validateTfModelPath(serving.getModelPath(),
      serving.getModelVersion());
    
    // Check that the batching option has been specified
    if (serving.isBatchingEnabled() == null) {
      throw new IllegalArgumentException("Batching is null");
    }
  }
  
  /**
   * Validates that the provided model path follows the Tensorflow standard
   *
   * @param path the path to validate
   * @param version the version of the model
   * @throws IllegalArgumentException
   */
  private void validateTfModelPath(String path, Integer version) throws IllegalArgumentException {
    try {
      List<Inode> children = inodeController.getChildren(Paths.get(path, version.toString()).toString());
      
      if (children.stream().noneMatch(inode -> inode.getInodePK().getName().equals("variables")) ||
        children.stream().noneMatch(inode -> inode.getInodePK().getName().contains(".pb"))) {
        throw new IllegalArgumentException("The model path does not respect the TensorFlow standard");
      }
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("The model path provided does not exists");
    }
  }
  
  /**
   * Check if there is already a serving with the same name as a new/updated serving in the project
   *
   * @param project the project to query
   * @param servingWrapper the serving to compare with the existing servings
   * @throws ServingException if a duplicate was found in the database
   */
  public void checkDuplicates(Project project, ServingWrapper servingWrapper) throws ServingException {
    Serving serving = servingFacade.findByProjectAndName(project,
      servingWrapper.getServing().getName());
    if (serving != null && !serving.getId().equals(servingWrapper.getServing().getId())) {
      // There is already an entry for this project
      throw new ServingException(RESTCodes.ServingErrorCode.DUPLICATEDENTRY, Level.FINE);
    }
  }
  
  private void validateArtifact(Serving serving) throws ServingException, UnsupportedEncodingException {
    if (!settings.getKubeInstalled()) {
      // If Kubernetes is not installed
      if (serving.getArtifactVersion() != null) {
        throw new ServingException(RESTCodes.ServingErrorCode.KUBERNETES_NOT_INSTALLED, Level.SEVERE, "Artifacts " +
          "only supported in Kubernetes or KFServing deployments");
      }
      return; // no more validations needed
    }

    if (serving.getArtifactVersion() != null) {
      if (serving.getTransformer() != null && serving.getArtifactVersion() == 0) {
        throw new IllegalArgumentException("Transformers are not supported with MODEL-ONLY artifacts (i.e version 0)");
      }
      if (serving.getTransformer() == null && serving.getArtifactVersion() > 0) {
        throw new IllegalArgumentException("Transformer cannot be null for a non-MODEL-ONLY artifact");
      }
    }
  }
  
  private void validateTransformer(Project project, Serving serving)
    throws UnsupportedEncodingException, ServingException {
    if (serving.getServingTool() != ServingTool.KFSERVING) {
      throw new IllegalArgumentException("KFServing is required for using transformers");
    }
    if (serving.getTransformer().contains("/")) {
      validatePythonUserInput(project, serving.getTransformer(), Arrays.asList(".py",
        ".ipynb"));
    }
  }
  
  private void validateInferenceLogging(ServingWrapper servingWrapper, Serving serving) {
    boolean withKafkaTopic =
      servingWrapper.getKafkaTopicDTO() != null && !servingWrapper.getKafkaTopicDTO().getName().equals("NONE");
    
    if (!withKafkaTopic) {
      if (serving.getInferenceLogging() != null) {
        throw new IllegalArgumentException("Inference logger mode provided but no kafka topic specified");
      }
      return; // no more validations needed
    }
    
    if (serving.getInferenceLogging() == null) {
      throw new IllegalArgumentException("Inference logger mode not provided or invalid");
    }
    
    if (serving.getServingTool() != ServingTool.KFSERVING) {
      if (serving.getInferenceLogging() != InferenceLogging.ALL) {
        throw new IllegalArgumentException("Fine-grained inference logger is only supported in KFServing deployments");
      }
    }
  }
  
  private void validateInstances(Serving serving) {
    if (serving.getTransformer() == null && serving.getTransformerInstances() != null) {
      throw new IllegalArgumentException("Number of transformer instances cannot be provided without a transformer");
    }
    if (serving.getTransformer() != null && serving.getTransformerInstances() == null) {
      throw new IllegalArgumentException("Number of transformer instances must be provided when using a transformer");
    }
  }

  /**
   * Infer the model name from the modelPath
   * Assumes modelPath is absolute from /Projects
   * And starts with the following format /Projects/{project}/Models/{model}
   *
   * @param servingWrapper
   */
  public void inferModelName(ServingWrapper servingWrapper) {
    String modelPath = servingWrapper.getServing().getModelPath();
    String[] split = modelPath.split("/");
    servingWrapper.getServing().setModelName(split[4]);
  }
}
