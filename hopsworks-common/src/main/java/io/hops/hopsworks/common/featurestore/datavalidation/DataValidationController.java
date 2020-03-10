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

package io.hops.hopsworks.common.featurestore.datavalidation;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.GlobFilter;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DataValidationController {
  private static Logger LOGGER = Logger.getLogger(DataValidationController.class.getName());
  private static final String PATH_TO_DATA_VALIDATION = Path.SEPARATOR + Settings.DIR_ROOT + Path.SEPARATOR
      + "%s" + Path.SEPARATOR + Settings.ServiceDataset.DATAVALIDATION.getName();
  
  private static final String PATH_TO_DATA_VALIDATION_RESULT = PATH_TO_DATA_VALIDATION + Path.SEPARATOR + "%s"
      + Path.SEPARATOR + "%d";
  private static final String PATH_TO_DATA_VALIDATION_RULES = PATH_TO_DATA_VALIDATION + Path.SEPARATOR + "rules";
  private static final String PATH_TO_DATA_VALIDATION_RULES_FILE = PATH_TO_DATA_VALIDATION_RULES + Path.SEPARATOR
      + "%s_%d-rules.json";
  private static final String HDFS_FILE_PATH = "hdfs://%s";
  private static final String DEFAULT_MAIN_CLASS = "io.hops.hopsworks.verification.Verification";
  
  @EJB
  private DistributedFsService distributedFsService;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private Settings settings;
  
  public String getHopsVerificationMainClass(Path jarFile) {
    String mainClass = settings.getHopsVerificationMainClass();
    if (mainClass != null) {
      return mainClass;
    }
    DistributedFileSystemOps dfso = null;
    try {
      dfso = distributedFsService.getDfsOps();
      try (JarInputStream kis = new JarInputStream(dfso.open(jarFile))) {
        String discoveredMainClass = kis.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        settings.setHopsVerificationMainClass(discoveredMainClass);
      }
    } catch (IOException ex) {
      LOGGER.log(Level.WARNING, "Could not load Main-class of: " + jarFile.toString() + " Falling back to "
          + DEFAULT_MAIN_CLASS);
      settings.setHopsVerificationMainClass(DEFAULT_MAIN_CLASS);
    } finally {
      if (dfso != null) {
        distributedFsService.closeDfsClient(dfso);
      }
    }
    return settings.getHopsVerificationMainClass();
  }
  
  public String writeRulesToFile(Users user, Project project, FeaturegroupDTO featureGroup,
      List<ConstraintGroup> constraintGroups) throws FeaturestoreException {
    LOGGER.log(Level.FINE, "Writing validation rules to file for feature group " + featureGroup.getName());
    String jsonRules = convert2deequRules(constraintGroups);
    DistributedFileSystemOps udfso = null;
    try {
      String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
      udfso = distributedFsService.getDfsOps(hdfsUsername);
      Path rulesPath = getDataValidationRulesFilePath(project, featureGroup.getName(), featureGroup.getVersion());
      LOGGER.log(Level.FINEST, "Writing rules " + jsonRules + " to file " + rulesPath.toString());
      writeToHDFS(rulesPath, jsonRules, udfso);
      return String.format(HDFS_FILE_PATH, rulesPath.toString());
    } catch (IOException ex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_DATA_VALIDATION_RULES,
          Level.WARNING, "Failed to create data validation rules",
          "Could not write data validation rules to HDFS", ex);
    } finally {
      if (udfso != null) {
        distributedFsService.closeDfsClient(udfso);
      }
    }
  }
  
  public List<ConstraintGroup> readRulesForFeatureGroup(Users user, Project project, FeaturegroupDTO featureGroup)
    throws FeaturestoreException {
    LOGGER.log(Level.FINE, "Reading rules file for feature group " + featureGroup.getName());
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;
    try {
      Path path2rules = new Path(String.format(PATH_TO_DATA_VALIDATION_RULES, project.getName())
          + Path.SEPARATOR + "*");
      udfso = distributedFsService.getDfsOps(hdfsUsername);
      GlobFilter filter = new GlobFilter(featureGroup.getName() + "_*-rules.json");
      FileStatus[] rules = udfso.getFilesystem().globStatus(path2rules, filter);
      if (rules == null || rules.length == 0) {
        LOGGER.log(Level.FINE, "Did not find any validation rules for " + featureGroup.getName());
        return Collections.EMPTY_LIST;
      }
      List<ConstraintGroup> constraintGroups = new ArrayList<>();
      for (int i = 0; i < rules.length; i++) {
        FileStatus fileStatus = rules[i];
        Path path2rule = fileStatus.getPath();
        try (FSDataInputStream inStream = udfso.open(path2rule)) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          IOUtils.copyBytes(inStream, out, 512);
          String content = out.toString("UTF-8");
          LOGGER.log(Level.FINEST, "Found rules " + content + " for feature group " + featureGroup.getName());
          List<ConstraintGroup> groups = convertFromDeequRules(content);
          constraintGroups.addAll(groups);
          out.close();
        }
      }
      return constraintGroups;
    } catch (IOException ex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_DATA_VALIDATION_RULES,
          Level.WARNING, "Failed to read validation rules",
          "Could not read validation rules from HDFS for Feature group " + featureGroup.getName(), ex);
    } finally {
      if (udfso != null) {
        distributedFsService.closeDfsClient(udfso);
      }
    }
  }
  
  public ValidationResult getValidationResultForFeatureGroup(Users user, Project project, FeaturegroupDTO featureGroup)
    throws FeaturestoreException {
    LOGGER.log(Level.FINE, "Fetching validation result for feature group " + featureGroup.getName());
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;
    try {
      Path path2result = new Path(String.format(PATH_TO_DATA_VALIDATION_RESULT, project.getName(),
          featureGroup.getName(), featureGroup.getVersion()) + Path.SEPARATOR + "*");
      udfso = distributedFsService.getDfsOps(hdfsUsername);
      GlobFilter filter = new GlobFilter("*.json");
      FileStatus[] resultFiles = udfso.getFilesystem().globStatus(path2result, filter);
      if (resultFiles == null || resultFiles.length == 0) {
        LOGGER.log(Level.FINE, "Did not find any validation result for " + featureGroup.getName());
        return new ValidationResult(ValidationResult.Status.Empty, Collections.EMPTY_LIST);
      }
      List<ConstraintResult> constraintResults = new ArrayList<>();
      Gson gson = new GsonBuilder()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create();
      for (FileStatus fileStatus : resultFiles) {
        Path resultFile = fileStatus.getPath();
        try (FSDataInputStream inStream = udfso.open(resultFile)) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          IOUtils.copyBytes(inStream, out, 512);
          String[] constraintResultsJson = out.toString("UTF-8").split("\n");
          for (String cr : constraintResultsJson) {
            LOGGER.log(Level.FINEST, "Found result " + cr + " for feature group " + featureGroup.getName());
            ConstraintResult constraintResult = gson.fromJson(cr, ConstraintResult.class);
            constraintResults.add(constraintResult);
          }
        }
      }
      ValidationResult validationResult = new ValidationResult();
      validationResult.setConstraintsResult(constraintResults);
      assignValidationResultStatus(validationResult);
      return validationResult;
    } catch (IOException ex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_READ_DATA_VALIDATION_RESULT,
          Level.WARNING, "Failed to read validation result",
          "Failed to read validation result from HDFS for Feature group " + featureGroup.getName(), ex);
    } finally {
      if (udfso != null) {
        distributedFsService.closeDfsClient(udfso);
      }
    }
  }
  
  /**
   * If there are no Warnings or Errors then total status is Success.
   *
   * If there are only constraints with severity level Warning, then if
   * (a) there are no errors but at least one Warning, status is Warning
   * (b) there is at least one error, status is Error
   *
   * If there is at least one constraint with severity level Error, then if
   * there is any Warning or Error, final status is Error
   * @param validationResult
   */
  private void assignValidationResultStatus(final ValidationResult validationResult) {
    int success = 0, warning = 0, error = 0;
    ConstraintGroupLevel level = ConstraintGroupLevel.Warning;
    for (ConstraintResult cr : validationResult.getConstraintsResult()) {
      if (cr.getCheckLevel().equals(ConstraintGroupLevel.Error)) {
        level = ConstraintGroupLevel.Error;
      }
      switch (cr.getConstraintStatus()) {
        case Success:
          success++;
          break;
        case Warning:
          warning++;
          break;
        case Failure:
          error++;
          break;
        default:
      }
    }
    if (success != 0 && warning == 0 && error == 0) {
      validationResult.setStatus(ValidationResult.Status.Success);
      return;
    }
    if (level.equals(ConstraintGroupLevel.Warning)) {
      if (warning > 0 && error == 0) {
        validationResult.setStatus(ValidationResult.Status.Warning);
        return;
      }
      if (warning >= 0 && error > 0) {
        validationResult.setStatus(ValidationResult.Status.Failure);
        return;
      }
    }
    if (level.equals(ConstraintGroupLevel.Error)) {
      validationResult.setStatus(ValidationResult.Status.Failure);
    }
  }
  
  private void writeToHDFS(Path path2file, String content, DistributedFileSystemOps udfso) throws IOException {
    try (FSDataOutputStream outStream = udfso.create(path2file)) {
      outStream.writeBytes(content);
      outStream.hflush();
      LOGGER.log(Level.FINE, "Finished writing rules to file " + path2file.toString());
    }
  }
  
  private Path getDataValidationRulesFilePath(Project project, String featureGroupName, Integer featureGroupVersion) {
    return new Path(String.format(PATH_TO_DATA_VALIDATION_RULES_FILE, project.getName(), featureGroupName,
        featureGroupVersion));
  }
  
  private String convert2deequRules(List<ConstraintGroup> constraintGroups) {
    Gson serializer = new Gson();
    JsonObject json = new JsonObject();
    JsonArray constraintGroupsJSON = new JsonArray();
    for (ConstraintGroup constraintGroup : constraintGroups) {
      JsonElement constraintGroupJSON = serializer.toJsonTree(constraintGroup);
      constraintGroupsJSON.add(constraintGroupJSON);
    }
    json.add("constraintGroups", constraintGroupsJSON);
    return serializer.toJson(json);
  }
  
  private List<ConstraintGroup> convertFromDeequRules(String rules) {
    Gson deserializer = new Gson();
    JsonElement topLevelObject = deserializer.fromJson(rules, JsonElement.class);
    JsonArray constraintGroupsJSON = topLevelObject.getAsJsonObject()
        .getAsJsonArray("constraintGroups");
    List<ConstraintGroup> constraintGroups = new ArrayList<>(constraintGroupsJSON.size());
    constraintGroupsJSON.forEach(cgj -> {
      ConstraintGroup cg = deserializer.fromJson(cgj, ConstraintGroup.class);
      constraintGroups.add(cg);
    });
    return constraintGroups;
  }
}