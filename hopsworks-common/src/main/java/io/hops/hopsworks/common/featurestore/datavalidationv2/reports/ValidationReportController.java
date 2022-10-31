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

package io.hops.hopsworks.common.featurestore.datavalidationv2.reports;

import io.hops.hopsworks.alerting.api.alert.dto.PostableAlert;
import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.dao.AbstractFacade.FilterBy;
import io.hops.hopsworks.common.dao.AbstractFacade.SortBy;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.datavalidationv2.expectations.ExpectationFacade;
import io.hops.hopsworks.common.featurestore.datavalidationv2.results.ValidationResultDTO;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.ValidationRuleAlertStatus;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.IngestionResult;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationIngestionPolicy;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationReport;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ValidationResult;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlertStatus;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_REPORT_EVALUATION_PARAMETERS;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_REPORT_META;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_REPORT_STATISTICS;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_EXCEPTION_INFO;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_EXPECTATION_CONFIG;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_META;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_VALIDATION_RESULT_RESULT_FIELD;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ValidationReportController {
  private static final Logger LOGGER = Logger.getLogger(ExpectationSuiteController.class.getName());

  @EJB
  private ValidationReportFacade validationReportFacade;
  @EJB
  private ExpectationFacade expectationFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private DatasetController datasetController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private AlertController alertController;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;

  /////////////////////////////////////////////////////
  ////// VALIDATION REPORT CRUD
  /////////////////////////////////////////////////////

  public ValidationReport getValidationReportById(Integer validationReportId)
    throws FeaturestoreException {
    Optional<ValidationReport> validationReport = validationReportFacade.findById(validationReportId);

    if (!validationReport.isPresent()) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.VALIDATION_REPORT_NOT_FOUND,
        Level.FINE, String.format("No Validation report was found with id : %d.",
        validationReportId));
    }
    return validationReport.get();
  }

  public CollectionInfo<ValidationReport> getAllValidationReportByFeatureGroup(Integer offset, Integer limit,
    Set<? extends SortBy> sorts, Set<? extends FilterBy> filters, Featuregroup featuregroup) {
    return validationReportFacade.findByFeaturegroup(offset, limit, sorts, filters, featuregroup);
  }

  public ValidationReport createValidationReport(Users user, Featuregroup featuregroup,
    ValidationReportDTO reportDTO)
    throws FeaturestoreException {

    ValidationReport validationReport = convertReportDTOToPersistent(user, featuregroup, reportDTO);
    validationReportFacade.persist(validationReport);

    fsActivityFacade.logValidationReportActivity(user, validationReport);
    
    // trigger alerts if any
    triggerAlerts(featuregroup, validationReport);

    return validationReport;
  }
  
  private void triggerAlerts(Featuregroup featureGroup, ValidationReport validationReport) {
    List<PostableAlert> postableAlerts = getPostableAlerts(featureGroup, validationReport);
    alertController.sendFgAlert(postableAlerts, featureGroup.getFeaturestore().getProject(), featureGroup.getName());
  }
  
  private List<PostableAlert> getPostableAlerts(Featuregroup featureGroup, ValidationReport validationReport) {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    if (featureGroup.getFeatureGroupAlerts() != null && !featureGroup.getFeatureGroupAlerts().isEmpty()) {
      String name = featurestoreFacade.getHiveDbName(featureGroup.getFeaturestore().getHiveDbId());
      for (FeatureGroupAlert alert : featureGroup.getFeatureGroupAlerts()) {
        if (alert.getStatus() == ValidationRuleAlertStatus.FAILURE
            && validationReport.getIngestionResult() == IngestionResult.REJECTED) {
          postableAlerts.add(getPostableAlert(alert, name, featureGroup, validationReport));
        } else if (alert.getStatus() == ValidationRuleAlertStatus.SUCCESS
            && validationReport.getIngestionResult() == IngestionResult.INGESTED) {
          postableAlerts.add(getPostableAlert(alert, name, featureGroup, validationReport));
        }
      }
    }
    if (featureGroup.getFeaturestore().getProject().getProjectServiceAlerts() != null &&
      !featureGroup.getFeaturestore().getProject().getProjectServiceAlerts().isEmpty()) {
      String name = featurestoreFacade.getHiveDbName(featureGroup.getFeaturestore().getHiveDbId());
      for (ProjectServiceAlert alert : featureGroup.getFeaturestore().getProject().getProjectServiceAlerts()) {
        if (ProjectServiceEnum.FEATURESTORE.equals(alert.getService())
            && alert.getStatus() == ProjectServiceAlertStatus.VALIDATION_FAILURE
            && validationReport.getIngestionResult() == IngestionResult.REJECTED) {
          postableAlerts.add(getPostableAlert(alert, name, featureGroup, validationReport));
        } else if (ProjectServiceEnum.FEATURESTORE.equals(alert.getService())
            && alert.getStatus() == ProjectServiceAlertStatus.VALIDATION_SUCCESS
            && validationReport.getIngestionResult() == IngestionResult.INGESTED) {
          postableAlerts.add(getPostableAlert(alert, name, featureGroup, validationReport));
        }
      }
    }
    return postableAlerts;
  }
  
  private PostableAlert getPostableAlert(FeatureGroupAlert alert, String featureStoreName, Featuregroup featureGroup,
    ValidationReport validationReport) {
    return alertController.getPostableFgAlert(featureGroup.getFeaturestore().getProject().getName(),
      alert.getAlertType(), alert.getSeverity(), validationReport.getIngestionResult().toString(),
      constructAlertSummary(featureGroup, validationReport), constructAlertDescription(validationReport),
      featureGroup.getId(), featureStoreName, featureGroup.getName(), featureGroup.getVersion());
  }
  
  private PostableAlert getPostableAlert(ProjectServiceAlert alert, String featureStoreName, Featuregroup featureGroup,
    ValidationReport validationReport) {
    return alertController.getPostableFgAlert(featureGroup.getFeaturestore().getProject().getName(),
      alert.getAlertType(), alert.getSeverity(), validationReport.getIngestionResult().toString(),
      constructAlertSummary(featureGroup, validationReport), constructAlertDescription(validationReport),
      featureGroup.getId(), featureStoreName, featureGroup.getName(), featureGroup.getVersion());
  }
  
  public String constructAlertSummary(Featuregroup featureGroup, ValidationReport validationReport) {
    return String.format("Feature Group: %s, version: %s, expectation suite: %s, success: %s",
      featureGroup.getName(),
      featureGroup.getVersion(), featureGroup.getExpectationSuite().getName(), validationReport.getSuccess(),
      validationReport.getStatistics());
  }
  
  public String constructAlertDescription(ValidationReport validationReport) {
    return String.format("Statistics: %s", validationReport.getStatistics());
  }

  public void deleteValidationReportById(Users user, Integer validationReportId) throws FeaturestoreException {
    Optional<ValidationReport> validationReport = validationReportFacade.findById(validationReportId);
    if (!validationReport.isPresent()) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.VALIDATION_REPORT_NOT_FOUND, Level.WARNING,
        String.format("ValidationReport with id : %d was not found causing delete to fail", validationReportId));
    }
    deleteSingleReportInode(user, validationReport.get());
  }

  private void deleteSingleReportInode(Users user, ValidationReport validationReport)
    throws FeaturestoreException {
    Featuregroup featuregroup = validationReport.getFeaturegroup();
    Project project = featuregroup.getFeaturestore().getProject();
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
      // delete json files
      udfso.rm(inodeController.getPath(validationReport.getInode()), false);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_ON_DISK_VALIDATION_REPORT,
        Level.WARNING, "", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public void deleteFeaturegroupDataValidationDir(Users user, Featuregroup featuregroup)
    throws FeaturestoreException {
    Project project = featuregroup.getFeaturestore().getProject();
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
      Dataset dataValidationDir = getOrCreateDataValidationDataset(project, user);
      Path targetDir = new Path(datasetController.getDatasetPath(dataValidationDir), featuregroup.getName());
      udfso.rm(targetDir, true);
    } catch (DatasetException | HopsSecurityException | IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_ON_DISK_VALIDATION_REPORT,
        Level.WARNING, "", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public ValidationReport convertReportDTOToPersistent(Users user, Featuregroup featuregroup,
    ValidationReportDTO reportDTO) throws FeaturestoreException {
    verifyValidationReportDTOFields(reportDTO);
    ValidationReport report = new ValidationReport();
    report.setFeaturegroup(featuregroup);
    report.setMeta(reportDTO.getMeta());
    report.setSuccess(reportDTO.getSuccess());
    report.setStatistics(reportDTO.getStatistics());
    report.setEvaluationParameters(reportDTO.getEvaluationParameters());
    
    // infer ingestion result
    report.setIngestionResult(inferIngestionResult(reportDTO.getSuccess(), featuregroup.getExpectationSuite()
      .getValidationIngestionPolicy()));

    // Parse the report to get validation time, just create the date 
    Date validationTime;
    try {
      JSONObject reportMeta = new JSONObject(reportDTO.getMeta());
      String validationTimeString = reportMeta.getJSONObject("run_id").getString("run_time");
      String formatDateString = "yyyy-MM-dd'T'hh:mm:ss.SSSSSSX";
      validationTime = new SimpleDateFormat(formatDateString).parse(validationTimeString);
    } catch (JSONException | ParseException exception) {
      validationTime = new Date();
    }
    report.setValidationTime(validationTime);

    // Dump the whole report to a file. 
    Inode reportInode = registerValidationReportToDisk(user, featuregroup, reportDTO, validationTime);
    report.setInode(reportInode);

    List<ValidationResult> results = new ArrayList<ValidationResult>();

    for (ValidationResultDTO dto : reportDTO.getResults()) {
      results.add(convertResultDTOToPersistent(report, dto));
    }
    report.setValidationResults(results);

    return report;
  }
  
  public IngestionResult inferIngestionResult(Boolean success, ValidationIngestionPolicy validationIngestionPolicy)
      throws FeaturestoreException {
    if (validationIngestionPolicy == ValidationIngestionPolicy.ALWAYS) {
      return IngestionResult.INGESTED;
    } else if (validationIngestionPolicy == ValidationIngestionPolicy.STRICT) {
      if (success) {
        return IngestionResult.INGESTED;
      } else {
        return IngestionResult.REJECTED;
      }
    }
    throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_INFERRING_INGESTION_RESULT, Level.SEVERE,
      String.format("Validation Ingestion Policy: %s, Success: %s", validationIngestionPolicy, success));
  }

  public ValidationResult convertResultDTOToPersistent(ValidationReport report, ValidationResultDTO dto)
    throws FeaturestoreException {
    verifyValidationResultDTOFields(dto);
    ValidationResult result = new ValidationResult();
    result.setMeta(dto.getMeta());
    result.setSuccess(dto.getSuccess());
    result.setExpectationConfig(dto.getExpectationConfig());
    result.setValidationReport(report);

    // We need:
    // - Get the expectation id from the meta field in the expectation_config field.
    // - Shorten result field if too long
    // - Shorten exceptionInfo field if too long
    if (dto.getResult().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_RESULT_FIELD) {
      result.setResult(validationResultShortenResultField(dto.getResult()));
    } else {
      result.setResult(dto.getResult());
    }

    if (dto.getExceptionInfo().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_EXCEPTION_INFO) {
      result.setExceptionInfo(validationResultShortenExceptionInfoField(dto.getExceptionInfo()));
    } else {
      result.setExceptionInfo(dto.getExceptionInfo());
    }

    result.setExpectation(parseExpectationIdFromResultDTO(dto.getExpectationConfig()));
    return result;
  }

  private Expectation parseExpectationIdFromResultDTO(String dtoExpectationConfig) throws FeaturestoreException {
    // 1. Parse config, 2. Look for expectation_id, 3. findExpectationById, 4. setExpectation, 5. Celebrate!
    JSONObject expectationConfig;
    Integer expectationId;
    try {
      expectationConfig = new JSONObject(dtoExpectationConfig);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result expectation config field %s is not a valid json.",
          dtoExpectationConfig),
        e.getMessage()
      );
    }

    JSONObject meta;
    try {
      meta = expectationConfig.getJSONObject("meta");
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result expectation config meta field %s is not a valid json.",
          dtoExpectationConfig),
        e.getMessage()
      );
    }

    try {
      expectationId = meta.getInt("expectationId");
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.KEY_NOT_FOUND_OR_INVALID_VALUE_TYPE_IN_JSON_OBJECT,
        Level.SEVERE,
        String.format("Validation result expectation config meta %s does not contain expectationId key or the " +
          "associated value does not convert to an integer", meta),
        e.getMessage());
    }

    Optional<Expectation> expectation = expectationFacade.findById(expectationId);

    if (!expectation.isPresent()) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.EXPECTATION_NOT_FOUND, Level.WARNING);
    }

    return expectation.get();
  }

  public String validationResultShortenResultField(String result) {
    JSONObject resultJson;
    try {
      resultJson = new JSONObject(result);
    } catch (JSONException e) {
      LOGGER.warning(String.format(
        "Parsing result field threw JSONException that should have been handled when verifying input.\n%s\n%s",
        e.getMessage(), e.getStackTrace().toString()));
      resultJson = new JSONObject();
    }

    JSONObject shortResultJson = new JSONObject();
    String userMessage =
      "Result field exceeded max available space in SQL table, " +
        "download validation report file to access the complete result.";
    shortResultJson.put("user_message", userMessage);

    if (resultJson.has("observed_value")) {
      shortResultJson.put("observed_value", resultJson.getString("observed_value"));

      if (shortResultJson.toString().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_RESULT_FIELD) {
        shortResultJson.remove("observed_value");
        return shortResultJson.toString();
      }
    }

    if (resultJson.has("unexpected_count") && resultJson.has("partial_unexpected_list")
      && resultJson.has("unexpected_percent") && resultJson.has("unexpected_percent_nonmissing")) {

      shortResultJson.put("unexpected_count", resultJson.getInt("unexpected_count"));
      shortResultJson.put("unexpected_percent", resultJson.getFloat("unexpected_percent"));
      shortResultJson.put("unexpected_percent_nonmissing", resultJson.getFloat("unexpected_percent_nonmissing"));
      shortResultJson.put("partial_unexpected_list", resultJson.getString("partial_unexpected_list"));

      if (shortResultJson.toString().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_RESULT_FIELD) {
        shortResultJson.remove("partial_unexpected_list");
        return shortResultJson.toString();
      }
    }

    return shortResultJson.toString();
  }

  public String validationResultShortenExceptionInfoField(String exceptionInfo) {
    JSONObject exceptionInfoJson;
    try {
      exceptionInfoJson = new JSONObject(exceptionInfo);
    } catch (JSONException e) {
      LOGGER.warning(String.format(
        "Parsing exceptionInfo field threw JSONException that should have been handled when verifying input.\n%s\n%s",
        e.getMessage(), e.getStackTrace().toString()));
      exceptionInfoJson = new JSONObject();
    }

    JSONObject shortExceptionInfoJson = new JSONObject();
    String userMessage =
      "exception_info field exceeded max available space in SQL table, " +
        "download validation report file to access the complete info.";
    shortExceptionInfoJson.put("user_message", userMessage);
    shortExceptionInfoJson.put("raised_exception", exceptionInfoJson.getBoolean("raised_exception"));

    shortExceptionInfoJson.put("exception_message", exceptionInfoJson.getString("exception_message"));
    if (shortExceptionInfoJson.toString().length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_EXCEPTION_INFO) {
      shortExceptionInfoJson.remove("exception_message");
      return shortExceptionInfoJson.toString();
    }

    // exception_traceback cannot fit otherwise we would not be in this function

    return shortExceptionInfoJson.toString();
  }

  private Inode registerValidationReportToDisk(Users user, Featuregroup featuregroup,
    ValidationReportDTO reportDTO, Date validationTime) throws FeaturestoreException {
    DistributedFileSystemOps udfso = null;
    Project project = featuregroup.getFeaturestore().getProject();

    JSONObject reportJSON = convertValidationReportDTOToJson(reportDTO);

    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

      // Dataset is confusing terminology. Get path to on_disk dataValidationDir
      Dataset dataValidationDir = getOrCreateDataValidationDataset(project, user);

      // All validation report attached to a particular featuregroup version will be stored in same directory
      Path reportDirPath = new Path(datasetController.getDatasetPath(dataValidationDir), featuregroup.getName());
      if (!udfso.isDir(reportDirPath.toString())) {
        udfso.mkdir(reportDirPath.toString());
      }
      reportDirPath = new Path(reportDirPath, featuregroup.getVersion().toString());
      if (!udfso.isDir(reportDirPath.toString())) {
        udfso.mkdir(reportDirPath.toString());
      }
      reportDirPath = new Path(reportDirPath, "ValidationReports");
      if (!udfso.isDir(reportDirPath.toString())) {
        udfso.mkdir(reportDirPath.toString());
      }
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss");
      String fileName = String.format("validation_report_%s.json", formatter.format(validationTime));
      Path reportPath = new Path(reportDirPath, fileName);
      if (udfso.exists(reportPath)) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_SAVING_ON_DISK_VALIDATION_REPORT,
          Level.SEVERE, String.format("Validation report with file name %s already exists.", fileName));
      }
      udfso.create(reportPath, reportJSON.toString());
      Inode inode = inodeController.getInodeAtPath(reportPath.toString());

      return inode;
    } catch (DatasetException | HopsSecurityException | IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_SAVING_ON_DISK_VALIDATION_REPORT,
        Level.WARNING, e.getMessage());
    } finally {
      dfs.closeDfsClient(udfso);
    }

  }

  private Dataset getOrCreateDataValidationDataset(Project project, Users user)
    throws DatasetException, HopsSecurityException {
    Optional<Dataset> dataValidationDataset = project.getDatasetCollection().stream()
      .filter(d -> d.getName().equals(Settings.ServiceDataset.DATAVALIDATION.getName()))
      .findFirst();
    // This is the case of an old project without DATAVALIDATION dataset, create it.
    if (dataValidationDataset.isPresent()) {
      return dataValidationDataset.get();
    } else {
      return createDataValidationDataset(project, user);
    }
  }

  private Dataset createDataValidationDataset(Project project, Users user)
    throws DatasetException, HopsSecurityException {
    // Needs super user privileges as we are creating a dataset
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      return datasetController.createDataset(user, project,
        Settings.ServiceDataset.DATAVALIDATION.getName(),
        Settings.ServiceDataset.DATAVALIDATION.getDescription(),
        Provenance.Type.DISABLED.dto, false, DatasetAccessPermission.EDITABLE, dfso);
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }

  //TODO(Victor) refactor with a convertValidationResultDTOToJson
  private JSONObject convertValidationReportDTOToJson(ValidationReportDTO reportDTO) throws FeaturestoreException {
    JSONObject reportJSON = new JSONObject();

    try {
      if (reportDTO.getFullJson() != null) {
        reportJSON = new JSONObject(reportDTO.getFullJson());
      } else {
        reportJSON.put("evaluation_parameters", new JSONObject(reportDTO.getEvaluationParameters()));
        reportJSON.put("statistics", new JSONObject(reportDTO.getStatistics()));
        reportJSON.put("success", reportDTO.getSuccess());
        reportJSON.put("meta", new JSONObject(reportDTO.getMeta()));


        JSONArray resultsJsonArray = new JSONArray();

        for (ValidationResultDTO resultDTO : reportDTO.getResults()) {
          JSONObject resultJSON = new JSONObject();
          resultJSON.put("success", resultDTO.getSuccess());
          resultJSON.put("exception_info", new JSONObject(resultDTO.getExceptionInfo()));
          resultJSON.put("result", new JSONObject(resultDTO.getResult()));
          resultJSON.put("meta", new JSONObject(resultDTO.getMeta()));
          resultJSON.put("expectation_config", new JSONObject(resultDTO.getExpectationConfig()));

          resultsJsonArray.put(resultJSON);
        }

        reportJSON.put("results", resultsJsonArray);
      }
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.VALIDATION_REPORT_IS_NOT_VALID_JSON, Level.WARNING, e.getMessage());
    }

    return reportJSON;
  }

  ////////////////////////////////////////
  //// Input Verification for Validation Report
  ///////////////////////////////////////

  public void verifyValidationReportDTOFields(ValidationReportDTO dto) throws FeaturestoreException {
    verifyValidationReportEvaluationParameters(dto.getEvaluationParameters());
    verifyValidationReportMeta(dto.getMeta());
    verifyValidationReportStatistics(dto.getStatistics());
  }

  public void verifyValidationReportStatistics(String statistics) throws FeaturestoreException {
    if (statistics == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Validation report statistics field cannot be null. Pass an empty stringified JSON."
      );
    }

    if (statistics.length() > MAX_CHARACTERS_IN_VALIDATION_REPORT_STATISTICS) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Validation report statistics field %s exceeds the max allowed character length %d.",
          statistics, MAX_CHARACTERS_IN_VALIDATION_REPORT_STATISTICS)
      );
    }

    try {
      new JSONObject(statistics);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation report statistics field %s is not a valid json.", statistics),
        e.getMessage()
      );
    }
  }

  public void verifyValidationReportEvaluationParameters(String evaluationParameters) throws FeaturestoreException {
    if (evaluationParameters == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Validation report evaluation_parameters field cannot be null. Pass an empty stringified JSON."
      );
    }

    if (evaluationParameters.length() > MAX_CHARACTERS_IN_VALIDATION_REPORT_EVALUATION_PARAMETERS) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Validation report evaluation_parameters field %s exceeds the max allowed character length %d.",
          evaluationParameters, MAX_CHARACTERS_IN_VALIDATION_REPORT_EVALUATION_PARAMETERS)
      );
    }

    try {
      new JSONObject(evaluationParameters);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation report evaluation_parameters field %s is not a valid json.", evaluationParameters),
        e.getMessage()
      );
    }
  }

  public void verifyValidationReportMeta(String meta) throws FeaturestoreException {
    if (meta == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Validation report meta field cannot be null. Pass an empty stringified JSON."
      );
    }

    if (meta.length() > MAX_CHARACTERS_IN_VALIDATION_REPORT_META) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Validation report meta field %s exceeds the max allowed character length %d.",
          meta, MAX_CHARACTERS_IN_VALIDATION_REPORT_META)
      );
    }

    try {
      new JSONObject(meta);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation report meta field %s is not a valid json.", meta),
        e.getMessage()
      );
    }
  }

  ////////////////////////////////////////
  //// Input Verification for Validation Report
  ///////////////////////////////////////

  public void verifyValidationResultDTOFields(ValidationResultDTO dto) throws FeaturestoreException {
    verifyValidationResultExceptionInfo(dto.getExceptionInfo());
    verifyValidationResultMeta(dto.getMeta());
    verifyValidationResultExpectationConfig(dto.getExpectationConfig());
    verifyValidationResultResultAndObservedValue(dto.getResult());
  }

  public void verifyValidationResultMeta(String meta) throws FeaturestoreException {
    if (meta == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Validation result meta field cannot be null. Pass an empty stringified JSON."
      );
    }

    if (meta.length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_META) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Validation result meta field %s exceeds the max allowed character length %d.",
          meta, MAX_CHARACTERS_IN_VALIDATION_RESULT_META)
      );
    }

    try {
      new JSONObject(meta);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result meta field %s is not a valid json.", meta),
        e.getMessage()
      );
    }
  }

  public void verifyValidationResultExpectationConfig(String expectationConfig) throws FeaturestoreException {
    if (expectationConfig == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_NULLABLE,
        Level.SEVERE,
        "Validation result expectation config field cannot be null. Pass an empty stringified JSON."
      );
    }

    if (expectationConfig.length() > MAX_CHARACTERS_IN_VALIDATION_RESULT_EXPECTATION_CONFIG) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER,
        Level.SEVERE,
        String.format("Validation result expectation config field %s exceeds the max allowed character length %d.",
          expectationConfig, MAX_CHARACTERS_IN_VALIDATION_RESULT_EXPECTATION_CONFIG)
      );
    }

    try {
      new JSONObject(expectationConfig);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result expectation config field %s is not a valid json.", expectationConfig),
        e.getMessage()
      );
    }
  }

  public void verifyValidationResultExceptionInfo(String exceptionInfo) throws FeaturestoreException {
    if (exceptionInfo == null) {
      exceptionInfo = "{}";
      return;
    }

    try {
      new JSONObject(exceptionInfo);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result exception info field %s is not a valid json.", exceptionInfo),
        e.getMessage()
      );
    }

    // The length of the result field will need to be checked as well before writing to SQL table but 
    // but first the full report will be written to disk.
  }

  public void verifyValidationResultResultAndObservedValue(String result) throws FeaturestoreException {
    // For result_format = {"result_format": "BOOLEAN_ONLY"}, result field is null. Turned into empty JSON.
    if (result == null) {
      result = "{}";
      return;
    }

    // If not null it must be valid json object
    try {
      new JSONObject(result);
    } catch (JSONException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.INPUT_FIELD_IS_NOT_VALID_JSON,
        Level.SEVERE,
        String.format("Validation result result field %s is not a valid json.", result),
        e.getMessage()
      );
    }
    // The length of the result field will need to be checked as well before writing to SQL table but 
    // but first the full report will be written to disk.
  }
}
