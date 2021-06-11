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
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
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
   * @param servingWrapper contains the user-data to validate
   * @param project the project where the serving resides
   * @throws ServingException
   * @throws java.io.UnsupportedEncodingException
   */
  public void validateUserInput(ServingWrapper servingWrapper, Project project) throws ServingException,
      UnsupportedEncodingException {
    // Check that the modelName is present
    if (Strings.isNullOrEmpty(servingWrapper.getServing().getName())) {
      throw new IllegalArgumentException("Serving name not provided");
    } else if (servingWrapper.getServing().getName().contains(" ")) {
      throw new IllegalArgumentException("Serving name cannot contain spaces");
    }
    // Check that the artifactPath is present
    if (Strings.isNullOrEmpty(servingWrapper.getServing().getArtifactPath())) {
      throw new IllegalArgumentException("Artifact path not provided");
    } else {
      // Format artifact path (e.g remove duplicated '/')
      String formattedArtifactPath = Paths.get(servingWrapper.getServing().getArtifactPath()).toString();
      servingWrapper.getServing().setArtifactPath(formattedArtifactPath);
    }
    if (servingWrapper.getServing().getVersion() == null) {
      throw new IllegalArgumentException("Serving version not provided");
    }
    if (servingWrapper.getServing().getInstances() == null) {
      throw new IllegalArgumentException("Number of instances not provided");
    }
    // Check for duplicated entries
    checkDuplicates(project, servingWrapper);
    //Validate that serving name follows allowed regex as required by the InferenceResource to use it as a
    //Rest Endpoint
    Pattern urlPattern = Pattern.compile("[a-zA-Z0-9]+");
    Matcher urlMatcher = urlPattern.matcher(servingWrapper.getServing().getName());
    if(!urlMatcher.matches()){
      throw new IllegalArgumentException("Serving name must follow regex: \"[a-zA-Z0-9]+\"");
    }
    //Serving-type-specific validations
    if (servingWrapper.getServing().getModelServer() == null) {
      throw new IllegalArgumentException("Model server not provided or unsupported");
    }
    if (servingWrapper.getServing().getModelServer() == ModelServer.TENSORFLOW_SERVING) {
      validateTfUserInput(servingWrapper);
    }
    if (servingWrapper.getServing().getModelServer() == ModelServer.FLASK) {
      validateSKLearnUserInput(servingWrapper, project);
    }
    // Serving tool validation
    if (servingWrapper.getServing().getServingTool() == null) {
      throw new IllegalArgumentException("Serving tool not provided or unsupported");
    }
    // Serving-tool-specific validations
    if (servingWrapper.getServing().getServingTool() == ServingTool.KFSERVING) {
      validateKFServingUserInput(servingWrapper);
    }
  }
  
  /**
   * Validates user data for creating or updating a SkLearn Serving Instance
   *
   * @param servingWrapper the user data
   * @param project the project to create the serving for
   * @throws ServingException if the python environment is not activated for the project
   * @throws java.io.UnsupportedEncodingException
   */
  public void validateSKLearnUserInput(ServingWrapper servingWrapper, Project project) throws ServingException,
      UnsupportedEncodingException {
  
    // Check that the script name is valid and exists
    String scriptName = Utils.getFileName(servingWrapper.getServing().getArtifactPath());
    if(!scriptName.contains(".py")){
      throw new IllegalArgumentException("Script name should be a valid python script name");
    }
    String hdfsPath = servingWrapper.getServing().getArtifactPath();
    //Remove hdfs:// if it is in the path
    hdfsPath = Utils.prepPath(hdfsPath);
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
   * @param servingWrapper the user data
   * @throws ServingException
   */
  public void validateKFServingUserInput(ServingWrapper servingWrapper) throws ServingException {
    if (!settings.getKubeInstalled()) {
      throw new ServingException(RESTCodes.ServingErrorCode.KUBERNETES_NOT_INSTALLED, Level.SEVERE, "Serving tool not" +
        " supported");
    }
  
    if (!settings.getKubeKFServingInstalled()) {
      throw new ServingException(RESTCodes.ServingErrorCode.KFSERVING_NOT_ENABLED, Level.SEVERE, "Serving tool not " +
        "supported");
    }
    
    if (servingWrapper.getServing().getModelServer() == ModelServer.FLASK) {
      throw new IllegalArgumentException("KFServing not supported for SKLearn models");
    }
    
    if (servingWrapper.getServing().isBatchingEnabled()) {
      throw new IllegalArgumentException("Request batching is not supported in KFServing deployments");
    }
  
    // Service name is used as DNS subdomain. It must consist of lower case alphanumeric characters, '-' or '.', and
    // must start and end with an alphanumeric character. (e.g. 'example.com', regex used for validation is '[a-z0-9]
    // ([-a-z0-9]*[a-z0-9])?(\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*').
    Pattern namePattern = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*");
    Matcher nameMatcher = namePattern.matcher(servingWrapper.getServing().getName());
    if(!nameMatcher.matches()){
      throw new IllegalArgumentException("Serving name must consist of lower case alphanumeric characters, '-' or '" +
        ".', and start and end with an alphanumeric character");
    }
  }
  
  /**
   * Validates user data for creating or updating a Tensorflow Serving Instance
   *
   * @param servingWrapper the user data
   * @throws ServingException if the python environment is not activated for the project
   */
  public void validateTfUserInput(ServingWrapper servingWrapper) {
    // Check that the modelPath respects the TensorFlow standard
    validateTfModelPath(servingWrapper.getServing().getArtifactPath(),
      servingWrapper.getServing().getVersion());
    
    // Check that the batching option has been specified
    if (servingWrapper.getServing().isBatchingEnabled() == null) {
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
}
