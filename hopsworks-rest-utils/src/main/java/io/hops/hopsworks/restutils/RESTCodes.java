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
        + " Contact an administrator to increase your limit.", Response.Status.BAD_REQUEST),
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
    PROJECT_RETENTON_CHANGED(32, "Project retention period changed.", Response.Status.OK),

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
    TENSORBOARD_ELASTIC_INDEX_NOT_FOUND(67, "Could not find elastic index for TensorBoard.",
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
    PROJECT_NAME_TOO_LONG(75, "Project name is too long - cannot be longer than 29 characters.",
        Response.Status.BAD_REQUEST);



    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 150000;

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
    DATASET_ALREADY_PUBLIC(13, "Dataset is already in project.", Response.Status.CONFLICT),
    DATASET_ALREADY_IN_PROJECT(14, "Dataset is already public.", Response.Status.BAD_REQUEST),
    DATASET_NOT_PUBLIC(15, "DataSet is not public.", Response.Status.BAD_REQUEST),
    DATASET_NOT_EDITABLE(16, "DataSet is not editable.", Response.Status.BAD_REQUEST),
    DATASET_PENDING(17, "DataSet is not yet accessible. Accept the share request to access it.",
        Response.Status.BAD_REQUEST),
    PATH_NOT_FOUND(18, "Path not found", Response.Status.BAD_REQUEST),
    PATH_NOT_DIRECTORY(19, "Requested path is not a directory", Response.Status.BAD_REQUEST),
    PATH_IS_DIRECTORY(20, "Requested path is a directory", Response.Status.BAD_REQUEST),
    DOWNLOAD_ERROR(21, "You cannot download from a non public shared dataset",
        Response.Status.BAD_REQUEST),
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
    PATH_ENCODING_NOT_SUPPORTED(51, "Unsupported encoding.", Response.Status.BAD_REQUEST);


    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 110000;


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
    METADATA_MAX_SIZE_EXCEEDED(5, "Metadata is too long. The maximum " +
        "size for metadata and name is 13500 and 255 characters.",
        Response.Status.BAD_REQUEST),
    METADATA_MISSING_FIELD(6, "Metadata missing field. Required fields are " +
        "path, name, and metadata.",
        Response.Status.BAD_REQUEST),
    METADATA_ERROR(7, "Error while processing the extended metadata.",
        Response.Status.INTERNAL_SERVER_ERROR);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 180000;

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
    JOB_START_FAILED(0, "An error occurred while trying to start this job.", Response.Status.BAD_REQUEST),
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

    ELASTIC_INDEX_NOT_FOUND(14, "Elasticsearch indices do not exist", Response.Status.BAD_REQUEST),
    ELASTIC_TYPE_NOT_FOUND(15, "Elasticsearch type does not exist", Response.Status.BAD_REQUEST),
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
    APPID_NOT_FOUND(27, "AppId not found.", Response.Status.NOT_FOUND);
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 130000;


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
    private final int range = 140000;

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
    ELASTIC_SERVER_NOT_AVAILABLE(2, "The Elasticsearch Server is either down or misconfigured.",
        Response.Status.BAD_REQUEST),
    ELASTIC_SERVER_NOT_FOUND(3, "Problem when reaching the Elasticsearch server",
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
    ELASTIC_INDEX_CREATION_ERROR(12, "Error while creating index in elastic",
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
    INVALID_YML_SIZE(19, ".yml file too large. Maximum size is 10000 bytes",
      Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_FROM_YML_ERROR(20, "Failed to create Anaconda environment from .yml file.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PYTHON_INVALID_VERSION(21, "Invalid version of python (valid: '2.7', and '3.6'",
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
    ELASTIC_INDEX_NOT_FOUND(31, "Elastic index was not found in elasticsearch",
        Response.Status.NOT_FOUND),
    ELASTIC_INDEX_TYPE_NOT_FOUND(32, "Elastic index type was not found in elasticsearch",
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
    GIT_COMMAND_FAILURE(46, "Git command failed to execute", Response.Status.BAD_REQUEST);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 100000;

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
    SCHEMA_NOT_FOUND(6, "Topic has not schema attached to it.", Response.Status.NOT_FOUND),
    KAFKA_GENERIC_ERROR(7, "An error occurred while retrieving information from Kafka service",
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
    CREATE_SCHEMA_RESERVED_NAME(16, "The provided schema name is reserved",
      Response.Status.METHOD_NOT_ALLOWED),
    DELETE_RESERVED_SCHEMA(17, "The schema is reserved and cannot be deleted",
      Response.Status.METHOD_NOT_ALLOWED),
    SCHEMA_VERSION_NOT_FOUND(18, "Specified version of the schema not found", Response.Status.NOT_FOUND),
    PROJECT_IS_NOT_THE_OWNER_OF_THE_TOPIC(19, "Specified project is not the owner of the topic",
      Response.Status.BAD_REQUEST),
    ACL_FOR_ANY_USER(20, "Cannot create an ACL for user with email '*'", Response.Status.BAD_REQUEST);


    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 190000;


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
        Response.Status.SERVICE_UNAVAILABLE);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 120000;

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
    CERTIFICATE_SIGN_USER_ERR(19, "Error signing the certificate", Response.Status.BAD_REQUEST);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 200000;


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

    SEC_Q_EMPTY(10, "Security Question cannot be empty.", Response.Status.BAD_REQUEST),
    SEC_A_EMPTY(11, "Security Answer cannot be empty.", Response.Status.BAD_REQUEST),
    SEC_Q_NOT_IN_LIST(12, "Choose a Security Question from the list.", Response.Status.BAD_REQUEST),
    SEC_QA_INCORRECT(13, "Security question or answer did not match", Response.Status.BAD_REQUEST),
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
    SEC_QA_CHANGED(27, "Your have successfully changed your security questions and answer.",
        Response.Status.BAD_REQUEST),
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
    ACCOUNT_NOT_ACTIVE(51, "This account is not active", Response.Status.BAD_REQUEST);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 160000;


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

  public enum DelaErrorCode implements RESTErrorCode {

    //response for validation error
    THIRD_PARTY_ERROR(0, "Generic third party error.", Response.Status.INTERNAL_SERVER_ERROR),
    CLUSTER_NOT_REGISTERED(1, "Cluster not registered.", Response.Status.UNAUTHORIZED),
    HEAVY_PING(2, "Heavy ping required.", Response.Status.BAD_REQUEST),
    USER_NOT_REGISTERED(3, "User not registered.", Response.Status.UNAUTHORIZED),
    DATASET_EXISTS(4, "Dataset exists.", Response.Status.CONFLICT),
    DATASET_DOES_NOT_EXIST(5, "Dataset does not exist.", Response.Status.NOT_FOUND),
    DATASET_PUBLISH_PERMISSION_ERROR(6, "Dataset shared - only owner can publish.",
        Response.Status.FORBIDDEN),
    ILLEGAL_ARGUMENT(7, "Illegal Argument.", Response.Status.BAD_REQUEST),
    README_RETRIEVAL_FAILED(8, "Readme retrieval failed.", Response.Status.EXPECTATION_FAILED),
    README_ACCESS_PROBLEM(9, "Readme access problem.", Response.Status.BAD_REQUEST),
    COMMUNICATION_FAILURE(10, "Communication failure.", Response.Status.EXPECTATION_FAILED),
    DATASET_EMPTY(11, "Dataset empty.", Response.Status.BAD_REQUEST),
    SUBDIRS_NOT_SUPPORTED(12, "Subdirectories not supported.", Response.Status.BAD_REQUEST),
    ACCESS_ERROR(13, "Access error.", Response.Status.BAD_REQUEST),
    MISCONFIGURED(14, "misconfigured", Response.Status.EXPECTATION_FAILED),
    USER_NOT_FOUND(15, "user not found.", Response.Status.FORBIDDEN),
    DATASET_DELETE_ERROR(16, "Delete dataset error.", Response.Status.EXPECTATION_FAILED),
    DELA_NOT_AVAILABLE(17, "Dela not available.", Response.Status.BAD_REQUEST),
    HOPSSITE_NOT_AVAILABLE(18, "hopssite not available", Response.Status.BAD_REQUEST),
    REMOTE_DELA_NOT_AVAILABLE(19, "Remote dela not available.", Response.Status.BAD_REQUEST),
    DELA_TRANSFER_NOT_AVAILABLE(20, "Dela transfer not available.", Response.Status.BAD_REQUEST),
    MANIFEST_ENCODING_ERROR(21, "Manifest cannot be read as UTF", Response.Status.BAD_REQUEST),
    DATASET_NOT_PUBLIC(22, "dataset not public - no manifest", Response.Status.BAD_REQUEST),
    INODE_NOT_FOUND(23, "Inode not found.", Response.Status.BAD_REQUEST);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    public final int range = 170000;


    DelaErrorCode(Integer code, String message, Response.StatusType respStatus) {
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

    BADSIGNREQUEST(0, "No CSR provided", Response.Status.BAD_REQUEST),
    BADREVOKATIONREQUEST(1, "No certificate identifier provided", Response.Status.BAD_REQUEST),
    CERTNOTFOUND(2, "Certificate file not found", Response.Status.NO_CONTENT),
    CERTEXISTS(3, "Certificate with the same identifier already exists", Response.Status.BAD_REQUEST);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 220000;

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
    public final int range = 230000;


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

    INSTANCENOTFOUND(0, "Serving instance not found", Response.Status.NOT_FOUND),
    DELETIONERROR(1, "Serving instance could not be deleted",
        Response.Status.INTERNAL_SERVER_ERROR),
    UPDATEERROR(2, "Serving instance could not be updated", Response.Status.INTERNAL_SERVER_ERROR),
    LIFECYCLEERROR(3, "Serving instance could not be started/stopped", Response.Status.BAD_REQUEST),
    LIFECYCLEERRORINT(4, "Serving instance could not be started/stopped",
        Response.Status.INTERNAL_SERVER_ERROR),
    STATUSERROR(5, "Error getting TFServing instance status", Response.Status.INTERNAL_SERVER_ERROR),
    PATHNOTFOUND(6, "Model Path not found", Response.Status.BAD_REQUEST),
    COMMANDNOTRECOGNIZED(7, "Command not recognized", Response.Status.BAD_REQUEST),
    COMMANDNOTPROVIDED(8, "Command not provided", Response.Status.BAD_REQUEST),
    SPECNOTPROVIDED(9, "TFServing spec not provided", Response.Status.BAD_REQUEST),
    BAD_TOPIC(10, "Topic provided cannot be used for Serving logging", Response.Status.BAD_REQUEST),
    DUPLICATEDENTRY(11, "An entry with the same name already exists in this project",
      Response.Status.BAD_REQUEST),
    PYTHON_ENVIRONMENT_NOT_ENABLED(12, "Python environment has not been enabled in this project, " +
      "which is required for serving SkLearn Models", Response.Status.BAD_REQUEST),
    UPDATE_SERVING_TYPE_ERROR(13, "The serving type of a serving cannot be updated.",
      Response.Status.BAD_REQUEST);


    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    public final int range = 240000;


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
    SERVING_INSTANCE_BAD_REQUEST(8, "Serving instance bad request error", Response.Status.BAD_REQUEST);

    private int code;
    private String message;
    private Response.Status respStatus;
    public final int range = 250000;

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
    ACTIVITY_NOT_FOUND(1, "Activity instance not found", Response.Status.NOT_FOUND);

    private int code;
    private String message;
    private Response.Status respStatus;
    public final int range = 260000;

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
    FEATURESTORE_NOT_FOUND(8, "Featurestore wasn't found.",
        Response.Status.BAD_REQUEST),
    FEATUREGROUP_NOT_FOUND(9, "Featuregroup wasn't found.",
        Response.Status.BAD_REQUEST),
    COULD_NOT_FETCH_FEATUREGROUP_SHOW_CREATE_SCHEMA
        (10, "The query SHOW CREATE SCHEMA for the featuregroup in Hive failed.",
            Response.Status.BAD_REQUEST),
    TRYING_TO_UNSHARE_A_FEATURESTORE_THAT_IS_NOT_SHARED
        (11, "Trying to un-share a featurestore that is not shared",
            Response.Status.BAD_REQUEST),
    TRAINING_DATASET_NOT_FOUND(12, "Training dataset wasn't found.",
        Response.Status.BAD_REQUEST),
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
    COULD_NOT_INITIATE_HIVE_CONNECTION(19, "Could not initiate connecton to Hive Server",
        Response.Status.INTERNAL_SERVER_ERROR),
    HIVE_UPDATE_STATEMENT_ERROR(20, "Hive Update Statement failed",
        Response.Status.INTERNAL_SERVER_ERROR),
    HIVE_READ_QUERY_ERROR(21, "Hive Read Query failed",
        Response.Status.INTERNAL_SERVER_ERROR),
    CORRELATION_MATRIX_EXCEED_MAX_SIZE(22,
      "The size of the provided correlation matrix exceed the maximum size of 50 columns", Response.Status.BAD_REQUEST),
    FEATURESTORE_NAME_NOT_PROVIDED(23, "Featurestore name was not provided", Response.Status.BAD_REQUEST),
    UNAUTHORIZED_FEATURESTORE_OPERATION(24, "Only data owners are allowed to delete or update feature groups/" +
      "training datasets that are not created by themself.", Response.Status.UNAUTHORIZED),
    STORAGE_CONNECTOR_ID_NOT_PROVIDED(25, "Storage backend id not provided", Response.Status.BAD_REQUEST),
    CANNOT_FETCH_HIVE_SCHEMA_FOR_ON_DEMAND_FEATUREGROUPS(26, "Fetching Hive Schema of On-demand feature groups is not" +
      " supported", Response.Status.BAD_REQUEST),
    ON_DEMAND_FEATUREGROUP_JDBC_CONNECTOR_NOT_FOUND(27, "The JDBC Connector for the on-demand feature group could not" +
      " be found", Response.Status.BAD_REQUEST),
    PREVIEW_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS(28, "Fetching Hive Schema of On-demand feature groups is not" +
      " supported", Response.Status.BAD_REQUEST),
    CLEAR_OPERATION_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS(29, "Clearing Feature Group contents is not supported " +
      "for on-demand feature groups", Response.Status.BAD_REQUEST),
    ILLEGAL_STORAGE_CONNECTOR_NAME(30, "Illegal storage connector name", Response.Status.BAD_REQUEST),
    ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION(31, "Illegal storage connector description",
      Response.Status.BAD_REQUEST),
    ILLEGAL_JDBC_CONNECTION_STRING(32, "Illegal JDBC Connection String",
      Response.Status.BAD_REQUEST),
    ILLEGAL_JDBC_CONNECTION_ARGUMENTS(33, "Illegal JDBC Connection Arguments",
      Response.Status.BAD_REQUEST),
    ILLEGAL_S3_CONNECTOR_BUCKET(34, "Illegal S3 connector bucket",
      Response.Status.BAD_REQUEST),
    ILLEGAL_S3_CONNECTOR_ACCESS_KEY(35, "Illegal S3 connector access key",
      Response.Status.BAD_REQUEST),
    ILLEGAL_S3_CONNECTOR_SECRET_KEY(36, "Illegal S3 connector secret key",
      Response.Status.BAD_REQUEST),
    ILLEGAL_HOPSFS_CONNECTOR_DATASET(37, "Illegal Hopsfs connector dataset",
      Response.Status.BAD_REQUEST),
    ILLEGAL_FEATUREGROUP_NAME(38, "Illegal feature group name", Response.Status.BAD_REQUEST),
    ILLEGAL_FEATUREGROUP_DESCRIPTION(39, "Illegal feature group description",
      Response.Status.BAD_REQUEST),
    ILLEGAL_FEATURE_NAME(40, "Illegal feature name",
      Response.Status.BAD_REQUEST),
    ILLEGAL_FEATURE_DESCRIPTION(41, "Illegal feature description",
      Response.Status.BAD_REQUEST),
    JDBC_CONNECTOR_NOT_FOUND(42, "JDBC Connector not found",
      Response.Status.BAD_REQUEST),
    JDBC_CONNECTOR_ID_NOT_PROVIDED(43, "JDBC Connector Id was not provided", Response.Status.BAD_REQUEST),
    INVALID_SQL_QUERY(44, "Invalid SQL query", Response.Status.BAD_REQUEST),
    S3_CONNECTOR_NOT_FOUND(45, "S3 Connector not found", Response.Status.BAD_REQUEST),
    HOPSFS_CONNECTOR_NOT_FOUND(46, "HopsFs Connector not found", Response.Status.BAD_REQUEST),
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
    ILLEGAL_TRAINING_DATASET_NAME(53, "Illegal training dataset name", Response.Status.BAD_REQUEST),
    S3_CONNECTOR_ID_NOT_PROVIDED(54, "S3 Connector Id was not provided", Response.Status.BAD_REQUEST),
    HOPSFS_CONNECTOR_ID_NOT_PROVIDED(55, "HopsFS Connector Id was not provided", Response.Status.BAD_REQUEST),
    ILLEGAL_TRAINING_DATASET_DESCRIPTION(56, "Illegal training dataset description",
      Response.Status.BAD_REQUEST),
    ILLEGAL_TRAINING_DATASET_DATA_FORMAT(57, "Illegal training dataset data format",
      Response.Status.BAD_REQUEST),
    ILLEGAL_TRAINING_DATASET_VERSION(58, "Illegal training dataset version",
      Response.Status.BAD_REQUEST),
    ILLEGAL_FEATUREGROUP_VERSION(59, "Illegal feature group version",
      Response.Status.BAD_REQUEST),
    ILLEGAL_STORAGE_CONNECTOR_TYPE(60, "The provided storage connector type was not recognized",
        Response.Status.BAD_REQUEST),
    FEATURESTORE_INITIALIZATION_ERROR(61, "Featurestore Initialization Error", Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_UTIL_ARGS_FAILURE(62, "Could not write featurestore util args to HDFS",
        Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_ONLINE_SECRETS_ERROR(63, "Could not get JDBC connection for the online featurestore",
        Response.Status.INTERNAL_SERVER_ERROR),
    FEATURESTORE_ONLINE_NOT_ENABLED(64, "Online featurestore not enabled", Response.Status.BAD_REQUEST),
    SYNC_TABLE_NOT_FOUND(65, "The Hive Table to Sync with the feature store was not " +
      "found in the metastore", Response.Status.BAD_REQUEST),
    COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE(66, "Could not initiate connecton to " +
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
    TRAININGDATASETJOB_FEATURE_NOT_EXISTING(82, "Feature does not exist", Response.Status.BAD_REQUEST),
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
      Response.Status.BAD_REQUEST);

    private int code;
    private String message;
    private Response.Status respStatus;
    public final int range = 270000;

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

    TENSORBOARD_CLEANUP_ERROR(1, "Failed when deleting a running TensorBoard", Response.Status.INTERNAL_SERVER_ERROR),
    TENSORBOARD_START_ERROR(2, "Failed to start TensorBoard", Response.Status.INTERNAL_SERVER_ERROR),
    TENSORBOARD_FETCH_ERROR(3, "Error while fetching TensorBoard from database",
            Response.Status.INTERNAL_SERVER_ERROR);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 280000;

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
    private final int range = 290000;

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
    VERSION_NOT_SPECIFIED(10, "Version not specified.", Response.Status.BAD_REQUEST);

    private int code;
    private String message;
    private Response.Status respStatus;
    public final int range = 300000;

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
    public final int range = 310000;

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
    KEY_NAME_NOT_VALID(9, "Api key name not valid", Response.Status.BAD_REQUEST);

    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    private final int range = 320000;

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

}
