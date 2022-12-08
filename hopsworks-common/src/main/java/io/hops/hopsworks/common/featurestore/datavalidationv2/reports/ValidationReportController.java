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
import io.hops.hopsworks.common.featurestore.datavalidationv2.results.ValidationResultController;
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
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.IngestionResult;
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
  @EJB
  private ValidationResultController validationResultController;

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
    report.setIngestionResult(reportDTO.getIngestionResult());

    // Parse the report to get validation time, just create the date 
    Date validationTime;
    try {
      JSONObject reportMeta = new JSONObject(reportDTO.getMeta());
      String validationTimeString = reportMeta.getString("validation_time");
      String formatDateString = "yyyyMMdd'T'HHmmss.SSS";
      validationTime = new SimpleDateFormat(formatDateString).parse(
        validationTimeString.substring(0, validationTimeString.length() - 4));
    } catch (JSONException | ParseException exception) {
      validationTime = new Date();
    }
    report.setValidationTime(validationTime);

    // Dump the whole report to a file. 
    Inode reportInode = registerValidationReportToDisk(user, featuregroup, reportDTO, validationTime);
    report.setInode(reportInode);

    List<ValidationResult> results = new ArrayList<ValidationResult>();

    for (ValidationResultDTO dto : reportDTO.getResults()) {
      results.add(validationResultController.convertResultDTOToPersistent(report, dto));
    }
    report.setValidationResults(results);

    return report;
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
          resultsJsonArray.put(validationResultController.convertValidationResultDTOToJson(resultDTO));
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
}
