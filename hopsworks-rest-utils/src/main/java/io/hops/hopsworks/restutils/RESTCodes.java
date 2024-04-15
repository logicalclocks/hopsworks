/*
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
 */

package io.hops.hopsworks.restutils;

import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Hopsworks error codes categorized by resource. Convention for code values is the following:
 * 1. All error codes must be a total of 6 digits, first 2 indicate error type and last 4 indicate error id.
 * 2. Service error codes start  with "10".
 * 3. Dataset error codes start  with "11".
 * 4. Generic error codes start  with "12".
 * 5. Job error codes start  with "13".
 * 6. Request error codes start  with "14".
 * 7. Project error codes start  with "15".
 * 8. Dela error codes start with "17"
 * 9. Metadata error codes start with "18"
 * 10. Kafka error codes start with "19"
 * 11. User error codes start with "20"
 * 12. Security error codes start  with "20".
 * 14. CA error codes start with "22".
 * 15. DelaCSR error codes start with "23".
 * 16. Serving error codes start with "24".
 * 17. Inference error codes start with "25".
 * 18. Activities error codes start with "26".
 * 19. Featurestore error codes start with "27".
 * 20. Python error codes start with "28".
 *
 * Schema Registry error codes are violating the convention. Following the confluent docs, they return a 5 digits
 * codes starting with the http status code and then a number. e.g. 50001 for Internal Server Error
 */

@XmlRootElement
public class RESTCodes {


  public interface RESTErrorCode {

    Response.StatusType getRespStatus();

    Integer getCode();

    String getMessage();

    int getRange();
  }
  public enum ProjectErrorCode implements RESTErrorCode {

    //project error response
    PROJECT_EXISTS(1, "Project with the same name already exists.", Response.Status.CONFLICT),
    NUM_PROJECTS_LIMIT_REACHED(2, "You have reached the maximum number of projects you could create."
        + " Contact an administrator to increase your limit or delete some of the existing projects.",
            Response.Status.BAD_REQUEST),
    INVALID_PROJECT_NAME(3, "Invalid project name, valid characters: [a-zA-Z0-9]((?!__)[_a-zA-Z0-9]){0,62}",
        Response.Status.BAD_REQUEST),
    PROJECT_NOT_FOUND(4, "Project wasn't found.", Response.Status.BAD_REQUEST),
    PROJECT_NOT_REMOVED(5, "Project wasn't removed.", Response.Status.BAD_REQUEST),
    PROJECT_FOLDER_NOT_CREATED(7, "Project folder could not be created in HDFS.",
        Response.Status.INTERNAL_SERVER_ERROR),
    STARTER_PROJECT_BAD_REQUEST(8, "Type of starter project is not valid", Response.Status.BAD_REQUEST),
    PROJECT_FOLDER_NOT_REMOVED(9, "Project folder could not be removed from HDFS.",
        Response.Status.BAD_REQUEST),
    PROJECT_REMOVAL_NOT_ALLOWED(10, "Project can only be deleted by its owner.",
        Response.Status.FORBIDDEN),
    PROJECT_MEMBER_NOT_REMOVED(11, "Failed to remove team member.",
        Response.Status.INTERNAL_SERVER_ERROR),
    MEMBER_REMOVAL_NOT_ALLOWED(12,
        "Your project role does not allow to remove other members from this project.",
        Response.Status.FORBIDDEN),
    PROJECT_OWNER_NOT_ALLOWED(13, "Removing the project owner is not allowed.",
        Response.Status.FORBIDDEN),
    PROJECT_OWNER_ROLE_NOT_ALLOWED(14, "Changing the role of the project owner is not allowed.",
        Response.Status.FORBIDDEN),
    FOLDER_INODE_NOT_CREATED(15, "Folder Inode could not be created in DB.",
        Response.Status.BAD_REQUEST),
    FOLDER_NAME_NOT_SET(16, "Name cannot be empty.", Response.Status.BAD_REQUEST),
    FOLDER_NAME_TOO_LONG(17, "Name cannot be longer than 88 characters.", Response.Status.BAD_REQUEST),
    FOLDER_NAME_CONTAIN_DISALLOWED_CHARS(18, "Name cannot contain any of the characters ",
        Response.Status.BAD_REQUEST),

    FILE_NAME_EXIST(19, "File with the same name already exists.", Response.Status.BAD_REQUEST),
    FILE_NOT_FOUND(20, "File not found.", Response.Status.BAD_REQUEST),
    NO_MEMBER_TO_ADD(21, " No member to add.", Response.Status.BAD_REQUEST),
    NO_MEMBER_ADD(22, " No member added.", Response.Status.BAD_REQUEST),
    TEAM_MEMBER_NOT_FOUND(23, "The selected user is not a team member in this project.",
        Response.Status.NOT_FOUND),
    TEAM_MEMBER_ALREADY_EXISTS(24, " The selected user is already a team member of this project.",
        Response.Status.BAD_REQUEST),
    ROLE_NOT_SET(25, "Role cannot be empty.", Response.Status.BAD_REQUEST),
    PROJECT_NOT_SELECTED(26, "No project selected", Response.Status.BAD_REQUEST),
    QUOTA_NOT_FOUND(27, "Quota information not found.", Response.Status.BAD_REQUEST),
    QUOTA_ERROR(28, "Quota update error.", Response.Status.BAD_REQUEST),
    PROJECT_QUOTA_ERROR(29, "This project is out of credits.", Response.Status.PRECONDITION_FAILED),

    //project success messages
    PROJECT_CREATED(30, "Project created successfully.", Response.Status.CREATED),
    PROJECT_DESCRIPTION_CHANGED(31, "Project description changed.", Response.Status.OK),

    PROJECT_SERVICE_ADDED(33, "Project service added", Response.Status.OK),
    PROJECT_SERVICE_ADD_FAILURE(34, "Failure adding service", Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_REMOVED(35, "The project and all related files were removed successfully.",
        Response.Status.OK),
    PROJECT_REMOVED_NOT_FOLDER(36,
        "The project was removed successfully. But its datasets have not been deleted.",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_MEMBER_REMOVED(37, "Member removed successfully", Response.Status.OK),
    PROJECT_MEMBERS_ADDED(38, "Members added successfully", Response.Status.OK),
    PROJECT_MEMBER_ADDED(39, "One member added successfully", Response.Status.OK),
    MEMBER_ROLE_UPDATED(40, "Role updated successfully.", Response.Status.OK),
    MEMBER_REMOVED_FROM_TEAM(41, "Member removed from team.", Response.Status.OK),
    PROJECT_INODE_CREATION_ERROR(42, "Could not create dummy Inode",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_FOLDER_EXISTS(43, "A folder with same name as the project already exists in the system.",
        Response.Status.CONFLICT),
    PROJECT_USER_EXISTS(44, "Filesystem user(s) already exists in the system.",
        Response.Status.CONFLICT),
    PROJECT_GROUP_EXISTS(45, "Filesystem group(s) already exists in the system.",
        Response.Status.CONFLICT),
    PROJECT_CERTIFICATES_EXISTS(46, "Certificates for this project already exist in the system.",
        Response.Status.CONFLICT),
    PROJECT_QUOTA_EXISTS(47, "Quotas corresponding to this project already exist in the system.",
        Response.Status.CONFLICT),
    PROJECT_LOGS_EXIST(48, "Logs corresponding to this project already exist in the system.",
        Response.Status.CONFLICT),
    PROJECT_VERIFICATIONS_FAILED(49, "Error occurred while running verifications",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_SET_PERMISSIONS_ERROR(50, "Error occurred while setting permissions for project folders.",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HANDLER_PRECREATE_ERROR(51, "Error occurred during project precreate handler.",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HANDLER_POSTCREATE_ERROR(52, "Error occurred during project postcreate handler.",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HANDLER_PREDELETE_ERROR(53, "Error occurred during project predelete handler.",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HANDLER_POSTDELETE_ERROR(54, "Error occurred during project postdelete handler.",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_TOUR_FILES_ERROR(55, "Error while adding tour files to project.",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_KIBANA_CREATE_INDEX_ERROR(56, "Could not create kibana index-pattern for project",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_KIBANA_CREATE_SEARCH_ERROR(57, "Could not create kibana search for project",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_KIBANA_CREATE_DASHBOARD_ERROR(58, "Could not create kibana dashboard for project",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HIVEDB_CREATE_ERROR(59, "Could not create Hive DB for project",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_CONDA_LIBS_NOT_FOUND(60, "No preinstalled anaconda libs found.",
        Response.Status.NOT_FOUND),
    KILL_MEMBER_JOBS(61, "Could not kill user's yarn applications",
        Response.Status.INTERNAL_SERVER_ERROR),
    JUPYTER_SERVER_NOT_FOUND(62, "Could not find Jupyter entry for user in this project.",
        Response.Status.NOT_FOUND),
    PYTHON_LIB_ALREADY_INSTALLED(63, "This python library is already installed on this project",
        Response.Status.NOT_MODIFIED),
    PYTHON_LIB_NOT_INSTALLED(64,
        "This python library is not installed for this project. Cannot remove/upgrade " +
            "op", Response.Status.NOT_MODIFIED),
    ANACONDA_NOT_ENABLED(66, "First enable Anaconda. Click on 'Python' -> Activate Anaconda",
        Response.Status.PRECONDITION_FAILED),
    TENSORBOARD_OPENSEARCH_INDEX_NOT_FOUND(67, "Could not find OpenSearch index for TensorBoard.",
        Response.Status.NOT_FOUND),
    PROJECT_ROLE_FORBIDDEN(68, "Your project role does not allow to perform this action.",
        Response.Status.FORBIDDEN),
    FOLDER_NAME_ENDS_WITH_DOT(69, "Name cannot end in a period.", Response.Status.BAD_REQUEST),
    FOLDER_NAME_EXISTS(70, "A directory with the same name already exists. "
        + "If you want to replace it delete it first then try recreating.", Response.Status.BAD_REQUEST),
    PROJECT_SERVICE_NOT_FOUND(71, "service was not found.", Response.Status.BAD_REQUEST),
    QUOTA_REQUEST_NOT_COMPLETE(72, "Please specify both " + "namespace and space quota.", Response.Status.BAD_REQUEST),
    RESERVED_PROJECT_NAME(73, "Not allowed - reserved project name, pick another project name.",
        Response.Status.BAD_REQUEST),
    PROJECT_ANACONDA_ENABLE_ERROR(74, "Failed to enable conda.", Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_NAME_TOO_LONG(75, "Project name is too long - cannot be longer than 25 characters.",
        Response.Status.BAD_REQUEST),
    PROJECT_DOCKER_VERSION_EXTRACT_ERROR(76,
        "Failed to extract the hopsworks version of the docker image for this project.",
        Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_DEFAULT_JOB_CONFIG_NOT_FOUND(77, "Default job config not found",
      Response.Status.NOT_FOUND),
    ALERT_NOT_FOUND(78, "Alert not found", Response.Status.NOT_FOUND),
    ALERT_ILLEGAL_ARGUMENT(79, "Alert missing argument.", Response.Status.BAD_REQUEST),
    ALERT_ALREADY_EXISTS(80, "Alert with the same status already exists.", Response.Status.BAD_REQUEST),
    FAILED_TO_ADD_MEMBER(81, "Failed to add member.", Response.Status.BAD_REQUEST),
    FAILED_TO_CREATE_ROUTE(82, "Failed to create route.", Response.Status.BAD_REQUEST),
    FAILED_TO_DELETE_ROUTE(83, "Failed to delete route.", Response.Status.BAD_REQUEST),
    //project handler error response
    PROJECT_TEAM_ROLE_HANDLER_ADD_MEMBER_ERROR(84, "Error occurred during project team role add handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_TEAM_ROLE_HANDLER_UPDATE_MEMBERS_ERROR(85, "Error occurred during project team role update handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_TEAM_ROLE_HANDLER_REMOVE_MEMBER_ERROR(86, "Error occurred during project team role remove handler.",
      Response.Status.INTERNAL_SERVER_ERROR);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 150000;

    ProjectErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public int getRange() {
      return range;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

  }

  public enum DatasetErrorCode implements RESTErrorCode {

    //DataSet
    DATASET_OPERATION_FORBIDDEN(0, "Dataset/content operation forbidden", Response.Status.FORBIDDEN),
    DATASET_OPERATION_INVALID(1, "Operation cannot be performed.", Response.Status.BAD_REQUEST),
    DATASET_OPERATION_ERROR(2, "Dataset operation failed.", Response.Status.INTERNAL_SERVER_ERROR),
    DATASET_ALREADY_SHARED_WITH_PROJECT(3, "Dataset already shared with project",
        Response.Status.BAD_REQUEST),
    DATASET_NOT_SHARED_WITH_PROJECT(4, "Dataset is not shared with project.",
        Response.Status.BAD_REQUEST),
    DATASET_NAME_EMPTY(5, "DataSet name cannot be empty.", Response.Status.BAD_REQUEST),
    FILE_CORRUPTED_REMOVED_FROM_HDFS(6, "Corrupted file removed from hdfs.",
        Response.Status.BAD_REQUEST),
    INODE_DELETION_ERROR(7, "File/Dir could not be deleted.", Response.Status.INTERNAL_SERVER_ERROR),
    INODE_NOT_FOUND(8, "File not found.", Response.Status.NOT_FOUND),
    DATASET_REMOVED_FROM_HDFS(9, "DataSet removed from hdfs.", Response.Status.BAD_REQUEST),
    SHARED_DATASET_REMOVED(10, "The shared dataset has been removed from this project.",
        Response.Status.BAD_REQUEST),
    DATASET_NOT_FOUND(11, "DataSet not found.", Response.Status.BAD_REQUEST),
    DESTINATION_EXISTS(12, "Destination already exists.", Response.Status.BAD_REQUEST),
    DATASET_ALREADY_PUBLIC(13, "Dataset is already public.", Response.Status.CONFLICT),
    DATASET_ALREADY_IN_PROJECT(14, "Dataset is already in project.", Response.Status.BAD_REQUEST),
    DATASET_NOT_PUBLIC(15, "DataSet is not public.", Response.Status.BAD_REQUEST),
    DATASET_NOT_EDITABLE(16, "DataSet is not editable.", Response.Status.BAD_REQUEST),
    DATASET_PENDING(17, "DataSet is not yet accessible. Accept the share request to access it.",
        Response.Status.BAD_REQUEST),
    PATH_NOT_FOUND(18, "Path not found", Response.Status.BAD_REQUEST),
    PATH_NOT_DIRECTORY(19, "Requested path is not a directory", Response.Status.BAD_REQUEST),
    PATH_IS_DIRECTORY(20, "Requested path is a directory", Response.Status.BAD_REQUEST),
    DOWNLOAD_ERROR(21, "Failed to download.", Response.Status.BAD_REQUEST),
    DOWNLOAD_PERMISSION_ERROR(22, "Your role does not allow to download this file",
        Response.Status.BAD_REQUEST),
    DATASET_PERMISSION_ERROR(23, "Could not update dataset permissions",
        Response.Status.INTERNAL_SERVER_ERROR),
    COMPRESSION_ERROR(24, "Error while performing a (un)compress operation",
        Response.Status.INTERNAL_SERVER_ERROR),
    DATASET_OWNER_ERROR(25, "You cannot perform this action on a dataset you are not the owner",
        Response.Status.BAD_REQUEST),
    DATASET_PUBLIC_IMMUTABLE(26, "Public datasets are immutable.", Response.Status.BAD_REQUEST),
    DATASET_NAME_INVALID(28, "Name of dir is invalid", Response.Status.BAD_REQUEST),
    IMAGE_SIZE_INVALID(29,
      "Image is too big to display please download it by double-clicking it instead",
      Response.Status.BAD_REQUEST),
    FILE_PREVIEW_ERROR(30, "README.md too large to be previewd", Response.Status.BAD_REQUEST),
    DATASET_PARAMETERS_INVALID(31, "Invalid parameters for requested dataset operation",
        Response.Status.BAD_REQUEST),
    EMPTY_PATH(32, "Empty path requested", Response.Status.BAD_REQUEST),
    ONGOING_PERMISSION_OPERATION(33, "There is an ongoing permission operation", Response.Status.CONFLICT),
    UPLOAD_PATH_NOT_SPECIFIED(35, "The path to upload the template was not specified",
        Response.Status.BAD_REQUEST),
    README_NOT_ACCESSIBLE(36, "Readme not accessible.", Response.Status.UNAUTHORIZED),
    COMPRESSION_SIZE_ERROR(37,
        "Not enough free space on the local scratch directory to download and unzip this " +
            "file. Talk to your admin to increase disk space at the path: hopsworks/staging_dir",
        Response.Status.PRECONDITION_FAILED),
    INVALID_PATH_FILE(38, "The requested path does not resolve to a valid file",
        Response.Status.BAD_REQUEST),
    INVALID_PATH_DIR(39, "The requested path does not resolve to a valid directory",
        Response.Status.BAD_REQUEST),
    UPLOAD_DIR_CREATE_ERROR(40, "Uploads directory could not be created in the file system",
        Response.Status.INTERNAL_SERVER_ERROR),
    UPLOAD_CONCURRENT_ERROR(41, "A file with the same name is being uploaded",
        Response.Status.PRECONDITION_FAILED),
    UPLOAD_RESUMABLEINFO_INVALID(42, "ResumableInfo is invalid", Response.Status.BAD_REQUEST),
    UPLOAD_ERROR(43, "Error occurred while uploading file",
        Response.Status.INTERNAL_SERVER_ERROR),
    DATASET_REQUEST_EXISTS(44, "Request for this dataset from this project already exists.",
        Response.Status.CONFLICT),
    COPY_FROM_PROJECT(45, "Cannot copy file/folder from another project", Response.Status.FORBIDDEN),
    COPY_TO_PUBLIC_DS(46, "Can not copy to a public dataset.", Response.Status.FORBIDDEN),
    DATASET_SUBDIR_ALREADY_EXISTS(47, "A sub-directory with the same name already exists.",
        Response.Status.BAD_REQUEST),
    DOWNLOAD_NOT_ALLOWED(48, "Downloading files is not allowed. Please contact the system administrator for further " +
      "information.", Response.Status.FORBIDDEN),
    DATASET_REQUEST_ERROR(49, "Could not send dataset request", Response.Status.INTERNAL_SERVER_ERROR),
    DATASET_ACCESS_PERMISSION_DENIED(50, "Permission denied.", Response.Status.FORBIDDEN),
    PATH_ENCODING_NOT_SUPPORTED(51, "Unsupported encoding.", Response.Status.BAD_REQUEST),
    ATTACH_XATTR_ERROR(52, "Failed to attach Xattr.", Response.Status.INTERNAL_SERVER_ERROR),
    TARGET_PROJECT_NOT_FOUND(53, "Target project not found.", Response.Status.INTERNAL_SERVER_ERROR);


    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 110000;


    DatasetErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum MetadataErrorCode implements RESTErrorCode {

    TEMPLATE_ALREADY_AVAILABLE(0, "The template is already available", Response.Status.BAD_REQUEST),
    TEMPLATE_INODEID_EMPTY(1, "The template id is empty", Response.Status.BAD_REQUEST),
    TEMPLATE_NOT_ATTACHED(2, "The template could not be attached to a file", Response.Status.BAD_REQUEST),
    DATASET_TEMPLATE_INFO_MISSING(3, "Template info is missing. Please provide InodeDTO path and templateId.",
        Response.Status.BAD_REQUEST),
    NO_METADATA_EXISTS(4, "No metadata found", Response.Status.BAD_REQUEST),
    METADATA_MAX_SIZE_EXCEEDED(5, "Metadata is too large",
        Response.Status.BAD_REQUEST),
    METADATA_MISSING_FIELD(6, "Metadata missing attributed name.",
        Response.Status.BAD_REQUEST),
    METADATA_ERROR(7, "Error while processing the extended metadata.",
        Response.Status.INTERNAL_SERVER_ERROR),
    METADATA_ILLEGAL_NAME(8, "Metadata name is illegal.", Response.Status.BAD_REQUEST);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 180000;

    MetadataErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum JobErrorCode implements RESTErrorCode {

    // JOBS Error Messages
    JOB_START_FAILED(0, "An error occurred while trying to start this job. Check the job logs for details",
            Response.Status.BAD_REQUEST),
    JOB_STOP_FAILED(1, "An error occurred while trying to stop this job.", Response.Status.BAD_REQUEST),
    JOB_TYPE_UNSUPPORTED(2, "Unsupported job type.", Response.Status.BAD_REQUEST),
    JOB_ACTION_UNSUPPORTED(3, "Unsupported action type.", Response.Status.BAD_REQUEST),
    JOB_NAME_EMPTY(5, "Job name is not set.", Response.Status.BAD_REQUEST),
    JOB_NAME_INVALID(6, "Job name is invalid. Invalid charater(s) in job name", Response.Status.BAD_REQUEST),
    JOB_EXECUTION_NOT_FOUND(7, "Execution not found.", Response.Status.NOT_FOUND),
    JOB_EXECUTION_TRACKING_URL_NOT_FOUND(8, "Tracking url not found.", Response.Status.BAD_REQUEST),
    JOB_NOT_FOUND(9, "Job not found.", Response.Status.NOT_FOUND),
    JOB_EXECUTION_INVALID_STATE(10, "Execution state is invalid.", Response.Status.BAD_REQUEST),
    JOB_LOG(11, "Job log error.", Response.Status.BAD_REQUEST),
    JOB_DELETION_ERROR(12, "Error while deleting job.", Response.Status.BAD_REQUEST),
    JOB_CREATION_ERROR(13, "Error while creating job.", Response.Status.BAD_REQUEST),

    OPENSEARCH_INDEX_NOT_FOUND(14, "OpenSearch indices do not exist", Response.Status.BAD_REQUEST),
    OPENSEARCH_TYPE_NOT_FOUND(15, "OpenSearch type does not exist", Response.Status.BAD_REQUEST),
    TENSORBOARD_ERROR(16, "Error getting the TensorBoard(s) for this application", Response.Status.NO_CONTENT),
    APPLICATIONID_NOT_FOUND(17, "Error while deleting job.", Response.Status.BAD_REQUEST),
    JOB_ACCESS_ERROR(18, "Cannot access job", Response.Status.FORBIDDEN),
    LOG_AGGREGATION_NOT_ENABLED(19, "YARN log aggregation is not enabled",
        Response.Status.SERVICE_UNAVAILABLE),
    LOG_RETRIEVAL_ERROR(20, "Error while retrieving YARN logs", Response.Status.INTERNAL_SERVER_ERROR),

    JOB_SCHEDULE_UPDATE(21, "Could not update schedule.", Response.Status.INTERNAL_SERVER_ERROR),
    JAR_INSPECTION_ERROR(22, "Could not inspect jar file.", Response.Status.INTERNAL_SERVER_ERROR),
    PROXY_ERROR(23, "Could not get proxy user.", Response.Status.INTERNAL_SERVER_ERROR),

    JOB_CONFIGURATION_CONVERT_TO_JSON_ERROR(24, "Could not convert JobConfiguration to json",
        Response.Status.BAD_REQUEST),
    JOB_DELETION_FORBIDDEN(25, "Your role does not allow to delete this job.",  Response.Status.FORBIDDEN),
    UNAUTHORIZED_EXECUTION_ACCESS(26, "This execution does not belong to a job of this project. ",
      Response.Status.FORBIDDEN),
    APPID_NOT_FOUND(27, "AppId not found.", Response.Status.NOT_FOUND),
    JOB_PROGRAM_VERSIONING_FAILED(28, "Failed to version application program", Response.Status.INTERNAL_SERVER_ERROR),
    INSUFFICIENT_EXECUTOR_MEMORY(29, "Insufficient executor memory provided.", Response.Status.BAD_REQUEST),
    NODEMANAGERS_OFFLINE(30, "Nodemanagers are offline", Response.Status.SERVICE_UNAVAILABLE),
    DOCKER_MOUNT_NOT_ALLOWED(31, "It is not allowed to mount volumes.", Response.Status.BAD_REQUEST),
    DOCKER_MOUNT_DIR_NOT_ALLOWED(32, "It is not allowed to mount this directory.", Response.Status.BAD_REQUEST),
    DOCKER_UID_GID_STRICT(33, "Docker jobs run in uid/gid strict mode." +
            " It it now allowed to set uid/gid. If you remove the uid/gid, the job will run with a default user." +
            " Please ask an administrator to update the setting if necessary.",
            Response.Status.BAD_REQUEST),
    JOB_ALERT_NOT_FOUND(34, "Job alert not found", Response.Status.NOT_FOUND),
    JOB_ALERT_ILLEGAL_ARGUMENT(35, "Job alert missing argument.", Response.Status.BAD_REQUEST),
    JOB_ALERT_ALREADY_EXISTS(36, "Job alert with the same status already exists.", Response.Status.BAD_REQUEST),
    DOCKER_INVALID_JOB_PROPERTIES(37, "Received invalid job property values", Response.Status.BAD_REQUEST),
    FAILED_TO_CREATE_ROUTE(38, "Failed to create route.", Response.Status.BAD_REQUEST),
    FAILED_TO_DELETE_ROUTE(39, "Failed to delete route.", Response.Status.BAD_REQUEST),
    EXECUTIONS_LIMIT_REACHED(40, "Job reached the maximum number of executions.",
            Response.Status.BAD_REQUEST),
    JOB_ALREADY_EXISTS(41, "Job with this name already exists.", Response.Status.BAD_REQUEST),
    JOB_SCHEDULE_NOT_FOUND(42, "Cannot find the job schedule.", Response.Status.NOT_FOUND),
    UNMATCHED_JOB_NAME(43, "Provided job names do not match.", Response.Status.BAD_REQUEST),
    UNMATCHED_JOB_SCHEDULE_AND_JOB_NAME(44, "Requested job schedule id does not match the job name.",
      Response.Status.BAD_REQUEST);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 130000;


    JobErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum RequestErrorCode implements RESTErrorCode {

    MESSAGE_ACCESS_NOT_ALLOWED(0, "Message not allowed.", Response.Status.FORBIDDEN),
    EMAIL_EMPTY(1, "Email cannot be empty.", Response.Status.BAD_REQUEST),
    EMAIL_INVALID(2, "Not a valid email address.", Response.Status.BAD_REQUEST),
    DATASET_REQUEST_ERROR(3, "Error while submitting dataset request", Response.Status.BAD_REQUEST),
    REQUEST_UNKNOWN_ACTION(4, "Unknown request action", Response.Status.BAD_REQUEST),
    MESSAGE_NOT_FOUND(5, "Message was not found", Response.Status.NOT_FOUND);


    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 140000;

    RequestErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum ServiceErrorCode implements RESTErrorCode {

    JUPYTER_ADD_FAILURE(1, "Failed to create Jupyter notebook dir. Jupyter will not work properly. "
        + "Try recreating the following dir manually.", Response.Status.BAD_REQUEST),
    OPENSEARCH_SERVER_NOT_AVAILABLE(2, "The OpenSearch Server is either down or misconfigured.",
        Response.Status.BAD_REQUEST),
    OPENSEARCH_SERVER_NOT_FOUND(3, "Problem when reaching the OpenSearch server",
        Response.Status.SERVICE_UNAVAILABLE),

    //Hive
    HIVE_ADD_FAILURE(4, "Failed to create the Hive database", Response.Status.BAD_REQUEST),
    // LLAP
    LLAP_STATUS_INVALID(5, "Unrecognized new LLAP status", Response.Status.BAD_REQUEST),
    LLAP_CLUSTER_ALREADY_UP(6, "LLAP cluster already up", Response.Status.BAD_REQUEST),
    LLAP_CLUSTER_ALREADY_DOWN(7, "LLAP cluster already down", Response.Status.BAD_REQUEST),

    //Database
    DATABASE_UNAVAILABLE(8, "The database is temporarily unavailable. Please try again later",
        Response.Status.SERVICE_UNAVAILABLE),
    ZOOKEEPER_SERVICE_UNAVAILABLE(10, "ZooKeeper service unavailable",
        Response.Status.SERVICE_UNAVAILABLE),
    ANACONDA_NODES_UNAVAILABLE(11, "No conda machine is enabled. Contact the administrator.",
        Response.Status.SERVICE_UNAVAILABLE),
    OPENSEARCH_INDEX_CREATION_ERROR(12, "Error while creating index in opensearch",
        Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_LIST_LIB_FORMAT_ERROR(13,
        "Problem listing libraries. Did conda get upgraded and change its output format?",
        Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_LIST_LIB_ERROR(14, "Problem listing libraries. Please contact the Administrator",
        Response.Status.INTERNAL_SERVER_ERROR),
    JUPYTER_HOME_ERROR(16, "Couldn't resolve JUPYTER_HOME using DB.",
        Response.Status.INTERNAL_SERVER_ERROR),
    JUPYTER_STOP_ERROR(17, "Couldn't stop Jupyter Notebook Server.",
        Response.Status.INTERNAL_SERVER_ERROR),
    INVALID_YML(18, "Invalid .yml file", Response.Status.BAD_REQUEST),
    INVALID_YML_SIZE(19, ".yml file too large. Please set a higher value for variable " +
        "max_env_yml_byte_size",
      Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_FROM_YML_ERROR(20, "Failed to create Anaconda environment from .yml file.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PYTHON_INVALID_VERSION(21, "Invalid version of python (valid: '3.7'",
      Response.Status.BAD_REQUEST),
    ANACONDA_REPO_ERROR(22, "Problem adding the repo.", Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_OP_IN_PROGRESS(23,
        "A conda environment operation is currently executing (create/remove/list). Wait " +
            "for it to finish or clear it first.", Response.Status.PRECONDITION_FAILED),
    HOST_TYPE_NOT_FOUND(24, "No hosts with the desired capability.",
        Response.Status.PRECONDITION_FAILED),
    HOST_NOT_FOUND(25, "Host was not found.", Response.Status.NOT_FOUND),
    HOST_NOT_REGISTERED(26, "Host has not registered.", Response.Status.NOT_FOUND),
    ANACONDA_DEP_REMOVE_FORBIDDEN(27, "Could not uninstall library, it is a mandatory dependency",
        Response.Status.BAD_REQUEST),
    ANACONDA_DEP_INSTALL_FORBIDDEN(28, "Library is already installed", Response.Status.CONFLICT),
    ANACONDA_EXPORT_ERROR(29, "Failed to export Anaconda environment.",
        Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_LIST_LIB_NOT_FOUND(30, "No results found", Response.Status.NO_CONTENT),
    OPENSEARCH_INDEX_NOT_FOUND(31, "Index was not found in OpenSearch",
        Response.Status.NOT_FOUND),
    OPENSEARCH_INDEX_TYPE_NOT_FOUND(32, "Index type was not found in OpenSearch",
        Response.Status.NOT_FOUND),
    JUPYTER_SERVERS_NOT_FOUND(33, "Could not find any Jupyter notebook servers for this project.",
        Response.Status.NOT_FOUND),
    JUPYTER_SERVERS_NOT_RUNNING(34, "Could not find any Jupyter notebook servers for this project.",
        Response.Status.PRECONDITION_FAILED),
    JUPYTER_START_ERROR(35, "Jupyter server could not start.", Response.Status.INTERNAL_SERVER_ERROR),
    JUPYTER_SAVE_SETTINGS_ERROR(36, "Could not save Jupyter Settings.",
        Response.Status.INTERNAL_SERVER_ERROR),
    IPYTHON_CONVERT_ERROR(37, "Problem converting ipython notebook to python program",
        Response.Status.INTERNAL_SERVER_ERROR),
    EMAIL_SENDING_FAILURE(39, "Could not send email", Response.Status.INTERNAL_SERVER_ERROR),
    HOST_EXISTS(40, "Host exists", Response.Status.CONFLICT),
    TENSORFLOW_VERSION_NOT_SUPPORTED(41,
        "We currently do not support this version of TensorFlow. Update to a " +
            "newer version or contact an admin", Response.Status.BAD_REQUEST),
    SERVICE_GENERIC_ERROR(42, "Generic error while enabling the service", Response.Status.INTERNAL_SERVER_ERROR),
    JUPYTER_SERVER_ALREADY_RUNNING(43, "Jupyter Notebook Server is already running", Response.Status.BAD_REQUEST),
    ERROR_EXECUTING_REMOTE_COMMAND(44, "Error executing command over SSH", Response.Status.INTERNAL_SERVER_ERROR),
    OPERATION_NOT_SUPPORTED(45, "Supplied operation is not supported", Response.Status.BAD_REQUEST),

    GIT_COMMAND_FAILURE(46, "Git command failed to execute", Response.Status.BAD_REQUEST),
    JUPYTER_NOTEBOOK_VERSIONING_FAILED(47, "Failed to version notebook", Response.Status.INTERNAL_SERVER_ERROR),
    SERVICE_NOT_FOUND(48, "Service not found", Response.Status.NOT_FOUND),
    ACTION_FORBIDDEN(49, "Action forbidden", Response.Status.BAD_REQUEST),
    VARIABLE_NOT_FOUND(50, "Requested variable not found", Response.Status.NOT_FOUND),
    DOCKER_IMAGE_CREATION_ERROR(51, "Error while creating the docker image", Response.Status.INTERNAL_SERVER_ERROR),
    METASTORE_CONNECTION_ERROR(52, "Error opening connection with the Hive metastore",
        Response.Status.INTERNAL_SERVER_ERROR),
    SERVICE_DISCOVERY_ERROR(53, "Service not found", Response.Status.INTERNAL_SERVER_ERROR),
    WRONG_HDFS_USERNAME_PROVIDED_FOR_ATTACHING_JUPYTER_CONFIGURATION_TO_NOTEBOOK(54, "Failed to attach jupyter " +
        "configuration to notebook. Wrong hdfs username provided", Response.Status.BAD_REQUEST),
    ATTACHING_JUPYTER_CONFIG_TO_NOTEBOOK_FAILED(55, "Failed to attach jupyter configuration to notebook",
        Response.Status.INTERNAL_SERVER_ERROR),
    RM_METRICS_ERROR(56, "Failed to fetch utilization metrics", Response.Status.INTERNAL_SERVER_ERROR),
    PROMETHEUS_QUERY_ERROR(57, "Failed to execute prometheus query",
        Response.Status.INTERNAL_SERVER_ERROR),
    GRAFANA_PROXY_ERROR(58, "Unauthorized access to dashboard", Response.Status.INTERNAL_SERVER_ERROR),
    DOCKER_ERROR(59, "Failed to run docker command", Response.Status.INTERNAL_SERVER_ERROR),
    LOCAL_FILESYSTEM_ERROR(60, "Failed to write to local filesystem", Response.Status.INTERNAL_SERVER_ERROR),
    INVALID_DOCKER_COMMAND_FILE(61, "Invalid commands file provided", Response.Status.BAD_REQUEST),
    INVALID_ARTIFACT_FOR_DOCKER_COMMANDS(62, "Invalid artifact provided for docker commands",
        Response.Status.BAD_REQUEST),
    ENVIRONMENT_YAML_READ_ERROR(63, "Failed to read yaml file", Response.Status.INTERNAL_SERVER_ERROR),
    ENVIRONMENT_BUILD_NOT_FOUND(64, "Build not found in environment history",
        Response.Status.NOT_FOUND),
    ENVIRONMENT_HISTORY_READ_ERROR(65, "Failed to read environment history record from database",
        Response.Status.INTERNAL_SERVER_ERROR),
    ENVIRONMENT_HISTORY_CUSTOM_COMMANDS_FILE_READ_ERROR(66, "Failed to read custom command file",
        Response.Status.INTERNAL_SERVER_ERROR);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 100000;

    ServiceErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }

  }
  
  /**
   * Schema Registry codes are compatible with Confluent Schema Registry v5.3.1
   * https://docs.confluent.io/5.3.1/schema-registry/develop/api.html
   */
  public enum SchemaRegistryErrorCode implements RESTErrorCode {
    SUBJECT_NOT_FOUND(40401, "Subject not found", Response.Status.NOT_FOUND),
    VERSION_NOT_FOUND(40402, "Version not found", Response.Status.NOT_FOUND),
    SCHEMA_NOT_FOUND(40403, "Schema not found", Response.Status.NOT_FOUND),
    INCOMPATIBLE_AVRO_SCHEMA(40901, "Incompatible Avro schema", Response.Status.CONFLICT),
    INVALID_AVRO_SCHEMA(42201, "Invalid Avro schema", Status.UNPROCESSABLE_ENTITY),
    INVALID_VERSION(42202, "Invalid version", Status.UNPROCESSABLE_ENTITY),
    INVALID_COMPATIBILITY(42203, "Invalid compatibility level", Status.UNPROCESSABLE_ENTITY),
    INTERNAL_SERVER_ERROR(50001, "Error in the backend datastore", Response.Status.INTERNAL_SERVER_ERROR),
    OPERATION_TIMED_OUT(50002, "Operation timed out", Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_FORWARDING_REQUEST(50003, "Error while forwarding the request to the primary",
      Response.Status.INTERNAL_SERVER_ERROR);
  
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
  
    SchemaRegistryErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
      this.message = message;
      this.respStatus = respStatus;
    }
  
    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }
  
    @Override
    public Integer getCode() {
      return code;
    }
  
    @Override
    public String getMessage() {
      return message;
    }
  
    @Override
    public int getRange() {
      return 0;
    }
  }

  public enum KafkaErrorCode implements RESTErrorCode {

    TOPIC_NOT_FOUND(0, "No topics found", Response.Status.NOT_FOUND),
    BROKER_METADATA_ERROR(1, "An error occurred while retrieving topic metadata from broker",
        Response.Status.INTERNAL_SERVER_ERROR),
    TOPIC_ALREADY_EXISTS(2, "Kafka topic already exists in database. Pick a different topic name",
        Response.Status.CONFLICT),
    TOPIC_ALREADY_EXISTS_IN_ZOOKEEPER(3, "Kafka topic already exists in ZooKeeper. Pick a different topic name",
        Response.Status.CONFLICT),
    TOPIC_LIMIT_REACHED(4,
        "Topic limit reached. Contact your administrator to increase the number of topics " +
            "that can be created for this project.", Response.Status.PRECONDITION_FAILED),
    TOPIC_REPLICATION_ERROR(5, "Maximum topic replication factor exceeded", Response.Status.BAD_REQUEST),
    SCHEMA_NOT_FOUND(6, "Topic has no schema attached to it.", Response.Status.NOT_FOUND),
    KAFKA_GENERIC_ERROR(7, "An error occurred while retrieving information about Kafka",
        Response.Status.INTERNAL_SERVER_ERROR),
    DESTINATION_PROJECT_IS_TOPIC_OWNER(8, "Destination projet is topic owner",
        Response.Status.BAD_REQUEST),
    TOPIC_ALREADY_SHARED(9, "Topic is already shared", Response.Status.BAD_REQUEST),
    TOPIC_NOT_SHARED(10, "Topic is not shared with project", Response.Status.NOT_FOUND),
    ACL_ALREADY_EXISTS(11, "ACL already exists.", Response.Status.CONFLICT),
    ACL_NOT_FOUND(12, "ACL not found.", Response.Status.NOT_FOUND),
    ACL_NOT_FOR_TOPIC(13, "ACL does not belong to the specified topic", Response.Status.BAD_REQUEST),
    SCHEMA_IN_USE(14, "Schema is currently used by topics. topic", Response.Status.PRECONDITION_FAILED),
    BAD_NUM_PARTITION(15, "Invalid number of partitions", Response.Status.BAD_REQUEST),
    CREATE_SUBJECT_RESERVED_NAME(16, "The provided subject name is reserved for system calls",
      Response.Status.METHOD_NOT_ALLOWED),
    DELETE_RESERVED_SCHEMA(17, "The schema is reserved and cannot be deleted",
      Response.Status.METHOD_NOT_ALLOWED),
    SCHEMA_VERSION_NOT_FOUND(18, "Specified version of the schema not found", Response.Status.NOT_FOUND),
    PROJECT_IS_NOT_THE_OWNER_OF_THE_TOPIC(19, "Specified project is not the owner of the topic",
      Response.Status.BAD_REQUEST),
    ACL_FOR_ANY_USER(20, "Cannot create an ACL for user with email '*'", Response.Status.BAD_REQUEST),
    KAFKA_UNAVAILABLE(21, "Kafka is temporarily unavailable. Please try again later",
      Response.Status.SERVICE_UNAVAILABLE),
    TOPIC_DELETION_FAILED(22, "Could not delete Kafka topics.", Response.Status.INTERNAL_SERVER_ERROR),
    TOPIC_FETCH_FAILED(23, "Could not fetch topic details.", Response.Status.INTERNAL_SERVER_ERROR),
    TOPIC_CREATION_FAILED(24, "Could not create topic.", Response.Status.INTERNAL_SERVER_ERROR),
    BROKER_MISSING(25, "Could not find a broker endpoint.", Response.Status.NOT_FOUND);


    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 190000;


    KafkaErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum GenericErrorCode implements RESTErrorCode {

    UNKNOWN_ERROR(0, "A generic error occurred.", Response.Status.INTERNAL_SERVER_ERROR),
    ILLEGAL_ARGUMENT(1, "An argument was not provided or it was malformed.",
        Status.UNPROCESSABLE_ENTITY),
    ILLEGAL_STATE(2, "A runtime error occurred.", Response.Status.BAD_REQUEST),
    ROLLBACK(3, "The last transaction did not complete as expected",
        Response.Status.INTERNAL_SERVER_ERROR),
    WEBAPPLICATION(4, "Web application exception occurred", null),
    PERSISTENCE_ERROR(5, "Persistence error occurred", Response.Status.INTERNAL_SERVER_ERROR),
    UNKNOWN_ACTION(6, "This action can not be applied on this resource.", Response.Status.BAD_REQUEST),
    INCOMPLETE_REQUEST(7, "Some parameters were not provided or were not in the required format.",
        Response.Status.BAD_REQUEST),
    SECURITY_EXCEPTION(8, "A Java security error occurred.", Response.Status.INTERNAL_SERVER_ERROR),
    ENDPOINT_ANNOTATION_MISSING(9, "The requested endpoint did not have any project role annotation",
        Response.Status.SERVICE_UNAVAILABLE),
    ENTERPRISE_FEATURE(10, "This feature is only available in the enterprise edition", Response.Status.BAD_REQUEST),
    NOT_AUTHORIZED_TO_ACCESS(11, "Project not accessible to user", Response.Status.BAD_REQUEST),
    FEATURE_FLAG_NOT_ENABLED(12, "Platform feature not enabled", Response.Status.BAD_REQUEST);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 120000;

    GenericErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }

  }


  public enum SecurityErrorCode implements RESTErrorCode {

    MASTER_ENCRYPTION_PASSWORD_CHANGE(1,
        "Master password change procedure started. Check your inbox for final status", Response.Status.BAD_REQUEST),
    HDFS_ACCESS_CONTROL(2, "Access error while trying to access hdfs resource", Response.Status.FORBIDDEN),
    EJB_ACCESS_LOCAL(3, "Unauthorized invocation", Response.Status.UNAUTHORIZED),
    CERT_CREATION_ERROR(4, "Error while generating certificates.", Response.Status.INTERNAL_SERVER_ERROR),
    CERT_CN_EXTRACT_ERROR(5, "Error while extracting CN from certificate.", Response.Status.INTERNAL_SERVER_ERROR),
    CERT_ERROR(6, "Certificate could not be validated.", Response.Status.UNAUTHORIZED),
    CERT_ACCESS_DENIED(7, "Certificate access denied.", Response.Status.FORBIDDEN),
    CSR_ERROR(8, "Error while signing CSR.", Response.Status.INTERNAL_SERVER_ERROR),
    CERT_APP_REVOKE_ERROR(9, "Error while revoking application certificate, check the logs",
        Response.Status.INTERNAL_SERVER_ERROR),
    CERT_MATERIALIZATION_ERROR(10, "CertificateMaterializer error, could not materialize certificates",
        Response.Status.INTERNAL_SERVER_ERROR),
    MASTER_ENCRYPTION_PASSWORD_ACCESS_ERROR(11, "Could not read master encryption password.",
        Response.Status.INTERNAL_SERVER_ERROR),
    NOT_RENEWABLE_TOKEN(12, "Token can not be renewed.", Response.Status.BAD_REQUEST),
    INVALIDATION_ERROR(13, "Error while invalidating token.", Response.Status.EXPECTATION_FAILED),
    REST_ACCESS_CONTROL(14, "Client not authorized for this invocation.", Response.Status.FORBIDDEN),
    DUPLICATE_KEY_ERROR(15, "A signing key with the same name already exists.", Response.Status.CONFLICT),
    CERTIFICATE_REVOKATION_ERROR(16, "Error revoking the certificate", Response.Status.INTERNAL_SERVER_ERROR),
    CERTIFICATE_NOT_FOUND(17, "Could not find the certificate", Response.Status.BAD_REQUEST),
    CERTIFICATE_REVOKATION_USER_ERR(18, "Error revoking the certificate", Response.Status.BAD_REQUEST),
    CERTIFICATE_SIGN_USER_ERR(19, "Error signing the certificate", Response.Status.BAD_REQUEST),
    MASTER_ENCRYPTION_PASSWORD_RESET_ERROR(20, "Error resetting master encryption password.",
      Response.Status.INTERNAL_SERVER_ERROR);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 200000;


    SecurityErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum UserErrorCode implements RESTErrorCode {

    NO_ROLE_FOUND(0, "No valid role found for this user", Response.Status.UNAUTHORIZED),
    USER_DOES_NOT_EXIST(1, "User does not exist.", Response.Status.BAD_REQUEST),
    USER_WAS_NOT_FOUND(2, "User not found", Response.Status.NOT_FOUND),
    USER_EXISTS(3, "There is an existing account associated with this email", Response.Status.CONFLICT),
    ACCOUNT_REQUEST(4, "Your account has not yet been approved.", Response.Status.UNAUTHORIZED),
    ACCOUNT_DEACTIVATED(5, "This account has been deactivated.", Response.Status.UNAUTHORIZED),
    ACCOUNT_VERIFICATION(6, "You need to verify your account.", Response.Status.BAD_REQUEST),
    ACCOUNT_BLOCKED(7, "Your account has been blocked. Contact the administrator.",
        Response.Status.UNAUTHORIZED),
    AUTHENTICATION_FAILURE(8, "Authentication failed, invalid credentials", Response.Status.UNAUTHORIZED),
    LOGOUT_FAILURE(9, "Logout failed on backend.", Response.Status.BAD_REQUEST),

    PASSWORD_EMPTY(14, "Password cannot be empty.", Response.Status.BAD_REQUEST),
    PASSWORD_TOO_SHORT(15, "Password too short.", Response.Status.BAD_REQUEST),
    PASSWORD_TOO_LONG(16, "Password too long.", Response.Status.BAD_REQUEST),
    PASSWORD_INCORRECT(17, "Password incorrect", Response.Status.BAD_REQUEST),
    PASSWORD_PATTERN_NOT_CORRECT(18, "Password should include one uppercase letter, "
        + "one special character and/or alphanumeric characters.", Response.Status.BAD_REQUEST),
    INCORRECT_PASSWORD(19, "The password is incorrect. Please try again", Response.Status.UNAUTHORIZED),
    PASSWORD_MISS_MATCH(20, "Passwords do not match - typo?", Response.Status.BAD_REQUEST),
    TOS_NOT_AGREED(21, "You must agree to our terms of use.", Response.Status.BAD_REQUEST),
    CERT_DOWNLOAD_DENIED(22, "Admin is not allowed to download certificates", Response.Status.BAD_REQUEST),

    //success response
    CREATED_ACCOUNT(23, "You have successfully created an account\n"
        + "but you might need to wait until your account has been approved \n"
        + "before you can login.", Response.Status.BAD_REQUEST),
    PASSWORD_RESET_SUCCESSFUL(24,
        "Your password was successfully reset your new password have been sent to your email.",
        Response.Status.BAD_REQUEST),
    PASSWORD_RESET_UNSUCCESSFUL(25,
        "Your password could not be reset. Please try again later or contact support.",
        Response.Status.BAD_REQUEST),
    PASSWORD_CHANGED(26, "Your password was successfully changed.", Response.Status.BAD_REQUEST),
    PROFILE_UPDATED(28, "Your profile was updated successfully.", Response.Status.BAD_REQUEST),
    SSH_KEY_REMOVED(29, "Your ssh key was deleted successfully.", Response.Status.BAD_REQUEST),
    NOTHING_TO_UPDATE(30, "Nothing to update", Response.Status.BAD_REQUEST),
    CREATE_USER_ERROR(31, "Error while creating user", Response.Status.INTERNAL_SERVER_ERROR),
    CERT_AUTHORIZATION_ERROR(32, "Certificate CN does not match the username provided.",
        Response.Status.UNAUTHORIZED),
    PROJECT_USER_CERT_NOT_FOUND(33, "Could not find exactly one certificate for user in project.",
        Response.Status.FORBIDDEN),
    ACCOUNT_INACTIVE(34, "This account has not been activated", Response.Status.UNAUTHORIZED),
    ACCOUNT_LOST_DEVICE(35, "This account has registered a lost device.", Response.Status.UNAUTHORIZED),
    ACCOUNT_NOT_APPROVED(36, "This account has not yet been approved", Response.Status.UNAUTHORIZED),
    INVALID_EMAIL(37, "Invalid email format.", Response.Status.BAD_REQUEST),
    INCORRECT_DEACTIVATION_LENGTH(38, "The message should have a length between 5 and 500 characters",
        Response.Status.BAD_REQUEST),
    TMP_CODE_INVALID(39, "The temporary code was wrong.", Response.Status.UNAUTHORIZED),
    INCORRECT_CREDENTIALS(40, "Incorrect email or password.", Response.Status.BAD_REQUEST),
    INCORRECT_VALIDATION_KEY(41, "Incorrect validation key", Response.Status.BAD_REQUEST),
    ACCOUNT_ALREADY_VERIFIED(42, "User is already verified", Response.Status.CONFLICT),
    TWO_FA_ENABLE_ERROR(43, "Cannot enable 2-factor authentication.",
        Response.Status.INTERNAL_SERVER_ERROR),
    ACCOUNT_REGISTRATION_ERROR(44, "Account registration error.", Response.Status.INTERNAL_SERVER_ERROR),
    TWO_FA_DISABLED(45, "2-factor authentication is disabled.", Response.Status.PRECONDITION_FAILED),
    TRANSITION_STATUS_ERROR(46, "The user can't transition from current status to requested status",
      Response.Status.BAD_REQUEST),
    ACCESS_CONTROL(47, "Client not authorized for this invocation.", Response.Status.FORBIDDEN),
    SECRET_EMPTY(48, "Secret is empty", Response.Status.NOT_FOUND),
    SECRET_EXISTS(49, "Same Secret already exists", Response.Status.CONFLICT),
    SECRET_ENCRYPTION_ERROR(50, "Error encrypting/decrypting Secret", Response.Status.INTERNAL_SERVER_ERROR),
    ACCOUNT_NOT_ACTIVE(51, "This account is not active", Response.Status.BAD_REQUEST),
    ACCOUNT_ACTIVATION_FAILED(52, "Account activation failed", Response.Status.BAD_REQUEST),
    ROLE_NOT_FOUND(53, "Role not found", Response.Status.BAD_REQUEST),
    ACCOUNT_DELETION_ERROR(54, "Failed to delete account.", Response.Status.BAD_REQUEST),
    USER_NAME_NOT_SET(55, "User name not set.", Response.Status.BAD_REQUEST),
    SECRET_DELETION_FAILED(56, "Failed to delete secret.", Response.Status.BAD_REQUEST),
    USER_SEARCH_NOT_ALLOWED(57, "Search not allowed.", Response.Status.BAD_REQUEST),
    FAILED_TO_GENERATE_QR_CODE(58, "Failed to generate QR code.", Response.Status.EXPECTATION_FAILED),
    INVALID_OTP(59, "Invalid OTP.", Response.Status.BAD_REQUEST),
    USER_ACCOUNT_HANDLER_CREATE_ERROR(60, "Error occurred during user account update handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    USER_ACCOUNT_HANDLER_UPDATE_ERROR(61, "Error occurred during user account update handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    USER_ACCOUNT_HANDLER_REMOVE_ERROR(62, "Error occurred during user account remove handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    OPERATION_NOT_ALLOWED(63, "Operation not allowed on user", Response.Status.BAD_REQUEST),
    ACCOUNT_REJECTION_FAILED(64, "Account rejection failed", Response.Status.BAD_REQUEST),
    SECRET_CREATION_FAILED(65, "Secret creation failed", Response.Status.INTERNAL_SERVER_ERROR);


    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 160000;


    UserErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum CAErrorCode implements RESTErrorCode {

    BADSIGNREQUEST(0, "No CSR provided or CSR is malformed", Response.Status.BAD_REQUEST),
    BADREVOKATIONREQUEST(1, "No certificate identifier provided", Response.Status.BAD_REQUEST),
    CERTNOTFOUND(2, "Certificate not found", Response.Status.NO_CONTENT),
    CERTEXISTS(3, "Certificate with the same identifier already exists", Response.Status.BAD_REQUEST),
    BAD_SUBJECT_NAME(4, "Invalid certificate subject name", Response.Status.BAD_REQUEST),
    CERTIFICATE_DECODING_ERROR(5, "Could not decode certificate", Response.Status.INTERNAL_SERVER_ERROR),
    CERTIFICATE_REVOCATION_FAILURE(6, "Failed to revoke certificate", Response.Status.INTERNAL_SERVER_ERROR),
    CERTIFICATE_REVOCATION_LIST_READ(7, "Failed to read Certificate Revocation List",
        Response.Status.INTERNAL_SERVER_ERROR),
    CSR_GENERIC_ERROR(8, "Error handling certificate signing request", Response.Status.INTERNAL_SERVER_ERROR),
    CSR_SIGNING_ERROR(9, "Could not sign Certificate Signing Request", Response.Status.INTERNAL_SERVER_ERROR),
    CA_INITIALIZATION_ERROR(10, "Error while initializing Certificate Authorities",
        Response.Status.INTERNAL_SERVER_ERROR),
    PKI_GENERIC_ERROR(11, "Generic PKI error", Response.Status.INTERNAL_SERVER_ERROR);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 220000;

    CAErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum DelaCSRErrorCode implements RESTErrorCode {

    BADREQUEST(0, "User or CS not set", Response.Status.BAD_REQUEST),
    EMAIL(1, "CSR email not set or does not match user", Response.Status.UNAUTHORIZED),
    CN(3, "CSR common name not set", Response.Status.BAD_REQUEST),
    O(4, "CSR organization name not set", Response.Status.BAD_REQUEST),
    OU(5, "CSR organization unit name not set", Response.Status.BAD_REQUEST),
    NOTFOUND(6, "No cluster registered with the given organization name and organizational unit",
        Response.Status.BAD_REQUEST),
    SERIALNUMBER(7, "Cluster has already a signed certificate", Response.Status.BAD_REQUEST),
    CNNOTFOUND(8, "No cluster registered with the CSR common name", Response.Status.BAD_REQUEST),
    AGENTIDNOTFOUND(9, "No cluster registered for the user", Response.Status.UNAUTHORIZED);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 230000;


    DelaCSRErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum ServingErrorCode implements RESTErrorCode {

    INSTANCE_NOT_FOUND(0, "Serving instance not found", Response.Status.NOT_FOUND),
    DELETION_ERROR(1, "Serving instance could not be deleted",
        Response.Status.INTERNAL_SERVER_ERROR),
    UPDATE_ERROR(2, "Serving instance could not be updated", Response.Status.INTERNAL_SERVER_ERROR),
    LIFECYCLE_ERROR(3, "Serving instance could not be started/stopped", Response.Status.BAD_REQUEST),
    LIFECYCLE_ERROR_INT(4, "Serving instance could not be started/stopped",
        Response.Status.INTERNAL_SERVER_ERROR),
    STATUS_ERROR(5, "Error getting model server instance status", Response.Status.INTERNAL_SERVER_ERROR),
    MODEL_PATH_NOT_FOUND(6, "Model path not found", Response.Status.BAD_REQUEST),
    COMMAND_NOT_RECOGNIZED(7, "Command not recognized", Response.Status.BAD_REQUEST),
    COMMAND_NOT_PROVIDED(8, "Command not provided", Response.Status.BAD_REQUEST),
    SPEC_NOT_PROVIDED(9, "TFServing spec not provided", Response.Status.BAD_REQUEST),
    BAD_TOPIC(10, "Topic provided cannot be used for Serving logging", Response.Status.BAD_REQUEST),
    DUPLICATED_ENTRY(11, "An entry with the same name already exists in this project",
      Response.Status.BAD_REQUEST),
    PYTHON_ENVIRONMENT_NOT_ENABLED(12, "Python environment has not been enabled in this project, " +
      "which is required for serving SkLearn Models", Response.Status.BAD_REQUEST),
    UPDATE_MODEL_SERVER_ERROR(13, "The model server of a deployment cannot be updated.",
      Response.Status.BAD_REQUEST),
    KUBERNETES_NOT_INSTALLED(14, "Kubernetes is not installed", Response.Status.BAD_REQUEST),
    KSERVE_NOT_ENABLED(15, "KServe is not installed or disabled", Response.Status.BAD_REQUEST),
    SCRIPT_NOT_FOUND(16, "Script not found", Response.Status.BAD_REQUEST),
    MODEL_FILES_STRUCTURE_NOT_VALID(17, "Model path does not have a valid file structure",
      Response.Status.BAD_REQUEST),
    MODEL_ARTIFACT_NOT_VALID(18, "Model artifact not valid", Response.Status.BAD_REQUEST),
    MODEL_ARTIFACT_OPERATION_ERROR(19, "Model artifact cannot be created or changed", Response.Status.BAD_REQUEST),
    PREDICTOR_NOT_SUPPORTED(20, "Predictors not supported", Response.Status.BAD_REQUEST),
    TRANSFORMER_NOT_SUPPORTED(21, "Transformers not supported", Response.Status.BAD_REQUEST),
    KAFKA_TOPIC_NOT_FOUND(22, "Kafka topic not found", Response.Status.BAD_REQUEST),
    KAFKA_TOPIC_NOT_VALID(23, "Kafka topic not valid", Response.Status.BAD_REQUEST),
    FINEGRAINED_INF_LOGGING_NOT_SUPPORTED(24, "Fine-grained inference logging not supported",
      Response.Status.BAD_REQUEST),
    REQUEST_BATCHING_NOT_SUPPORTED(25, "Request batching not supported", Response.Status.BAD_REQUEST),
    CREATE_ERROR(26, "Serving instance could not be created", Response.Status.BAD_REQUEST),
    SERVER_LOGS_NOT_AVAILABLE(27, "Server logs not available", Response.Status.NOT_FOUND);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 240000;


    ServingErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }


  public enum InferenceErrorCode implements RESTErrorCode {

    SERVING_NOT_FOUND(0, "Serving instance not found", Response.Status.NOT_FOUND),
    SERVING_NOT_RUNNING(1, "Serving instance not running", Response.Status.BAD_REQUEST),
    REQUEST_ERROR(2, "Error contacting the serving server", Response.Status.INTERNAL_SERVER_ERROR),
    EMPTY_RESPONSE(3, "Empty response from the serving server", Response.Status.INTERNAL_SERVER_ERROR),
    BAD_REQUEST(4, "Request malformed", Response.Status.BAD_REQUEST),
    MISSING_VERB(5, "Verb is missing", Response.Status.BAD_REQUEST),
    ERROR_READING_RESPONSE(6, "Error while reading the response", Response.Status.INTERNAL_SERVER_ERROR),
    SERVING_INSTANCE_INTERNAL(7, "Serving instance internal error", Response.Status.INTERNAL_SERVER_ERROR),
    SERVING_INSTANCE_BAD_REQUEST(8, "Serving instance bad request error", Response.Status.BAD_REQUEST),
    REQUEST_AUTH_TYPE_NOT_SUPPORTED(9, "Authentication type not supported", Response.Status.BAD_REQUEST),
    UNAUTHORIZED(10, "Unauthorized request", Response.Status.UNAUTHORIZED),
    FORBIDDEN(11, "Forbidden request", Response.Status.FORBIDDEN),
    ENDPOINT_NOT_FOUND(12, "Inference endpoint not found", Response.Status.INTERNAL_SERVER_ERROR);

    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 250000;

    InferenceErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum ActivitiesErrorCode implements RESTErrorCode {

    FORBIDDEN(0, "You are not allow to perform this action.", Response.Status.FORBIDDEN),
    ACTIVITY_NOT_FOUND(1, "Activity instance not found", Response.Status.NOT_FOUND),
    ACTIVITY_NOT_SUPPORTED(2, "Activity type not supported", Response.Status.BAD_REQUEST);

    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 260000;

    ActivitiesErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  /**
   * Provides http status codes not available in Response.StatusType.
   */
  public enum Status implements Response.StatusType {
    UNPROCESSABLE_ENTITY(422, "UNPROCESSABLE_ENTITY");

    private final int code;
    private final String reason;
    private final Response.Status.Family family;

    Status(int statusCode, String reasonPhrase) {
      this.code = statusCode;
      this.reason = reasonPhrase;
      this.family = Response.Status.Family.familyOf(statusCode);
    }

    public static Status fromStatus(final int statusCode) {
      for (Status status : Status.values()) {
        if (status.getStatusCode() == statusCode) {
          return status;
        }
      }
      return null;
    }

    @Override
    public int getStatusCode() {
      return this.code;
    }

    @Override
    public Response.Status.Family getFamily() {
      return this.family;
    }

    @Override
    public String getReasonPhrase() {
      return this.toString();
    }

    @Override
    public String toString() {
      return this.reason;
    }
  }

  /**
   * Error codes for the featurestore microservice on Hopsworks
   */
  public enum FeaturestoreErrorCode implements RESTErrorCode {

    COULD_NOT_CREATE_FEATUREGROUP(1, "Could not create feature group and corresponding Hive table",
        Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_ID_NOT_PROVIDED(2, "Featurestore Id was not provided", Response.Status.BAD_REQUEST),
    FEATUREGROUP_ID_NOT_PROVIDED(3, "Featuregroup Id was not provided", Response.Status.BAD_REQUEST),
    FEATUREGROUP_VERSION_NOT_PROVIDED(4, "Featuregroup version was not provided", Response.Status.BAD_REQUEST),
    COULD_NOT_DELETE_FEATUREGROUP(5, "Could not delete feature group and corresponding Hive table",
        Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_CREATE_FEATURESTORE(6, "Could not create feature store and corresponding Hive database",
        Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_PREVIEW_FEATUREGROUP(7, "Could not preview the contents of the feature group ",
        Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_NOT_FOUND(8, "Featurestore wasn't found.", Response.Status.NOT_FOUND),
    FEATUREGROUP_NOT_FOUND(9, "Featuregroup wasn't found.", Response.Status.NOT_FOUND),
    COULD_NOT_FETCH_FEATUREGROUP_SHOW_CREATE_SCHEMA(10,
        "The query SHOW CREATE SCHEMA for the featuregroup in Hive failed.",
        Response.Status.INTERNAL_SERVER_ERROR),
    TRYING_TO_UNSHARE_A_FEATURESTORE_THAT_IS_NOT_SHARED(11, "Trying to un-share a featurestore that is not shared",
        Response.Status.BAD_REQUEST),
    TRAINING_DATASET_NOT_FOUND(12, "Training dataset wasn't found.", Response.Status.NOT_FOUND),
    TRAINING_DATASET_ID_NOT_PROVIDED(13, "Training dataset Id was not provided", Response.Status.BAD_REQUEST),
    COULD_NOT_DELETE_TRAINING_DATASET(14, "Could not delete training dataset",
        Response.Status.INTERNAL_SERVER_ERROR),
    CLONE_ID_NOT_PROVIDED(15, "Clone Id not provided despite requesting to clone feature group version",
        Response.Status.BAD_REQUEST),
    TRAINING_DATASET_ALREADY_EXISTS(16, "The provided training dataset name already exists",
        Response.Status.BAD_REQUEST),
    NO_PRIMARY_KEY_SPECIFIED(17, "A feature group or training dataset must have a primary key specified",
        Response.Status.BAD_REQUEST),
    CERTIFICATES_NOT_FOUND(18, "Could not find user certificates for authenticating with Hive Feature Store",
        Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_INITIATE_HIVE_CONNECTION(19, "Could not initiate connection to Hive Server",
        Response.Status.INTERNAL_SERVER_ERROR),
    HIVE_UPDATE_STATEMENT_ERROR(20, "Hive Update Statement failed",
        Response.Status.INTERNAL_SERVER_ERROR),
    HIVE_READ_QUERY_ERROR(21, "Hive Read Query failed",
        Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_NAME_NOT_PROVIDED(23, "Featurestore name was not provided", Response.Status.BAD_REQUEST),
    FORBIDDEN_FEATURESTORE_OPERATION(24, "User is forbidden to enact these changes",
        Response.Status.FORBIDDEN),
    STORAGE_CONNECTOR_ID_NOT_PROVIDED(25, "Storage backend id not provided", Response.Status.BAD_REQUEST),
    CANNOT_FETCH_HIVE_SCHEMA_FOR_ON_DEMAND_FEATUREGROUPS(26, "Fetching Hive Schema of On-demand feature groups is not" +
      " supported", Response.Status.BAD_REQUEST),
    ON_DEMAND_FEATUREGROUP_JDBC_CONNECTOR_NOT_FOUND(27, "The JDBC Connector for the on-demand feature group could not" +
      " be found", Response.Status.NOT_FOUND),
    PREVIEW_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS(28, "Fetching Hive Schema of On-demand feature groups is not" +
      " supported", Response.Status.BAD_REQUEST),
    CLEAR_OPERATION_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS(29, "Clearing Feature Group contents is not supported " +
      "for on-demand feature groups", Response.Status.BAD_REQUEST),
    ILLEGAL_STORAGE_CONNECTOR_NAME(30, "Illegal storage connector name", Response.Status.BAD_REQUEST),
    ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION(31, "Illegal storage connector description",
        Response.Status.BAD_REQUEST),
    ILLEGAL_JDBC_CONNECTION_STRING(32, "Illegal JDBC Connection String", Response.Status.BAD_REQUEST),
    ILLEGAL_JDBC_CONNECTION_ARGUMENTS(33, "Illegal JDBC Connection Arguments", Response.Status.BAD_REQUEST),
    ILLEGAL_S3_CONNECTOR_BUCKET(34, "Illegal S3 connector bucket", Response.Status.BAD_REQUEST),
    ILLEGAL_S3_CONNECTOR_ACCESS_KEY(35, "Illegal S3 connector access key", Response.Status.BAD_REQUEST),
    ILLEGAL_S3_CONNECTOR_SECRET_KEY(36, "Illegal S3 connector secret key", Response.Status.BAD_REQUEST),
    ILLEGAL_HOPSFS_CONNECTOR_DATASET(37, "Illegal Hopsfs connector dataset", Response.Status.BAD_REQUEST),
    ILLEGAL_FEATURE_NAME(40, "Illegal feature name", Response.Status.BAD_REQUEST),
    ILLEGAL_FEATURE_DESCRIPTION(41, "Illegal feature description", Response.Status.BAD_REQUEST),
    CONNECTOR_NOT_FOUND(42, "Connector not found", Response.Status.NOT_FOUND),
    CONNECTOR_ID_NOT_PROVIDED(43, "Connector Id was not provided", Response.Status.BAD_REQUEST),
    INVALID_SQL_QUERY(44, "Invalid SQL query", Response.Status.BAD_REQUEST),
    HOPSFS_CONNECTOR_NOT_FOUND(46, "HopsFs Connector not found", Response.Status.NOT_FOUND),
    STORAGE_CONNECTOR_TYPE_NOT_PROVIDED(47, "Storage Connector Type was not provided", Response.Status.BAD_REQUEST),
    COULD_NOT_CLEAR_FEATUREGROUP(48, "Could not clear contents of feature group",
        Response.Status.INTERNAL_SERVER_ERROR),
    ILLEGAL_FEATUREGROUP_TYPE(49, "The provided feature group type was not recognized",
        Response.Status.BAD_REQUEST),
    ILLEGAL_TRAINING_DATASET_TYPE(50, "The provided training dataset type was not recognized",
        Response.Status.BAD_REQUEST),
    CAN_ONLY_GET_INODE_FOR_HOPSFS_TRAINING_DATASETS(51, "Getting the inode id of a non-hopsfs training dataset is not" +
        " supported", Response.Status.BAD_REQUEST),
    TRAINING_DATASET_VERSION_NOT_PROVIDED(52, "Training Dataset version was not provided",
        Response.Status.BAD_REQUEST),
    S3_CONNECTOR_ID_NOT_PROVIDED(54, "S3 Connector Id was not provided", Response.Status.BAD_REQUEST),
    HOPSFS_CONNECTOR_ID_NOT_PROVIDED(55, "HopsFS Connector Id was not provided", Response.Status.BAD_REQUEST),
    ILLEGAL_TRAINING_DATASET_DATA_FORMAT(57, "Illegal training dataset data format",
        Response.Status.BAD_REQUEST),
    ILLEGAL_TRAINING_DATASET_VERSION(58, "Illegal training dataset version",
        Response.Status.BAD_REQUEST),
    ILLEGAL_FEATUREGROUP_VERSION(59, "Illegal feature group version",
        Response.Status.BAD_REQUEST),
    ILLEGAL_STORAGE_CONNECTOR_TYPE(60, "The provided storage connector type is not valid",
        Response.Status.BAD_REQUEST),
    FEATURESTORE_INITIALIZATION_ERROR(61, "Featurestore Initialization Error", Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_UTIL_ARGS_FAILURE(62, "Could not write featurestore util args to HDFS",
        Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_ONLINE_SECRETS_ERROR(63, "Could not get JDBC connection for the online featurestore",
        Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_ONLINE_NOT_ENABLED(64, "Online featurestore not enabled", Response.Status.BAD_REQUEST),
    SYNC_TABLE_NOT_FOUND(65, "The Hive Table to Sync with the feature store was not " +
        "found in the metastore", Response.Status.BAD_REQUEST),
    COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE(66, "Could not initiate connection to " +
        "MySQL Server", Response.Status.INTERNAL_SERVER_ERROR),
    MYSQL_JDBC_UPDATE_STATEMENT_ERROR(67, "MySQL JDBC Update Statement failed",
        Response.Status.INTERNAL_SERVER_ERROR),
    MYSQL_JDBC_READ_QUERY_ERROR(68, "MySQL JDBC Read Query failed",
        Response.Status.INTERNAL_SERVER_ERROR),
    ONLINE_FEATURE_SERVING_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS(69, "Online Feature Serving is only" +
        "supported for feature groups that are cached inside Hopsworks", Response.Status.BAD_REQUEST),
    ERROR_CREATING_ONLINE_FEATURESTORE_DB(70, "An error occurred when trying to create " +
        "the MySQL database for an online feature store", Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_CREATING_ONLINE_FEATURESTORE_USER(71, "An error occurred when trying to create " +
        "the MySQL database user for an online feature store", Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_DELETING_ONLINE_FEATURESTORE_DB(72, "An error occurred when trying to delete " +
        "the MySQL database for an online feature store", Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_DELETING_ONLINE_FEATURESTORE_USER(73, "An error occurred when trying to delete " +
        "the MySQL user for an online feature store", Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_GRANTING_ONLINE_FEATURESTORE_USER_PRIVILEGES(74, "An error occurred when trying to " +
        "grant/revoke privileges to a MySQL user for an online feature store", Response.Status.INTERNAL_SERVER_ERROR),
    ONLINE_FEATUREGROUP_CANNOT_BE_PARTITIONED(75, "An error occurred when trying to " +
        "create the MySQL table for the online feature group. User-defined partitioning is not supported for MySQL " +
        "tables", Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_CREATE_DATA_VALIDATION_RULES(76, "Failed to create data validation rules",
        Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_READ_DATA_VALIDATION_RESULT(77, "Failed to read data validation result",
        Response.Status.INTERNAL_SERVER_ERROR),
    IMPORT_JOB_ALREADY_RUNNING(78, "A job to import this featuregroup is already running",
        Response.Status.BAD_REQUEST),
    IMPORT_CONF_ERROR(79, "Error writing import job configuration", Response.Status.INTERNAL_SERVER_ERROR),
    TRAININGDATASETJOB_FAILURE(80, "Could not write featurestore cloud args to HDFS",
        Response.Status.INTERNAL_SERVER_ERROR),
    TRAININGDATASETJOB_DUPLICATE_FEATURE(81, "Feature list contains duplicate", Response.Status.BAD_REQUEST),
    FEATURE_DOES_NOT_EXIST(82, "Feature does not exist", Response.Status.BAD_REQUEST),
    TRAININGDATASETJOB_FEATUREGROUP_DUPLICATE(83, "Multiple featuregroups contain feature",
        Response.Status.BAD_REQUEST),
    TRAININGDATASETJOB_TRAININGDATASET_VERSION_EXISTS(84, "Illegal training dataset name - version combination",
        Response.Status.BAD_REQUEST),
    TRAININGDATASETJOB_CONF_ERROR(85, "Error writing training dataset job configuration to hdfs",
        Response.Status.INTERNAL_SERVER_ERROR),
    S3_KEYS_FORBIDDEN(86, "IAM role is configured for this instance. AWS access/secret keys are not allowed",
        Response.Status.BAD_REQUEST),
    MISSING_REDSHIFT_DRIVER(87, "Could not find Redshift JDBC driver. " +
        "Please upload it in Resources/RedshiftJDBC42-no-awssdk.jar", Response.Status.BAD_REQUEST),
    TRAININGDATASETJOB_MISSPECIFICATION(88, "Training dataset job is misspecified and cannot be created",
        Response.Status.BAD_REQUEST),
    FEATUREGROUP_EXISTS(89, "The feature group you are trying to create does already exist.",
        Response.Status.BAD_REQUEST),
    XATTRS_OPERATIONS_ONLY_SUPPORTED_FOR_CACHED_FEATUREGROUPS(90, "Attaching " +
        "extended attributes is only supported for cached featuregroups.",
        Response.Status.BAD_REQUEST),
    ILLEGAL_ENTITY_NAME(91, "Illegal feature store entity name", Response.Status.BAD_REQUEST),
    ILLEGAL_ENTITY_DESCRIPTION(92, "Illegal featurestore entity description", Response.Status.BAD_REQUEST),
    FEATUREGROUP_NAME_NOT_PROVIDED(94, "Feature group name was not provided", Response.Status.BAD_REQUEST),
    TRAINING_DATASET_NAME_NOT_PROVIDED(95, "Training dataset name was not provided", Response.Status.BAD_REQUEST),
    NO_PK_JOINING_KEYS(96, "Could not find any matching feature to join", Response.Status.BAD_REQUEST),
    LEFT_RIGHT_ON_DIFF_SIZES(97, "LeftOn and RightOn have different sizes", Response.Status.BAD_REQUEST),
    ILLEGAL_TRAINING_DATASET_SPLIT_NAME(98, "Illegal training dataset split name", Response.Status.BAD_REQUEST),
    ILLEGAL_TRAINING_DATASET_SPLIT_PERCENTAGE(99, "Illegal training dataset split percentage",
        Response.Status.BAD_REQUEST),
    TAG_NOT_ALLOWED(100, "The provided tag is not allowed", Response.Status.BAD_REQUEST),
    TAG_NOT_FOUND(101, "The provided tag is not attached", Response.Status.NOT_FOUND),
    FEATUREGROUP_NOT_ONLINE(102, "The feature group is not available online", Response.Status.BAD_REQUEST),
    FEATUREGROUP_ONDEMAND_NO_PARTS(103, "Partitions not available for on demand feature group",
        Response.Status.BAD_REQUEST),
    ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_ALGORITHM(104, "Illegal server encryption algorithm provided",
        Response.Status.BAD_REQUEST),
    ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_KEY(105, "Illegal server encryption key provided",
        Response.Status.BAD_REQUEST),
    TRAINING_DATASET_DUPLICATE_SPLIT_NAMES(106, "Duplicate split names in training dataset provided.",
        Response.Status.BAD_REQUEST),
    STATISTICS_READ_ERROR(107, "Error reading the statistics", Response.Status.INTERNAL_SERVER_ERROR),
    ILLEGAL_STATISTICS_CONFIG(108, "Illegal statistics config", Response.Status.BAD_REQUEST),
    ERROR_DELETING_STATISTICS(109, "Error deleting the statistics of a feature store entity",
        Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_GETTING_S3_CONNECTOR_ACCESS_AND_SECRET_KEY_FROM_SECRET(110, "Could not get access and secret " +
        "key from the user secret", Response.Status.INTERNAL_SERVER_ERROR),
    TRAINING_DATASET_NO_QUERY(111, "The training dataset wasn't generated from a query",
        Response.Status.BAD_REQUEST),
    TRAINING_DATASET_NO_SCHEMA(112, "No query or feature schema provided", Response.Status.BAD_REQUEST),
    QUERY_FAILED_FG_DELETED(113, "Cannot generate query, some feature groups were deleted",
        Response.Status.BAD_REQUEST),
    ILLEGAL_FEATUREGROUP_UPDATE(114, "Illegal feature group update", Response.Status.BAD_REQUEST),
    COULD_NOT_ALTER_FEAUTURE_GROUP_METADATA(115, "Failed to alter feature group meta data",
        Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_GET_FEATURE_GROUP_METADATA(116, "Failed to retrieve feature group meta data",
        Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_CREATING_HIVE_METASTORE_CLIENT(117, "Failed to open Hive Metastore client",
        Response.Status.INTERNAL_SERVER_ERROR),
    NO_DATA_AVAILABLE_FEATUREGROUP_COMMITDATE(118, "No data is available for feature group with this "
        +  "commit date", Response.Status.NOT_FOUND),
    PROVIDED_DATE_FORMAT_NOT_SUPPORTED(119, "Invalid date format", Response.Status.BAD_REQUEST),
    ONLINE_FEATURESTORE_JDBC_CONNECTOR_NOT_FOUND(120, "Online featurestore JDBC connector not found",
            Response.Status.INTERNAL_SERVER_ERROR),
    PRIMARY_KEY_REQUIRED(121, "Primary key is required when using Hudi time travel "
        + "format", Response.Status.BAD_REQUEST),
    DATABRICKS_INSTANCE_ALREADY_EXISTS(122, "Databricks Instance already registered", Response.Status.CONFLICT),
    DATABRICKS_INSTANCE_NOT_EXISTS(123, "Databricks Instance doesn't exists", Response.Status.NOT_FOUND),
    DATABRICKS_CANNOT_START_CLUSTER(124, "Could not start Databricks cluster",
        Response.Status.INTERNAL_SERVER_ERROR),
    DATABRICKS_ERROR(125, "Error communicating with Databricks", Response.Status.INTERNAL_SERVER_ERROR),
    STORAGE_CONNECTOR_GET_ERROR(126, "Error retrieving the storage connector",
        Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_ONLINE_FEATURES(127, "Error retrieving online features", Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_ONLINE_USERS(128, "Error getting database users", Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_ONLINE_GENERIC(129, "Error communicating with the online feature store",
        Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_CREATE_ON_DEMAND_FEATUREGROUP(130, "Could not create on demand feature group",
        Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_DELETE_ON_DEMAND_FEATUREGROUP(131, "Could not delete on demand feature group",
        Response.Status.INTERNAL_SERVER_ERROR),
    ILLEGAL_FEATURE_GROUP_FEATURE_DEFAULT_VALUE(132, "Illegal feature default value",
      Response.Status.BAD_REQUEST),
    KEYWORD_ERROR(133, "Keyword error for feature group/training dataset",
      Response.Status.INTERNAL_SERVER_ERROR),
    KEYWORD_FORMAT_ERROR(134, "Keyword format error", Response.Status.BAD_REQUEST),
    REDSHIFT_CONNECTOR_NOT_FOUND(135, "Redshift Connector not found", Response.Status.NOT_FOUND),
    ILLEGAL_STORAGE_CONNECTOR_ARG(136, "Illegal storage connector argument", Response.Status.BAD_REQUEST),
    ERROR_SAVING_STATISTICS(137, "Error saving statistics", Response.Status.BAD_REQUEST),
    FILTER_CONSTRUCTION_ERROR(138, "Failed to construct filter condition",
      Response.Status.INTERNAL_SERVER_ERROR),
    ILLEGAL_FILTER_ARGUMENTS(139, "Malformed filter conditions for Query", Response.Status.BAD_REQUEST),
    ILLEGAL_ON_DEMAND_DATA_FORMAT(140, "Illegal on-demand feature group data format",
        Response.Status.BAD_REQUEST),
    ERROR_JOB_SETUP(141, "Error setting up feature store job", Response.Status.INTERNAL_SERVER_ERROR),
    LABEL_NOT_FOUND(142, "Could not find label in training dataset schema", Response.Status.NOT_FOUND),
    DATA_VALIDATION_RESULTS_NOT_FOUND(143,
      "Could not find feature group validation results. " +
        "Make sure the results file was not manually removed from the dataset",
      Response.Status.NOT_FOUND),
    DATA_VALIDATION_NOT_FOUND(144, "Could not find feature group validation.", Response.Status.NOT_FOUND),
    FEATURE_STORE_EXPECTATION_NOT_FOUND(145, "Could not find feature store expectation.", Response.Status.NOT_FOUND),
    FEATURE_GROUP_EXPECTATION_NOT_FOUND(146, "Could not find feature group expectation.", Response.Status.NOT_FOUND),
    FEATURE_GROUP_EXPECTATION_FEATURE_NOT_FOUND(147,
                       "Could not find expectation feature(s) in feature group expectation.",
      Response.Status.NOT_FOUND),
    FEATURE_STORE_RULE_NOT_FOUND(148, "Could not find feature store data validation rule.",
      Response.Status.NOT_FOUND),
    FEATURE_GROUP_CHECKS_FAILED(149,
                       "Feature group validation checks did not pass, will not persist the data.",
                                Response.Status.EXPECTATION_FAILED),
    RULE_NOT_FOUND(150, "Rule with provided name was not found.", Response.Status.NOT_FOUND),
    AVRO_PRIMITIVE_TYPE_NOT_SUPPORTED(151, "Error converting Hive Type to Avro primitive type",
                                      Response.Status.BAD_REQUEST),
    AVRO_MAP_STRING_KEY(152, "Map types are only supported with STRING type keys", Response.Status.BAD_REQUEST),
    AVRO_MALFORMED_SCHEMA(153, "Error converting Hive schema to Avro", Response.Status.INTERNAL_SERVER_ERROR),
    FEATURE_GROUP_EXPECTATION_FEATURE_TYPE_INVALID(154,
            "Could not attach expectation because some feature types did not match rule types.",
            Response.Status.BAD_REQUEST),
    ALERT_NOT_FOUND(155, "Alert not found", Response.Status.NOT_FOUND),
    ALERT_ILLEGAL_ARGUMENT(156, "Alert missing argument.", Response.Status.BAD_REQUEST),
    ALERT_ALREADY_EXISTS(157, "Alert with the same status already exists.", Response.Status.BAD_REQUEST),
    ERROR_DELETING_TRANSFORMERFUNCTION(158, "Error deleting the transformer function of a feature store entity",
        Response.Status.INTERNAL_SERVER_ERROR),
    TRANSFORMATION_FUNCTION_ALREADY_EXISTS(159, "The provided transformation function name and version " +
        "already exists", Response.Status.BAD_REQUEST),
    TRANSFORMATION_FUNCTION_DOES_NOT_EXIST(160, "Transformation function does not exist", Response.Status.BAD_REQUEST),
    TRANSFORMATION_FUNCTION_READ_ERROR(161, "Error reading the transformation function",
        Response.Status.INTERNAL_SERVER_ERROR),
    TRANSFORMATION_FUNCTION_VERSION(162, "Illegal transformation function version",
                                     Response.Status.BAD_REQUEST),
    ILLEGAL_TRANSFORMATION_FUNCTION_OUTPUT_TYPE(163, "Illegal transformation function output type",
        Response.Status.BAD_REQUEST),
    FEATURE_WITH_TRANSFORMATION_NOT_FOUND(164, "Could not find feature in training dataset schema",
        Response.Status.NOT_FOUND),
    ILLEGAL_PREFIX_NAME(165, "Illegal feature name", Response.Status.BAD_REQUEST),
    ERROR_SAVING_CODE(166, "Error saving code", Response.Status.BAD_REQUEST),
    CODE_READ_ERROR(167, "Error reading the code", Response.Status.INTERNAL_SERVER_ERROR),
    CODE_NOT_FOUND(168, "Code not found", Response.Status.NOT_FOUND),
    FAILED_TO_CREATE_ROUTE(169, "Failed to create route.", Response.Status.BAD_REQUEST),
    FAILED_TO_DELETE_ROUTE(170, "Failed to delete route.", Response.Status.BAD_REQUEST),
    ILLEGAL_EVENT_TIME_FEATURE_TYPE(171, "Illegal event time feature type", Response.Status.BAD_REQUEST),
    EVENT_TIME_FEATURE_NOT_FOUND(172, "Event time feature not found", Response.Status.BAD_REQUEST),
    FEATURE_GROUP_MISSING_EVENT_TIME(173, "Feature group is not event time enabled", Response.Status.BAD_REQUEST),
    JOIN_OPERATOR_MISMATCH(174, "Join features and operator list have different sizes", Response.Status.BAD_REQUEST),
    VALIDATION_RULE_INCOMPLETE(175, "Rule is missing a required field.", Response.Status.BAD_REQUEST),
    COULD_NOT_CREATE_ONLINE_FEATUREGROUP(176, "Could not create online feature group",
      Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_GET_QUERY_FILTER(177, "Error getting query filter", Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_REGISTER_BUILTIN_TRANSFORMATION_FUNCTION(178, "This branch should not be reached. Please fix automatic " +
      "registering of the built-in transformation functions upon project creation",
      Response.Status.INTERNAL_SERVER_ERROR),
    FEATURE_VIEW_ALREADY_EXISTS(179, "The provided feature view name and version already exists",
        Response.Status.BAD_REQUEST),
    FEATURE_VIEW_CREATION_ERROR(180, "Cannot create feature view.", Response.Status.BAD_REQUEST),
    FEATURE_VIEW_NOT_FOUND(181, "Feature view wasn't found.", Response.Status.NOT_FOUND),
    KAFKA_STORAGE_CONNECTOR_STORE_NOT_EXISTING(182, "Provided certificate store location does not exist",
      Response.Status.BAD_REQUEST),
    VALIDATION_NOT_SUPPORTED(183, "Rule is not supported.", Response.Status.BAD_REQUEST),
    STREAM_FEATURE_GROUP_ONLINE_DISABLE_ENABLE(184,
      "Stream feature group cannot be online enabled if it was created as offline only.",
      Response.Status.BAD_REQUEST),
    GCS_FIELD_MISSING(185, "Field missing", Response.Status.BAD_REQUEST),
    NESTED_JOIN_NOT_ALLOWED(187, "Nested join is not supported.", Response.Status.BAD_REQUEST),
    FEATURE_NOT_FOUND(188, "Could not find feature.", Response.Status.NOT_FOUND),
    TRAINING_DATASET_COULD_NOT_BE_CREATED(186, "Could not create training dataset",
      Response.Status.INTERNAL_SERVER_ERROR),
    EXPECTATION_TYPE_NOT_FOUND(189, "Expectation type not supported.", Response.Status.NOT_FOUND),
    EXPECTATION_NOT_FOUND(190, "Expectation not found.", Response.Status.NOT_FOUND),
    NO_EXPECTATION_SUITE_ATTACHED_TO_THIS_FEATUREGROUP(191, "No Expectation Suite attached to this " +
      "feature group.", Response.Status.NOT_FOUND),
    VALIDATION_REPORT_NOT_FOUND(192, "Validation report not found.",
      Response.Status.NOT_FOUND),
    FAILED_TO_PARSE_EXPECTATION_CONFIG_TO_JSON(193, "Failed to parse expectation config field to json."
      + " Expectation config must be a valid json to fetch the expectationId from the meta field.",
      Response.Status.BAD_REQUEST),
    KEY_NOT_FOUND_OR_INVALID_VALUE_TYPE_IN_JSON_OBJECT(194, "Requested key has not been found in Json " +
      "object or associated value is not of required type.",
      Response.Status.BAD_REQUEST),
    FAILED_TO_PARSE_VALIDATION_RESULT_FOR_OBSERVED_VALUE(195, "Failed to parse result json "
      + "to get observed_value field", Response.Status.BAD_REQUEST),
    FAILED_TO_PARSE_EXPECTATION_META_FIELD(196, "Failed to parse expectation meta field.",
      Response.Status.BAD_REQUEST),
    VALIDATION_REPORT_IS_NOT_VALID_JSON(197, "Validation report is not a valid JSON.", Response.Status.BAD_REQUEST),
    ERROR_SAVING_ON_DISK_VALIDATION_REPORT(198, "Error saving full json report to disk.",
      Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_DELETING_ON_DISK_VALIDATION_REPORT(199, "Error deleting on-disk validation report.",
      Response.Status.INTERNAL_SERVER_ERROR),
    INPUT_FIELD_EXCEEDS_MAX_ALLOWED_CHARACTER(200, "Input field length exceeds max allowed characters.",
      Response.Status.BAD_REQUEST),
    INPUT_FIELD_IS_NOT_VALID_JSON(201, "Input field fail to be parsed to valid Json.",
      Response.Status.BAD_REQUEST),
    INPUT_FIELD_IS_NOT_NULLABLE(202, "Input field is not nullable.",
      Response.Status.BAD_REQUEST),
    ERROR_INFERRING_INGESTION_RESULT(203, "Could not infer ingestion result from validation ingestion policy and " +
      "validation success.", Response.Status.BAD_REQUEST),
    FAILED_TO_DELETE_TD_DATA(204, "Failed to delete training dataset.", Response.Status.BAD_REQUEST),
    ERROR_DELETING_FEATURE_VIEW(205, "Error deleting feature view.",
      Response.Status.INTERNAL_SERVER_ERROR),
    ILLEGAL_TRAINING_DATASET_TIME_SERIES_SPLIT(206, "Illegal training dataset time series split.",
        Response.Status.BAD_REQUEST),
    ILLEGAL_EXPECTATION_UPDATE(207, "Illegal Expectation update. To preserve the validation history this " + 
      "update is not allowed. Create a new Expectation by removing expectationId from meta field instead.", 
      Response.Status.BAD_REQUEST),
    EXPECTATION_SUITE_ALREADY_EXISTS(208, "An expectation suite is already attached to this " +
      "feature group. Either update the existing suite via the update endpoint or delete it first.",
      Response.Status.CONFLICT),
    FAILURE_HDFS_USER_OPERATION(209, "HDFS user operation failure",
      Response.Status.INTERNAL_SERVER_ERROR),
    FEATURE_NAME_NOT_FOUND(210, "The Feature Name was not found in this version of the Feature Group.",
      Response.Status.BAD_REQUEST),
    VALIDATION_RESULT_IS_NOT_VALID_JSON(211, "The validation result is not a valid json.", 
      Response.Status.BAD_REQUEST),
    FEATURE_OFFLINE_TYPE_NOT_PROVIDED(212, "Feature offline type cannot be null or empty.",
      Response.Status.BAD_REQUEST),
    AMBIGUOUS_FEATURE_ERROR(213, "Feature name is ambiguous.", Response.Status.BAD_REQUEST),
    STORAGE_CONNECTOR_TYPE_NOT_ENABLED(214, "Storage connector type not enabled", Response.Status.BAD_REQUEST),
    COULD_NOT_SHARE_PROJECT(215, "Could not share project", Response.Status.BAD_REQUEST),
    FILE_DELETION_ERROR(216, "Failed to delete file", Response.Status.BAD_REQUEST),
    FILE_READ_ERROR(217, "Failed to read file", Response.Status.BAD_REQUEST),
    DOCKER_FULLNAME_ERROR(218, "Failed to retrieve full docker image name", Response.Status.BAD_REQUEST),
    CONNECTION_CHECKER_LAUNCH_ERROR(219, "Failed to launch process to start docker container for testing " +
      "connection",
      Response.Status.BAD_REQUEST),
    CONNECTION_CHECKER_ERROR(220, "Failure in testing connection for storage connector",
      Response.Status.BAD_REQUEST),
    ERROR_CREATING_ONLINE_FEATURESTORE_KAFKA_OFFSET_TABLE(221, "An error occurred when trying to " +
            "create the kafka offset table for an online feature store", Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_CONSTRUCTING_VALIDATION_REPORT_DIRECTORY_PATH(222,
        "An error occurred while constructing validation report directory path",
        Response.Status.INTERNAL_SERVER_ERROR),
    SPINE_GROUP_ON_RIGHT_SIDE_OF_JOIN_NOT_ALLOWED(223, "Spine groups cannot be used on the right side" +
      "of a feature view join.", Response.Status.BAD_REQUEST),
    FEATURE_GROUP_DUPLICATE_FEATURE(224, "Feature list contains duplicate", Response.Status.BAD_REQUEST),
    HELPER_COL_NOT_FOUND(225, "Could not find helper column in feature view schema",
      Response.Status.NOT_FOUND),
    OPENSEARCH_DEFAULT_EMBEDDING_INDEX_SUFFIX_NOT_DEFINED(226, "Opensearch default embedding index not defined",
        Response.Status.INTERNAL_SERVER_ERROR),
    FEATURE_GROUP_COMMIT_NOT_FOUND(227, "Feature group commit not found", Response.Status.BAD_REQUEST),
    STATISTICS_NOT_FOUND(228, "Statistics wasn't found.", Response.Status.NOT_FOUND),
    INVALID_STATISTICS_WINDOW_TIMES(229, "Window times provided are invalid", Response.Status.BAD_REQUEST),
    COULD_NOT_DELETE_VECTOR_DB_INDEX(230, "Could not delete index from vector db.",
        Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_INITIATE_ARROW_FLIGHT_CONNECTION(231, "Could not initiate connection to Arrow Flight server",
        Response.Status.INTERNAL_SERVER_ERROR),
    ARROW_FLIGHT_READ_QUERY_ERROR(232, "Arrow Flight server Read Query failed",
        Response.Status.INTERNAL_SERVER_ERROR),
    FEATURE_MONITORING_ENTITY_NOT_FOUND(233, "Feature Monitoring entity not found.",
      Response.Status.NOT_FOUND),
    FEATURE_MONITORING_NOT_ENABLED(234, "Feature monitoring is not enabled.", Response.Status.BAD_REQUEST),
    FEATURE_NOT_FOUND_IN_VECTOR_DB(235, "Feature not found in vector db.",
        Response.Status.INTERNAL_SERVER_ERROR),
    COULD_NOT_PREVIEW_DATA_IN_VECTOR_DB(236, "Could not preview data in vector database.",
        Response.Status.INTERNAL_SERVER_ERROR),
    EMBEDDING_FEATURE_NOT_FOUND(237, "Embedding feature cannot be found in feature group.",
        Response.Status.BAD_REQUEST),
    COULD_NOT_GET_VECTOR_DB_INDEX(238, "Could not get index from vector db.",
        Response.Status.INTERNAL_SERVER_ERROR),
    EMBEDDING_INDEX_EXISTED(239, "Embedding index already exists.", Response.Status.BAD_REQUEST),
    INVALID_EMBEDDING_INDEX_NAME(240, "Embedding index name is not valid.", Response.Status.BAD_REQUEST),
    VECTOR_DATABASE_INDEX_MAPPING_LIMIT_EXCEEDED(241, "Index mapping limit exceeded.", Response.Status.BAD_REQUEST);

    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 270000;

    FeaturestoreErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }


  /**
   * TensorBoard specific error codes
   */
  public enum TensorBoardErrorCode implements RESTErrorCode {

    TENSORBOARD_CLEANUP_ERROR(1, "Error when deleting a running TensorBoard", Response.Status.INTERNAL_SERVER_ERROR),
    TENSORBOARD_START_ERROR(2, "Error to start TensorBoard", Response.Status.INTERNAL_SERVER_ERROR),
    TENSORBOARD_FETCH_ERROR(3, "Error while fetching TensorBoard information",
            Response.Status.INTERNAL_SERVER_ERROR),
    TENSORBOARD_NOT_FOUND(4, "Could not find TensorBoard",
        Response.Status.NOT_FOUND);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 280000;

    TensorBoardErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  /**
   * Airflow specific error codes
   */
  public enum AirflowErrorCode implements RESTErrorCode {

    JWT_NOT_CREATED(1, "JWT for Airflow service could not be created", Response.Status.INTERNAL_SERVER_ERROR),
    JWT_NOT_STORED(2, "JWT for Airflow service could not be stored", Response.Status.INTERNAL_SERVER_ERROR),
    AIRFLOW_DIRS_NOT_CREATED(3, "Airflow internal directories could not be created",
        Response.Status.INTERNAL_SERVER_ERROR),
    DAG_NOT_TEMPLATED(4, "Could not template DAG file", Response.Status.INTERNAL_SERVER_ERROR),
    AIRFLOW_MANAGER_UNINITIALIZED(5, "AirflowManager is not initialized",
        Response.Status.INTERNAL_SERVER_ERROR);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 290000;

    AirflowErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum PythonErrorCode implements RESTErrorCode {

    ANACONDA_ENVIRONMENT_NOT_FOUND(0, "No Anaconda environment created for this project.",
            Response.Status.NOT_FOUND),
    ANACONDA_ENVIRONMENT_ALREADY_INITIALIZED(1, "Anaconda environment already created for this project.",
            Response.Status.CONFLICT),
    PYTHON_SEARCH_TYPE_NOT_SUPPORTED(2, "The supplied search is not supported, only pip and conda are currently " +
            "supported.", Response.Status.BAD_REQUEST),
    PYTHON_LIBRARY_NOT_FOUND(3, "Library could not be found.", Response.Status.NOT_FOUND),
    YML_FILE_MISSING_PYTHON_VERSION(4, "No python binary version was found in the environment yaml file.",
            Response.Status.BAD_REQUEST),
    NOT_MATCHING_PYTHON_VERSIONS(5, "The supplied yaml files have mismatching python versions.",
            Response.Status.BAD_REQUEST),
    CONDA_INSTALL_REQUIRES_CHANNEL(6, "Conda package manager requires that a conda channel is selected in which the " +
            "library is located", Response.Status.BAD_REQUEST),
    INSTALL_TYPE_NOT_SUPPORTED(7, "The provided install type is not supported", Response.Status.BAD_REQUEST),
    CONDA_COMMAND_NOT_FOUND(8, "Command not found.", Response.Status.BAD_REQUEST),
    MACHINE_TYPE_NOT_SPECIFIED(9, "Machine type not specified.", Response.Status.BAD_REQUEST),
    VERSION_NOT_SPECIFIED(10, "Version not specified.", Response.Status.BAD_REQUEST),
    ANACONDA_ENVIRONMENT_INITIALIZING(11, "The project's Python environment is currently being initialized. Please " +
      "try again later.", Response.Status.BAD_REQUEST),
    ANACONDA_ENVIRONMENT_FILE_INVALID(12, "Path is not a valid environment file, " +
        "must be Anaconda .yml or requirements.txt", Response.Status.BAD_REQUEST),
    ANACONDA_PIP_CHECK_FAILED(13, "pip check command failed", Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_ENVIRONMENT_FAILED_INITIALIZATION(14, "The project's Python environment failed to initialize," +
        " please recreate the environment.", Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_ENVIRONMENT_REMOVAL_FAILED(15,
      "Deletion of the project's Python environment encountered an issue",
      Response.Status.INTERNAL_SERVER_ERROR),
    CONDA_COMMAND_DELETE_ERROR(16, "Failed to delete a command", Response.Status.BAD_REQUEST),
    CONDA_INSTALL_DISABLED(17, "Conda install option is disabled. Contact Admin user to enable it.",
      Response.Status.FORBIDDEN);

    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 300000;

    PythonErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum ResourceErrorCode implements RESTErrorCode {

    INVALID_QUERY_PARAMETER(0, "Invalid query.", Response.Status.NOT_FOUND);

    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 310000;

    ResourceErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum ApiKeyErrorCode implements RESTErrorCode {

    KEY_NOT_CREATED(1, "Api key could not be created", Response.Status.BAD_REQUEST),
    KEY_NOT_FOUND(2, "Api key not found", Response.Status.UNAUTHORIZED),
    KEY_ROLE_CONTROL_EXCEPTION(3, "No valid role found for this invocation", Response.Status.FORBIDDEN),
    KEY_SCOPE_CONTROL_EXCEPTION(4, "No valid scope found for this invocation", Response.Status.FORBIDDEN),
    KEY_SCOPE_NOT_SPECIFIED(5, "Api key scope can not be empty", Response.Status.BAD_REQUEST),
    KEY_SCOPE_EMPTY(6, "Api key scope can not be empty", Response.Status.BAD_REQUEST),
    KEY_NAME_EXIST(7, "Api key name already exists", Response.Status.BAD_REQUEST),
    KEY_NAME_NOT_SPECIFIED(8, "Api key name not specified", Response.Status.BAD_REQUEST),
    KEY_NAME_NOT_VALID(9, "Api key name not valid", Response.Status.BAD_REQUEST),
    KEY_HANDLER_CREATE_ERROR(10, "Error occurred during apikey create handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    KEY_HANDLER_DELETE_ERROR(11, "Error occurred during apikey delete handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    KEY_INVALID(12, "Invalid or incorrect API key.", Response.Status.UNAUTHORIZED),
    KEY_NOT_FOUND_IN_DATABASE(13, "API key not found in the database", Response.Status.UNAUTHORIZED);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 320000;

    ApiKeyErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public int getRange() {
      return range;
    }
  }
  
  public enum OpenSearchErrorCode implements RESTErrorCode {
    
    SIGNING_KEY_ERROR(0, "Couldn't get or create the elk signing key",
        Response.Status.INTERNAL_SERVER_ERROR),
    JWT_NOT_CREATED(1, "Jwt for elk couldn't be created",
        Response.Status.INTERNAL_SERVER_ERROR),
    KIBANA_REQ_ERROR(2, "Error while executing Kibana request",
        Response.Status.BAD_REQUEST),
    OPENSEARCH_CONNECTION_ERROR(3, "Couldn't connect to OpenSearch",
        Response.Status.SERVICE_UNAVAILABLE),
    OPENSEARCH_INTERNAL_REQ_ERROR(4, "Error while executing OpenSearch request",
        Response.Status.INTERNAL_SERVER_ERROR),
    OPENSEARCH_QUERY_ERROR(5, "Error while executing a user query on " +
        "OpenSearch", Response.Status.BAD_REQUEST),
    INVALID_OPENSEARCH_ROLE(6, "Invalid OpenSearch security role",
        Response.Status.INTERNAL_SERVER_ERROR),
    INVALID_OPENSEARCH_ROLE_USER(7, "Invalid OpenSearch security role for a user",
        Response.Status.UNAUTHORIZED),
    OPENSEARCH_QUERY_NO_MAPPING(8, "OpenSearch query uses a field that is not in the mapping of the index",
      Response.Status.BAD_REQUEST),
    OPENSEARCH_INDEX_NOT_FOUND(9, "OpenSearch index not found", Response.Status.NOT_FOUND);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private static final int range = 330000;
  
    OpenSearchErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }
    
    @Override
    public Response.StatusType getRespStatus() {
      return respStatus;
    }
    
    @Override
    public Integer getCode() {
      return code;
    }
    
    @Override
    public String getMessage() {
      return message;
    }
    
    @Override
    public int getRange() {
      return range;
    }
  }
  
  /**
   * Error codes for the provenance microservice on Hopsworks
   */
  public enum ProvenanceErrorCode implements RESTErrorCode {
    MALFORMED_ENTRY(1, "Provenance entry is malformed",
      Response.Status.INTERNAL_SERVER_ERROR),
    BAD_REQUEST(2, "Provenance query request is malformed",
      Response.Status.INTERNAL_SERVER_ERROR),
    UNSUPPORTED(3, "Provenance query is not supported",
      Response.Status.BAD_REQUEST),
    INTERNAL_ERROR(4, "Provenance logical error",
      Response.Status.INTERNAL_SERVER_ERROR),
    ARCHIVAL_STORE(5, "Provenance archival store error",
      Response.Status.INTERNAL_SERVER_ERROR),
    FS_ERROR(6, "Provenance xattr - file system error",
      Response.Status.INTERNAL_SERVER_ERROR);

    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 340000;

    ProvenanceErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum ExperimentsErrorCode implements RESTErrorCode {

    EXPERIMENT_NOT_FOUND(0, "No experiment found for provided id.",
        Response.Status.NOT_FOUND),
    RESULTS_NOT_FOUND(1, "No results found for provided id.",
        Response.Status.NOT_FOUND),
    RESULTS_RETRIEVAL_ERROR(2, "Error occurred when retrieving experiment results.",
        Response.Status.INTERNAL_SERVER_ERROR),
    EXPERIMENT_EXECUTABLE_NOT_FOUND(3, "Could not find experiment executable.",
        Response.Status.BAD_REQUEST),
    EXPERIMENT_EXECUTABLE_COPY_FAILED(4, "Error occurred when copying experiment executable.",
        Response.Status.INTERNAL_SERVER_ERROR),
    EXPERIMENT_LIST_FAILED(5, "Error occurred when fetching experiments.",
        Response.Status.INTERNAL_SERVER_ERROR),
    EXPERIMENT_MARSHALLING_FAILED(6,
        "Error occurred during marshalling/unmarshalling of experiment json.",
        Response.Status.INTERNAL_SERVER_ERROR);

    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 350000;


    ExperimentsErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum ModelRegistryErrorCode implements RESTErrorCode {

    MODEL_NOT_FOUND(0, "No model found for provided name and version.",
        Response.Status.NOT_FOUND),
    KEY_NOT_STRING(1, "metrics key is not a string.",
        Response.Status.NOT_FOUND),
    METRIC_NOT_NUMBER(2, "Could not cast provided metric to double.",
        Response.Status.BAD_REQUEST),
    MODEL_LIST_FAILED(3, "Error occurred when fetching models.",
        Response.Status.INTERNAL_SERVER_ERROR),
    MODEL_MARSHALLING_FAILED(4,
        "Error occurred during marshalling/unmarshalling of model json.",
        Response.Status.INTERNAL_SERVER_ERROR),
    MODEL_REGISTRY_ID_NOT_PROVIDED(5, "Model Registry Id was not provided.",
            Response.Status.BAD_REQUEST),
    MODEL_REGISTRY_ID_NOT_FOUND(6, "Model Registry Id was not found.",
            Response.Status.BAD_REQUEST),
    MODEL_REGISTRY_ACCESS_DENIED(7, "Model Registry not accessible.",
            Response.Status.FORBIDDEN),
    MODEL_REGISTRY_MODELS_DATASET_NOT_FOUND(8, "Models dataset does not exist in project.",
            Response.Status.INTERNAL_SERVER_ERROR),
    MODEL_CANNOT_BE_DELETED(9, "Could not delete the model", Response.Status.BAD_REQUEST);

    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 360000;

    ModelRegistryErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }
  
  public enum SchematizedTagErrorCode implements RESTErrorCode {
    TAG_SCHEMA_NOT_FOUND(0, "No schema found for provided name", Response.Status.NOT_FOUND),
    INVALID_TAG_SCHEMA(1, "Invalid tag schema.", Response.Status.BAD_REQUEST),
    TAG_NOT_FOUND(2, "No tag found for provided name.", Response.Status.NOT_FOUND),
    TAG_ALREADY_EXISTS(3, "Tag with the same name already exists.", Response.Status.CONFLICT),
    INVALID_TAG_NAME(4, "Invalid tag name.", Response.Status.BAD_REQUEST),
    INVALID_TAG_VALUE(5, "Invalid tag value.", Response.Status.BAD_REQUEST),
    TAG_NOT_ALLOWED(6, "The provided tag is not allowed", Response.Status.BAD_REQUEST),
    INTERNAL_PROCESSING_ERROR(7, "Internal error while processing tag", Response.Status.INTERNAL_SERVER_ERROR);
    
    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 370000;
    
    SchematizedTagErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }
    
    @Override
    public Integer getCode() {
      return code;
    }
    
    @Override
    public String getMessage() {
      return message;
    }
    
    public Response.StatusType getRespStatus() {
      return respStatus;
    }
    
    @Override
    public int getRange() {
      return range;
    }
  }
  
  public enum CloudErrorCode implements RESTErrorCode {
    CLOUD_FEATURE(0, "This method is only available in cloud deployments.", Response.Status.METHOD_NOT_ALLOWED),
    FAILED_TO_ASSUME_ROLE(1, "Failed to assume role.", Response.Status.BAD_REQUEST),
    ACCESS_CONTROL_EXCEPTION(2, "You are not allowed to assume this role.", Response.Status.FORBIDDEN),
    MAPPING_NOT_FOUND(3, "Mapping not found.", Response.Status.BAD_REQUEST),
    MAPPING_ALREADY_EXISTS(4, "Mapping for the given project and role already exists.", Response.Status.BAD_REQUEST),
    FAILED_TO_GET_CLUSTER_CRED(5, "Failed to get cluster credential.", Response.Status.BAD_REQUEST);
  
    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 380000;
  
    CloudErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }
  
    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }

  public enum AlertErrorCode implements RESTErrorCode {
    ALERT_CREATION_FAILED(0, "Failed to creat alert", Response.Status.BAD_REQUEST),
    RECEIVER_EXIST(1, "A receiver already exists.", Response.Status.BAD_REQUEST),
    ROUTE_EXIST(2, "A route already exists.", Response.Status.BAD_REQUEST),
    RECEIVER_NOT_FOUND(3, "Receiver not found.", Response.Status.BAD_REQUEST),
    ROUTE_NOT_FOUND(4, "Route not found.", Response.Status.BAD_REQUEST),
    SILENCE_NOT_FOUND(5, "Silence not found.", Response.Status.BAD_REQUEST),
    ILLEGAL_ARGUMENT(6, "Illegal argument.", Response.Status.BAD_REQUEST),
    FAILED_TO_CONNECT(7, "Failed to connect to alert manager.", Response.Status.PRECONDITION_FAILED),
    FAILED_TO_UPDATE_AM_CONFIG(8, "Failed to update alert manager configuration.",
        Response.Status.PRECONDITION_FAILED),
    RESPONSE_ERROR(9, "Alert manager response error.", Response.Status.BAD_REQUEST),
    ACCESS_CONTROL_EXCEPTION(10, "You are not allowed to access this resource.", Response.Status.FORBIDDEN),
    FAILED_TO_READ_CONFIGURATION(11, "Failed to read alert manager configuration.",
        Response.Status.PRECONDITION_FAILED),
    FAILED_TO_CLEAN(12, "Failed to clean project from alert manager config.",
        Response.Status.PRECONDITION_FAILED);

    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 390000;

    AlertErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }
  
  public enum RemoteAuthErrorCode implements RESTErrorCode {
    NOT_FOUND(0, "Not found.", Response.Status.NOT_FOUND),
    DUPLICATE_ENTRY(1, "Duplicate entry.", Response.Status.BAD_REQUEST),
    ILLEGAL_ARGUMENT(2, "Illegal argument.", Response.Status.BAD_REQUEST),
    WRONG_CONFIG(3, "Wrong configuration.", Response.Status.PRECONDITION_FAILED),
    TOKEN_PARSE_EXCEPTION(4, "Token ParseException.", Response.Status.EXPECTATION_FAILED),
    NOT_ALLOWED(5, "Operation not allowed.", Response.Status.BAD_REQUEST);
    
    private int code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 400000;
  
    RemoteAuthErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }
    
    @Override
    public Integer getCode() {
      return code;
    }
    
    @Override
    public String getMessage() {
      return message;
    }
    
    public Response.StatusType getRespStatus() {
      return respStatus;
    }
    
    @Override
    public int getRange() {
      return range;
    }
  }
  
  public enum CommandErrorCode implements RESTErrorCode {
    INTERNAL_SERVER_ERROR(0, "Something went wrong executing command",
      Response.Status.INTERNAL_SERVER_ERROR),
    INVALID_SQL_QUERY(1, "Invalid sql query for command", Response.Status.BAD_REQUEST),
    ILLEGAL_ARGUMENT(2, "Illegal argument in command", Response.Status.BAD_REQUEST),
    FILESYSTEM_ACCESS_ERROR(3, "Error accessing files system for components",
      Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_ACCESS_ERROR(4, "Error accessing the featurestore",
      Response.Status.INTERNAL_SERVER_ERROR),
    OPENSEARCH_ACCESS_ERROR(5, "Error acccessing OpenSearch", Response.Status.INTERNAL_SERVER_ERROR),
    SERIALIZATION_ERROR(6, "Error serializing content", Response.Status.INTERNAL_SERVER_ERROR),
    NOT_IMPLEMENTED(7, "Internal error dealing with new artifact type",
      Response.Status.NOT_IMPLEMENTED),
    DB_QUERY_ERROR(8, "DB error on query", Response.Status.INTERNAL_SERVER_ERROR),
    ARTIFACT_DELETED(9, "Artifact was deleted before command could be executed",
      Response.Status.INTERNAL_SERVER_ERROR);
    
    private final int code;
    private final String message;
    private final Response.Status respStatus;
    private static final int range = 410000;
  
    CommandErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }
    
    @Override
    public Integer getCode() {
      return code;
    }
    
    @Override
    public String getMessage() {
      return message;
    }
    
    public Response.StatusType getRespStatus() {
      return respStatus;
    }
    
    @Override
    public int getRange() {
      return range;
    }
  }

  public enum GitOpErrorCode implements RESTErrorCode {
    SIGNING_KEY_ERROR(0, "Couldn't get or create the GIT signing key.",
        Response.Status.INTERNAL_SERVER_ERROR),
    JWT_NOT_CREATED(1, "Jwt for GIT could not be created.", Response.Status.INTERNAL_SERVER_ERROR),
    INVALID_GIT_ROLE_USER(3, "Invalid git security role for a user.", Response.Status.UNAUTHORIZED),
    JWT_MATERIALIZATION_ERROR(4, "Could  not materialize jwt.", Response.Status.INTERNAL_SERVER_ERROR),
    GIT_PATHS_CREATION_ERROR(5, "Could not create git paths.", Response.Status.INTERNAL_SERVER_ERROR),
    REPOSITORY_URL_NOT_PROVIDED(6, "Repository url not provided.", Response.Status.BAD_REQUEST),
    DIRECTORY_PATH_NOT_PROVIDED(7, "Path to directory not provided.", Response.Status.BAD_REQUEST),
    DIRECTORY_PATH_DOES_NOT_EXIST(8, "The directory does not exist.", Response.Status.BAD_REQUEST),
    PATH_IS_NOT_DIRECTORY(9, "Path is not a directory.", Response.Status.BAD_REQUEST),
    GIT_HOME_ERROR(10, "Could not resolve GIT_HOME using DB.", Response.Status.INTERNAL_SERVER_ERROR),
    GIT_CONTAINER_LAUNCH_ERROR(12, "Could not launch the git container.",
        Response.Status.INTERNAL_SERVER_ERROR),
    EXECUTION_OBJECT_NOT_FOUND(13, "Execution object with the id not found.", Response.Status.NOT_FOUND),
    INVALID_AUTHENTICATION_METHOD(14, "Unknown authentication method.", Response.Status.BAD_REQUEST),
    INVALID_GITHUB_USERNAME(15, "Invalid git username.", Response.Status.BAD_REQUEST),
    USER_DOES_NOT_HAVE_PERMISSIONS_TO_GIT_DIR(16, "Git directory security error.",
        Response.Status.BAD_REQUEST),
    COMMIT_MESSAGE_IS_EMPTY(17, "Commit command message should not be empty.",
        Response.Status.BAD_REQUEST),
    INVALID_BRANCH_NAME(18, "Branch name should not be empty.", Response.Status.BAD_REQUEST),
    REPOSITORY_NOT_FOUND(19, "Repository not found.", Response.Status.NOT_FOUND),
    GIT_PROVIDER_NOT_PROVIDED(20, "Git provider not provided.", Response.Status.BAD_REQUEST),
    INVALID_REPOSITORY_URL(21, "Invalid repository url provided", Response.Status.BAD_REQUEST),
    DIRECTORY_IS_ALREADY_GIT_REPO(22, "Directory is already a git repository",
        Response.Status.BAD_REQUEST),
    INVALID_BRANCH_ACTION(23, "Invalid branch action provided.", Response.Status.BAD_REQUEST),
    INVALID_REMOTES_ACTION(24, "Invalid remotes action provided", Response.Status.BAD_REQUEST),
    INVALID_REMOTE_NAME(25, "Invalid remote name provided. Remote name should not be empty.",
        Response.Status.BAD_REQUEST),
    INVALID_REMOTE_URL_PROVIDED(26, "Invalid remote url provided. Remote url should not be empty.",
        Response.Status.BAD_REQUEST),
    GIT_OPERATION_ERROR(27, "Git operation error.", Response.Status.INTERNAL_SERVER_ERROR),
    GIT_REPOSITORIES_NOT_FOUND(28, "No git repository found in project", Response.Status.NOT_FOUND),
    GIT_USERNAME_AND_PASSWORD_NOT_SET(29, "Git username and password not set",
        Response.Status.BAD_REQUEST),
    COMMIT_FILES_EMPTY(30, "Files to add and commit is empty.", Response.Status.BAD_REQUEST),
    INVALID_REPOSITORY_ACTION(31, "Invalid repository action.", Response.Status.BAD_REQUEST),
    REMOTE_NOT_FOUND(32, "Git remote not found.", Response.Status.NOT_FOUND),
    INVALID_BRANCH_AND_COMMIT_CHECKOUT_COMBINATION(33, "Branch and Hash are mutually exclusive.",
        Response.Status.BAD_REQUEST),
    INVALID_GIT_COMMAND_CONFIGURATION(34, "Invalid git command operation",
        Response.Status.INTERNAL_SERVER_ERROR),
    USER_IS_NOT_REPOSITORY_OWNER(35, "User not allowed to perform operation in repository",
            Response.Status.FORBIDDEN),
    READ_ONLY_REPOSITORY(36, "Repository is read only", Response.Status.BAD_REQUEST),
    ERROR_VALIDATING_REPOSITORY_PATH(37, "Error validating git repository path",
        Response.Status.INTERNAL_SERVER_ERROR),
    ERROR_CANCELLING_GIT_EXECUTION(38, "Failed to cancel git execution", Response.Status.BAD_REQUEST);
    private Integer code;
    private String message;
    private Response.Status respStatus;
    private static final int range = 500000;

    GitOpErrorCode(Integer code, String message, Response.Status respStatus) {
      this.code = range + code;
      this.message = message;
      this.respStatus = respStatus;
    }

    @Override
    public Integer getCode() {
      return code;
    }

    @Override
    public String getMessage() {
      return message;
    }

    public Response.StatusType getRespStatus() {
      return respStatus;
    }

    @Override
    public int getRange() {
      return range;
    }
  }
}
