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

package io.hops.hopsworks.common.exception;

import io.hops.hopsworks.common.util.Settings;

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
 * 9. Template error codes start with "18"
 * 10. Kafka error codes start with "19"
 * 11. User error codes start with "20"
 * 12. Security error codes start  with "20".
 * 13. Zeppelin error codes start with "21".
 * 14. CA error codes start with "22".
 * 15. DelaCSR error codes start with "23".
 */
@XmlRootElement
public class RESTCodes {
  
  public interface RESTErrorCode {
    
    Response.StatusType getRespStatus();
    
    Integer getCode();
    
    String getMessage();
  }
  
  public enum ProjectErrorCode implements RESTErrorCode {
    
    //project error response
    NO_ROLE_FOUND(150000, "No valid role found for this user", Response.Status.BAD_REQUEST),
    PROJECT_EXISTS(150001, "Project with the same name already exists.", Response.Status.CONFLICT),
    NUM_PROJECTS_LIMIT_REACHED(150002, "You have reached the maximum number of projects you could create."
      + " Contact an administrator to increase your limit.", Response.Status.BAD_REQUEST),
    INVALID_PROJECT_NAME(150003, "Invalid project name, valid characters: [a-z,0-9].", Response.Status.BAD_REQUEST),
    PROJECT_NOT_FOUND(150004, "Project wasn't found.", Response.Status.BAD_REQUEST),
    PROJECT_NOT_REMOVED(150005, "Project wasn't removed.", Response.Status.BAD_REQUEST),
    PROJECT_FOLDER_NOT_CREATED(150007, "Project folder could not be created in HDFS.",
      Response.Status.INTERNAL_SERVER_ERROR),
    STARTER_PROJECT_BAD_REQUEST(150008, "Type of starter project is not valid", Response.Status.BAD_REQUEST),
    PROJECT_FOLDER_NOT_REMOVED(150009, "Project folder could not be removed from HDFS.", Response.Status.BAD_REQUEST),
    PROJECT_REMOVAL_NOT_ALLOWED(150010, "Project can only be deleted by its owner.", Response.Status.FORBIDDEN),
    PROJECT_MEMBER_NOT_REMOVED(150011, "Failed to remove team member.", Response.Status.INTERNAL_SERVER_ERROR),
    MEMBER_REMOVAL_NOT_ALLOWED(150012, "Your project role does not allow to remove other members of this project.",
      Response.Status.FORBIDDEN),
    PROJECT_OWNER_NOT_ALLOWED(150013, "Removing the project owner is not allowed.", Response.Status.BAD_REQUEST),
    PROJECT_OWNER_ROLE_NOT_ALLOWED(150014, "Changing the role of the project owner is not allowed.",
      Response.Status.BAD_REQUEST),
    FOLDER_INODE_NOT_CREATED(150015, "Folder Inode could not be created in DB.", Response.Status.BAD_REQUEST),
    FOLDER_NAME_NOT_SET(150016, "Name cannot be empty.", Response.Status.BAD_REQUEST),
    FOLDER_NAME_TOO_LONG(150017, "Name cannot be longer than 88 characters.", Response.Status.BAD_REQUEST),
    FOLDER_NAME_CONTAIN_DISALLOWED_CHARS(150018, "Name cannot contain any of the characters ",
      Response.Status.BAD_REQUEST),
    FOLDER_NAME_ENDS_WITH_DOT(150018, "Name cannot end in a period.", Response.Status.BAD_REQUEST),
    FOLDER_NAME_EXISTS(150018, "A directory with the same name already exists. "
      + "If you want to replace it delete it first then try recreating.", Response.Status.BAD_REQUEST),
    FILE_NAME_EXIST(150019, "File with the same name already exists.", Response.Status.BAD_REQUEST),
    FILE_NOT_FOUND(150020, "File not found.", Response.Status.BAD_REQUEST),
    PROJECT_SERVICE_NOT_FOUND(150020, "service was not found.", Response.Status.BAD_REQUEST),
    NO_MEMBER_TO_ADD(150021, " No member to add.", Response.Status.BAD_REQUEST),
    NO_MEMBER_ADD(150022, " No member added.", Response.Status.BAD_REQUEST),
    TEAM_MEMBER_NOT_FOUND(150023, " The selected user is not a team member in this project.",
      Response.Status.BAD_REQUEST),
    TEAM_MEMBER_ALREADY_EXISTS(150024, " The selected user is already a team member of this project.",
      Response.Status.BAD_REQUEST),
    ROLE_NOT_SET(150025, "Role cannot be empty.", Response.Status.BAD_REQUEST),
    PROJECT_NOT_SELECTED(150026, "No project selected", Response.Status.BAD_REQUEST),
    QUOTA_NOT_FOUND(150027, "Quota information not found.", Response.Status.BAD_REQUEST),
    QUOTA_REQUEST_NOT_COMPLETE(150028, "Please specify both " + "namespace and space quota.",
      Response.Status.BAD_REQUEST),
    QUOTA_ERROR(150028, "Quota update error.", Response.Status.BAD_REQUEST),
    PROJECT_QUOTA_ERROR(150029, "This project is out of credits.", Response.Status.PRECONDITION_FAILED),
    
    //project success messages
    PROJECT_CREATED(150030, "Project created successfully.", Response.Status.CREATED),
    PROJECT_DESCRIPTION_CHANGED(150031, "Project description changed.", Response.Status.OK),
    PROJECT_RETENTON_CHANGED(150032, "Project retention period changed.", Response.Status.OK),
    
    PROJECT_SERVICE_ADDED(150033, "Project service added", Response.Status.OK),
    PROJECT_SERVICE_ADD_FAILURE(150034, "Failure adding service", Response.Status.OK),
    PROJECT_REMOVED(150035, "The project and all related files were removed successfully.", Response.Status.OK),
    PROJECT_REMOVED_NOT_FOLDER(150036, "The project was removed successfully. But its datasets have not been deleted.",
      Response.Status.OK),
    PROJECT_MEMBER_REMOVED(150037, "Member removed successfully", Response.Status.OK),
    PROJECT_MEMBERS_ADDED(150038, "Members added successfully", Response.Status.OK),
    PROJECT_MEMBER_ADDED(150039, "One member added successfully", Response.Status.OK),
    MEMBER_ROLE_UPDATED(150040, "Role updated successfully.", Response.Status.OK),
    MEMBER_REMOVED_FROM_TEAM(150041, "Member removed from team.", Response.Status.OK),
    PROJECT_INODE_CREATION_ERROR(150042, "Could not create dummy Inode", Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_FOLDER_EXISTS(150043, "A folder with same name as the project already exists in the system.",
      Response.Status.CONFLICT),
    PROJECT_USER_EXISTS(150044, "Filesystem user(s) already exists in the system.", Response.Status.CONFLICT),
    PROJECT_GROUP_EXISTS(150045, "Filesystem group(s) already exists in the system.", Response.Status.CONFLICT),
    PROJECT_CERTIFICATES_EXISTS(150046, "Certificates for this project already exist in the system.",
      Response.Status.CONFLICT),
    PROJECT_QUOTA_EXISTS(150047, "Quotas corresponding to this project already exist in the system.",
      Response.Status.CONFLICT),
    PROJECT_LOGS_EXIST(150048, "Logs corresponding to this project already exist in the system.",
      Response.Status.CONFLICT),
    PROJECT_VERIFICATIONS_FAILED(150049, "Error occurred while running verifications",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_SET_PERMISSIONS_ERROR(150050, "Error occurred while setting permissions for project folders.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HANDLER_PRECREATE_ERROR(150051, "Error occurred during project precreate handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HANDLER_POSTCREATE_ERROR(150052, "Error occurred during project postcreate handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HANDLER_PREDELETE_ERROR(150053, "Error occurred during project predelete handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HANDLER_POSTDELETE_ERROR(150054, "Error occurred during project postdelete handler.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_TOUR_FILES_ERROR(150055, "Error while adding tour files to project.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_KIBANA_CREATE_INDEX_ERROR(150056, "Could not create kibana index-pattern for project",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_KIBANA_CREATE_SEARCH_ERROR(150057, "Could not create kibana search for project",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_KIBANA_CREATE_DASHBOARD_ERROR(150057, "Could not create kibana dashboard for project",
      Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_HIVEDB_CREATE_ERROR(150058, "Could not create Hive DB for project", Response.Status.INTERNAL_SERVER_ERROR),
    PROJECT_CONDA_LIBS_NOT_FOUND(150059, "No preinstalled anaconda libs found.",
      Response.Status.NOT_FOUND),
    KILL_MEMBER_JOBS(150060, "Could not kill user's yarn applications", Response.Status.INTERNAL_SERVER_ERROR),
    JUPYTER_SERVER_NOT_FOUND(150060, "Could not find Jupyter entry for user in this project.",
      Response.Status.NOT_FOUND),
    PYTHON_LIB_ALREADY_INSTALLED(150061, "This python library is already installed on this project",
      Response.Status.NOT_MODIFIED),
    PYTHON_LIB_NOT_INSTALLED(150062, "This python library is not installed for this project. Cannot remove/upgrade " +
      "op", Response.Status.NOT_MODIFIED),
    PROJECT_QUOTA_INSUFFICIENT(150063, "This project is out of credits", Response.Status.PRECONDITION_FAILED),
    ANACONDA_NOT_ENABLED(150064, "First enable Anaconda. Click on 'Python' -> Pick a version",
      Response.Status.PRECONDITION_FAILED),
    TENSORBOARD_ELASTIC_INDEX_NOT_FOUND(150065, "Could not find elastic index for TensorBoard.",
      Response.Status.NOT_FOUND);
    
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    ProjectErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum DatasetErrorCode implements RESTErrorCode {
    
    //DataSet
    DATASET_OPERATION_FORBIDDEN(110000, "Dataset/content operation forbidden", Response.Status.FORBIDDEN),
    DATASET_OPERATION_INVALID(110001, "Operation cannot be performed.", Response.Status.BAD_REQUEST),
    DATASET_OPERATION_ERROR(110002, "Dataset operation failed.", Response.Status.INTERNAL_SERVER_ERROR),
    DATASET_ALREADY_SHARED_WITH_PROJECT(110003, "Dataset already shared with project", Response.Status.BAD_REQUEST),
    DATASET_NOT_SHARED_WITH_PROJECT(11004, "Dataset is not shared with project.", Response.Status.BAD_REQUEST),
    DATASET_NAME_EMPTY(110005, "DataSet name cannot be empty.", Response.Status.BAD_REQUEST),
    FILE_CORRUPTED_REMOVED_FROM_HDFS(110006, "Corrupted file removed from hdfs.", Response.Status.BAD_REQUEST),
    INODE_DELETION_ERROR(110007, "File/Dir could not be deleted.", Response.Status.INTERNAL_SERVER_ERROR),
    INODE_NOT_FOUND(110008, "Inode was not found.", Response.Status.NOT_FOUND),
    DATASET_REMOVED_FROM_HDFS(110009, "DataSet removed from hdfs.", Response.Status.BAD_REQUEST),
    SHARED_DATASET_REMOVED(110010, "The shared dataset has been removed from this project.",
      Response.Status.BAD_REQUEST),
    DATASET_NOT_FOUND(110011, "DataSet not found.", Response.Status.BAD_REQUEST),
    DESTINATION_EXISTS(110012, "Destination already exists.", Response.Status.BAD_REQUEST),
    DATASET_ALREADY_PUBLIC(110013, "Dataset is already in project.", Response.Status.CONFLICT),
    DATASET_ALREADY_IN_PROJECT(110014, "Dataset is already public.", Response.Status.BAD_REQUEST),
    DATASET_NOT_PUBLIC(110015, "DataSet is not public.", Response.Status.BAD_REQUEST),
    DATASET_NOT_EDITABLE(110016, "DataSet is not editable.", Response.Status.BAD_REQUEST),
    DATASET_PENDING(110017, "DataSet is not yet accessible. Accept the share request to access it.",
      Response.Status.BAD_REQUEST),
    PATH_NOT_FOUND(110018, "Path not found", Response.Status.BAD_REQUEST),
    PATH_NOT_DIRECTORY(110019, "Requested path is not a directory", Response.Status.BAD_REQUEST),
    PATH_IS_DIRECTORY(110020, "Requested path is a directory", Response.Status.BAD_REQUEST),
    DOWNLOAD_ERROR(110021, "You cannot download from a non public shared dataset", Response.Status.BAD_REQUEST),
    DOWNLOAD_PERMISSION_ERROR(110022, "Your role does not allow to download this file", Response.Status.BAD_REQUEST),
    DATASET_PERMISSION_ERROR(110023, "Could not update dataset permissions", Response.Status.INTERNAL_SERVER_ERROR),
    COMPRESSION_ERROR(110024, "Error while performing a (un)compress operation",
      Response.Status.INTERNAL_SERVER_ERROR),
    DATASET_OWNER_ERROR(110025, "You cannot perform this action on a dataset you are not the owner",
      Response.Status.BAD_REQUEST),
    DATASET_PUBLIC_IMMUTABLE(110026, "Public datasets are immutable.", Response.Status.BAD_REQUEST),
    DATASET_NAME_INVALID(110028, "Name of dir is invalid", Response.Status.BAD_REQUEST),
    IMAGE_SIZE_INVALID(110029, "Image is too big to display please download it by double-clicking it instead",
      Response.Status.BAD_REQUEST),
    FILE_PREVIEW_ERROR(110030,
      Settings.README_FILE + " must be smaller than " + Settings.FILE_PREVIEW_TXT_SIZE_BYTES / 1024
        + " KB to be previewed", Response.Status.BAD_REQUEST),
    DATASET_PARAMETERS_INVALID(110031, "Invalid parameters for requested dataset operation",
      Response.Status.BAD_REQUEST),
    EMPTY_PATH(110032, "Empty path requested", Response.Status.BAD_REQUEST),
    
    UPLOAD_PATH_NOT_SPECIFIED(110035, "The path to upload the template was not specified",
      Response.Status.BAD_REQUEST),
    README_NOT_ACCESSIBLE(110036, "Readme not accessible.", Response.Status.UNAUTHORIZED),
    COMPRESSION_SIZE_ERROR(110037, "Not enough free space on the local scratch directory to download and unzip this " +
      "file. Talk to your admin to increase disk space at the path: hopsworks/staging_dir",
      Response.Status.PRECONDITION_FAILED),
    INVALID_PATH_FILE(110038, "The requested path does not resolve to a valid file", Response.Status.BAD_REQUEST),
    INVALID_PATH_DIR(110038, "The requested path does not resolve to a valid directory", Response.Status.BAD_REQUEST),
    UPLOAD_DIR_CREATE_ERROR(110039, "Uploads directory could not be created in the file system",
      Response.Status.INTERNAL_SERVER_ERROR),
    UPLOAD_CONCURRENT_ERROR(110040, "A file with the same name is being uploaded", Response.Status.PRECONDITION_FAILED),
    UPLOAD_RESUMABLEINFO_INVALID(110041, "ResumableInfo is invalid", Response.Status.BAD_REQUEST),
    UPLOAD_ERROR(110042, "Error occurred while uploading file",
      Response.Status.INTERNAL_SERVER_ERROR),
    DATASET_REQUEST_EXISTS(110043, "Request for this dataset from this project already exists.",
      Response.Status.CONFLICT),
    COPY_FROM_PROJECT(110044, "Cannot copy file/folder from another project", Response.Status.FORBIDDEN),
    COPY_TO_PUBLIC_DS(110045, "Can not copy to a public dataset.", Response.Status.FORBIDDEN);
    
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    DatasetErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum MetadataErrorCode implements RESTErrorCode {
    
    TEMPLATE_ALREADY_AVAILABLE(180000, "The template is already available", Response.Status.BAD_REQUEST),
    TEMPLATE_INODEID_EMPTY(180001, "The template id is empty", Response.Status.BAD_REQUEST),
    TEMPLATE_NOT_ATTACHED(180002, "The template could not be attached to a file", Response.Status.BAD_REQUEST),
    DATASET_TEMPLATE_INFO_MISSING(180003, "Template info is missing. Please provide InodeDTO path and templateId.",
      Response.Status.BAD_REQUEST),
    NO_METADATA_EXISTS(180004, "No metadata found", Response.Status.BAD_REQUEST),
    METADATA_MAX_SIZE_EXCEEDED(180005, "Metadata is too long. 12000 characters is the maximum size",
      Response.Status.BAD_REQUEST);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    MetadataErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum JobErrorCode implements RESTErrorCode {
    
    // JOBS Error Messages
    JOB_START_FAILED(130000, "An error occurred while trying to start this job.", Response.Status.BAD_REQUEST),
    JOB_STOP_FAILED(130001, "An error occurred while trying to stop this job.", Response.Status.BAD_REQUEST),
    JOB_TYPE_UNSUPPORTED(130002, "Unsupported job type.", Response.Status.BAD_REQUEST),
    JOB_ACTION_UNSUPPORTED(130003, "Unsupported action type.", Response.Status.BAD_REQUEST),
    JOB_NAME_EXISTS(130004, "Job with same name already exists.", Response.Status.CONFLICT),
    JOB_NAME_EMPTY(130005, "Job name is not set.", Response.Status.BAD_REQUEST),
    JOB_NAME_INVALID(130006, "Job name is invalid. Invalid charater(s) in job name, the following characters "
      + "(including space) are now allowed:" + Settings.FILENAME_DISALLOWED_CHARS, Response.Status.BAD_REQUEST),
    JOB_EXECUTION_NOT_FOUND(130007, "Execution not found.", Response.Status.NOT_FOUND),
    JOB_EXECUTION_TRACKING_URL_NOT_FOUND(130008, "Tracking url not found.", Response.Status.BAD_REQUEST),
    JOB_NOT_FOUND(130009, "Job not found.", Response.Status.NOT_FOUND),
    JOB_EXECUTION_INVALID_STATE(130010, "Execution state is invalid.", Response.Status.BAD_REQUEST),
    JOB_LOG(130011, "Job log error.", Response.Status.BAD_REQUEST),
    JOB_DELETION_ERROR(130012, "Error while deleting job.", Response.Status.BAD_REQUEST),
    JOB_CREATION_ERROR(130013, "Error while creating job.", Response.Status.BAD_REQUEST),
    ELASTIC_INDEX_NOT_FOUND(130014, "Elasticsearch indices do not exist", Response.Status.BAD_REQUEST),
    ELASTIC_TYPE_NOT_FOUND(130015, "Elasticsearch type does not exist", Response.Status.BAD_REQUEST),
    
    TENSORBOARD_ERROR(130016, "Error getting the Tensorboard(s) for this application", Response.Status.NO_CONTENT),
    
    APPLICATIONID_NOT_FOUND(130017, "Error while deleting job.", Response.Status.BAD_REQUEST),
    JOB_ACCESS_ERROR(130018, "Cannot access job", Response.Status.FORBIDDEN),
    LOG_AGGREGATION_NOT_ENABLED(130019, "YARN log aggregation is not enabled", Response.Status.SERVICE_UNAVAILABLE),
    LOG_RETRIEVAL_ERROR(130020, "Error while retrieving YARN logs", Response.Status.INTERNAL_SERVER_ERROR),
    
    JOB_SCHEDULE_UPDATE(130021, "Could not update schedule.", Response.Status.INTERNAL_SERVER_ERROR),
    JAR_INSEPCTION_ERROR(130022, "Could not inspect jar file.", Response.Status.INTERNAL_SERVER_ERROR),
    PROXY_ERROR(130023, "Could not get proxy user.", Response.Status.INTERNAL_SERVER_ERROR);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    JobErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum RequestErrorCode implements RESTErrorCode {
    
    EMAIL_EMPTY(140001, "Email cannot be empty.", Response.Status.BAD_REQUEST),
    EMAIL_INVALID(140002, "Not a valid email address.", Response.Status.BAD_REQUEST),
    DATASET_REQUEST_ERROR(140003, "Error while submitting dataset request", Response.Status.BAD_REQUEST),
    REQUEST_UNKNOWN_ACTION(140004, "Unknown request action", Response.Status.BAD_REQUEST),
    MESSAGE_NOT_FOUND(140005, "Message was not found", Response.Status.NOT_FOUND),
    MESSAGE_ACCESS_NOT_ALLOWED(140006, "Message not allowed.", Response.Status.FORBIDDEN);
    
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    RequestErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum ServiceErrorCode implements RESTErrorCode {
    
    // Zeppelin
    ZEPPELIN_ADD_FAILURE(100000, "Failed to create Zeppelin notebook dir. Zeppelin will not work properly. "
      + "Try recreating the following dir manually:", Response.Status.SERVICE_UNAVAILABLE),
    JUPYTER_ADD_FAILURE(100001, "Failed to create Jupyter notebook dir. Jupyter will not work properly. "
      + "Try recreating the following dir manually.", Response.Status.BAD_REQUEST),
    ELASTIC_SERVER_NOT_AVAILABLE(100002, "The Elasticsearch Server is either down or misconfigured.",
      Response.Status.BAD_REQUEST),
    ELASTIC_SERVER_NOT_FOUND(100003, "Problem when reaching the Elasticsearch server",
      Response.Status.SERVICE_UNAVAILABLE),
    
    //Hive
    HIVE_ADD_FAILURE(100004, "Failed to create the Hive database", Response.Status.BAD_REQUEST),
    // LLAP
    LLAP_STATUS_INVALID(100005, "Unrecognized new LLAP status", Response.Status.BAD_REQUEST),
    LLAP_CLUSTER_ALREADY_UP(100006, "LLAP cluster already up", Response.Status.BAD_REQUEST),
    LLAP_CLUSTER_ALREADY_DOWN(100007, "LLAP cluster already down", Response.Status.BAD_REQUEST),
    
    //Database
    DATABASE_UNAVAILABLE(100008, "The database is temporarily unavailable. Please try again later",
      Response.Status.SERVICE_UNAVAILABLE),
    TENSORBOARD_CLEANUP_ERROR(100009, "Could not delete tensorboard", Response.Status.INTERNAL_SERVER_ERROR),
    ZOOKEEPER_SERVICE_UNAVAILABLE(100010, "ZooKeeper service unavailable", Response.Status.SERVICE_UNAVAILABLE),
    ANACONDA_NODES_UNAVAILABLE(100011, "No conda machine is enabled. Contact the administrator.",
      Response.Status.SERVICE_UNAVAILABLE),
    ELASTIC_INDEX_CREATION_ERROR(100012, "Error while creating index in elastic",
      Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_LIST_LIB_FORMAT_ERROR(100013,
      "Problem listing libraries. Did conda get upgraded and change its output format?",
      Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_LIST_LIB_ERROR(100014, "Problem listing libraries. Please contact the Administrator",
      Response.Status.INTERNAL_SERVER_ERROR),
    ZEPPELIN_KILL_ERROR(100015, "Could not close zeppelin interpreters, please wait 60 seconds to retry",
      Response.Status.INTERNAL_SERVER_ERROR),
    JUPYTER_HOME_ERROR(100016, "Couldn't resolve JUPYTER_HOME using DB.", Response.Status.INTERNAL_SERVER_ERROR),
    JUPYTER_STOP_ERROR(100017, "Couldn't stop Jupyter Notebook Server.", Response.Status.INTERNAL_SERVER_ERROR),
    INVALID_YML(100018, "Invalid .yml file", Response.Status.BAD_REQUEST),
    INVALID_YML_SIZE(100019, ".yml file too large. Maximum size is 10000 bytes",
      Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_FROM_YML_ERROR(100020, "Failed to create Anaconda environment from .yml file.",
      Response.Status.INTERNAL_SERVER_ERROR),
    PYTHON_INVALID_VERSION(100021, "Invalid version of python (valid: '2.7', and '3.5', and '3.6'",
      Response.Status.BAD_REQUEST),
    ANACONDA_REPO_ERROR(100022, "Problem adding the repo.", Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_OP_IN_PROGRESS(100023, "A conda environment operation is currently executing (create/remove/list). Wait " +
      "for it to finish or clear it first.", Response.Status.PRECONDITION_FAILED),
    HOST_TYPE_NOT_FOUND(100024, "No hosts with the desired capability.", Response.Status.PRECONDITION_FAILED),
    HOST_NOT_FOUND(100025, "Hosts was not found.", Response.Status.NOT_FOUND),
    HOST_NOT_REGISTERED(100026, "Host has not registered.", Response.Status.NOT_FOUND),
    ANACONDA_DEP_REMOVE_FORBIDDEN(100027, "Could not uninstall library, it is a mandatory dependency",
      Response.Status.BAD_REQUEST),
    ANACONDA_DEP_INSTALL_FORBIDDEN(100028, "Library is already installed", Response.Status.CONFLICT),
    ANACONDA_EXPORT_ERROR(100029, "Failed to export Anaconda environment as .yml",
      Response.Status.INTERNAL_SERVER_ERROR),
    ANACONDA_LIST_LIB_NOT_FOUND(100030, "No results found", Response.Status.NO_CONTENT),
    ELASTIC_INDEX_NOT_FOUND(100031, "Elastic index was not found in elasticsearch", Response.Status.NOT_FOUND),
    ELASTIC_INDEX_TYPE_NOT_FOUND(100032, "Elastic index type was not found in elasticsearch",
      Response.Status.NOT_FOUND),
    JUPYTER_SERVERS_NOT_FOUND(100033, "Could not find any Jupyter notebook servers for this project.",
      Response.Status.NOT_FOUND),
    JUPYTER_SERVERS_NOT_RUNNING(100034, "Could not find any Jupyter notebook servers for this project.",
      Response.Status.PRECONDITION_FAILED),
    JUPYTER_START_ERROR(100035, "Jupyter server could not start.", Response.Status.INTERNAL_SERVER_ERROR),
    JUPYTER_SAVE_SETTINGS_ERROR(100036, "Could not save Jupyter Settings.", Response.Status.INTERNAL_SERVER_ERROR),
    IPYTHON_CONVERT_ERROR(100037, "Problem converting ipython notebook to python program",
      Response.Status.EXPECTATION_FAILED),
    TENSORBOARD_FETCH_ERROR(100038, "Error while fetching TensorBoard from database",
      Response.Status.INTERNAL_SERVER_ERROR),
    EMAIL_SENDING_FAILURE(100039, "Could not send email", Response.Status.EXPECTATION_FAILED),
    HOST_EXISTS(100040, "Host exists", Response.Status.CONFLICT);
    
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    ServiceErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum KafkaErrorCode implements RESTErrorCode {
    
    // Zeppelin
    TOPIC_NOT_FOUND(190000, "No topics found", Response.Status.NOT_FOUND),
    BROKER_METADATA_ERROR(190001, "An error occured while retrieving topic metadata from broker",
      Response.Status.INTERNAL_SERVER_ERROR),
    TOPIC_ALREADY_EXISTS(190002, "Kafka topic already exists in database. Pick a different topic name",
      Response.Status.CONFLICT),
    TOPIC_ALREADY_EXISTS_IN_ZOOKEEPER(190003, "Kafka topic already exists in ZooKeeper. Pick a different topic name",
      Response.Status.CONFLICT),
    TOPIC_LIMIT_REACHED(190004, "Topic limit reached. Contact your administrator to increase the number of topics " +
      "that can be created for this project.", Response.Status.PRECONDITION_FAILED),
    TOPIC_REPLICATION_ERROR(190005, "Maximum topic replication factor exceeded", Response.Status.BAD_REQUEST),
    SCHEMA_NOT_FOUND(190006, "Topic has not schema attached to it.", Response.Status.NOT_FOUND),
    KAFKA_GENERIC_ERROR(190007, "An error occured while retrieving information from Kafka service",
      Response.Status.INTERNAL_SERVER_ERROR),
    DESTINATION_PROJECT_IS_TOPIC_OWNER(190008, "Destination projet is topic owner", Response.Status.BAD_REQUEST),
    TOPIC_ALREADY_SHARED(190009, "Topic is already shared", Response.Status.BAD_REQUEST),
    TOPIC_NOT_SHARED(190010, "Topic is not shared with project", Response.Status.NOT_FOUND),
    ACL_ALREADY_EXISTS(190011, "ACL already exists.", Response.Status.CONFLICT),
    ACL_NOT_FOUND(190012, "ACL not found.", Response.Status.NOT_FOUND),
    ACL_NOT_FOR_TOPIC(190013, "ACL does not belong to the specified topic", Response.Status.BAD_REQUEST),
    SCHEMA_IN_USE(190014, "Schema is currently used by topics. topic", Response.Status.PRECONDITION_FAILED);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    KafkaErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum GenericErrorCode implements RESTErrorCode {
    
    UNKNOWN_ERROR(120000, "A generic error occurred.", Response.Status.INTERNAL_SERVER_ERROR),
    ILLEGAL_ARGUMENT(120001, "An argument was not provided or it was malformed.", Status.UNPROCESSABLE_ENTITY),
    ILLEGAL_STATE(120002, "A runtime error occurred.", Response.Status.BAD_REQUEST),
    ROLLBACK(120003, "The last transaction did not complete as expected", Response.Status.INTERNAL_SERVER_ERROR),
    WEBAPPLICATION(120004, "Web application exception occurred", null),
    PERSISTENCE_ERROR(120005, "Persistence error occured", Response.Status.INTERNAL_SERVER_ERROR),
    UNKNOWN_ACTION(120006, "This action can not be applied on this resource.", Response.Status.BAD_REQUEST),
    INCOMPLETE_REQUEST(120007, "Some parameters were not provided or were not in the required format.",
      Response.Status.BAD_REQUEST),
    SECURITY_EXCEPTION(120008, "A Java security error occurred.", Response.Status.INTERNAL_SERVER_ERROR);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    GenericErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  
  public enum SecurityErrorCode implements RESTErrorCode {
    
    MASTER_ENCRYPTION_PASSWORD_CHANGE(200001,
      "Master password change procedure started. Check your inbox for final status", Response.Status.BAD_REQUEST),
    HDFS_ACCESS_CONTROL(200002, "Access error while trying to access hdfs resource", Response.Status.FORBIDDEN),
    EJB_ACCESS_LOCAL(200003, "EJB access local error", Response.Status.UNAUTHORIZED),
    CERT_CREATION_ERROR(200004, "Error while generating certificates.", Response.Status.INTERNAL_SERVER_ERROR),
    CERT_CN_EXTRACT_ERROR(200005, "Error while extracting CN from certificate.", Response.Status.INTERNAL_SERVER_ERROR),
    CERT_ERROR(200006, "Certificate could not be validated.", Response.Status.UNAUTHORIZED),
    CERT_ACCESS_DENIED(200007, "Certificate access denied.", Response.Status.FORBIDDEN),
    CSR_ERROR(200008, "Error while signing CSR.", Response.Status.INTERNAL_SERVER_ERROR),
    CERT_APP_REVOKE_ERROR(200009, "Error while revoking application certificate, check the logs",
      Response.Status.INTERNAL_SERVER_ERROR),
    CERT_LOCATION_UNDEFINED(200010, "Could not identify local directory to clean certificates. Manual cleanup " +
      "required.", Response.Status.INTERNAL_SERVER_ERROR),
    MASTER_ENCRYPTION_PASSWORD_ACCESS_ERROR(200011, "Could not read master encryption password.",
      Response.Status.INTERNAL_SERVER_ERROR);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    SecurityErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum UserErrorCode implements RESTErrorCode {
    
    USER_DOES_NOT_EXIST(160001, "User does not exist.", Response.Status.BAD_REQUEST),
    USER_WAS_NOT_FOUND(160002, "User not found", Response.Status.NOT_FOUND),
    USER_EXISTS(160003, "There is an existing account associated with this email", Response.Status.CONFLICT),
    ACCOUNT_REQUEST(160004, "Your account has not yet been approved.", Response.Status.UNAUTHORIZED),
    ACCOUNT_DEACTIVATED(160005, "This account has been deactivated.", Response.Status.UNAUTHORIZED),
    ACCOUNT_VERIFICATION(160006, "You need to verify your account.", Response.Status.BAD_REQUEST),
    ACCOUNT_BLOCKED(160007, "Your account has been blocked. Contact the administrator.", Response.Status.UNAUTHORIZED),
    AUTHENTICATION_FAILURE(160008, "Authentication failed", Response.Status.UNAUTHORIZED),
    LOGOUT_FAILURE(160009, "Logout failed on backend.", Response.Status.BAD_REQUEST),
    
    SEC_Q_EMPTY(160010, "Security Question cannot be empty.", Response.Status.BAD_REQUEST),
    SEC_A_EMPTY(160011, "Security Answer cannot be empty.", Response.Status.BAD_REQUEST),
    SEC_Q_NOT_IN_LIST(160011, "Choose a Security Question from the list.", Response.Status.BAD_REQUEST),
    SEC_QA_INCORRECT(160012, "Security question or answer did not match", Response.Status.BAD_REQUEST),
    PASSWORD_EMPTY(160013, "Password cannot be empty.", Response.Status.BAD_REQUEST),
    PASSWORD_TOO_SHORT(160014, "Password too short.", Response.Status.BAD_REQUEST),
    PASSWORD_TOO_LONG(160015, "Password too long.", Response.Status.BAD_REQUEST),
    PASSWORD_INCORRECT(160016, "Password incorrect", Response.Status.BAD_REQUEST),
    PASSWORD_PATTERN_NOT_CORRECT(160017, "Password should include one uppercase letter, "
      + "one special character and/or alphanumeric characters.", Response.Status.BAD_REQUEST),
    INCORRECT_PASSWORD(160018, "The password is incorrect. Please try again", Response.Status.UNAUTHORIZED),
    PASSWORD_MISS_MATCH(160019, "Passwords do not match - typo?", Response.Status.BAD_REQUEST),
    TOS_NOT_AGREED(160020, "You must agree to our terms of use.", Response.Status.BAD_REQUEST),
    CERT_DOWNLOAD_DENIED(160021, "Admin is not allowed to download certificates", Response.Status.BAD_REQUEST),
    
    //success response
    CREATED_ACCOUNT(160022, "You have successfully created an account\n"
      + "but you might need to wait until your account has been approved \n"
      + "before you can login.", Response.Status.BAD_REQUEST),
    PASSWORD_RESET_SUCCESSFUL(160023, "Your password was successfully reset your new password have been sent to your "
      + "email.", Response.Status.BAD_REQUEST),
    PASSWORD_RESET_UNSUCCESSFUL(160024, "Your password could not be reset. Please try again later or contact support.",
      Response.Status.BAD_REQUEST),
    PASSWORD_CHANGED(160025, "Your password was successfully changed.", Response.Status.BAD_REQUEST),
    SEC_QA_CHANGED(160026, "Your have successfully changed your security questions and answer.",
      Response.Status.BAD_REQUEST),
    PROFILE_UPDATED(160027, "Your profile was updated successfully.", Response.Status.BAD_REQUEST),
    SSH_KEY_REMOVED(160028, "Your ssh key was deleted successfully.", Response.Status.BAD_REQUEST),
    NOTHING_TO_UPDATE(160029, "Nothing to update", Response.Status.BAD_REQUEST),
    AUTHORIZATION_FAILURE(160033, "Authorization failed", Response.Status.UNAUTHORIZED),
    CREATE_USER_ERROR(160034, "Error while creating user", Response.Status.INTERNAL_SERVER_ERROR),
    CERT_AUTHORIZATION_ERROR(160035, "Certificate CN does not match the username provided.",
      Response.Status.UNAUTHORIZED),
    PROJECT_USER_CERT_NOT_FOUND(160036, "Could not find exactly one certificate for user in project.",
      Response.Status.FORBIDDEN),
    ACCOUNT_INACTIVE(160037, "This account has not been activated", Response.Status.UNAUTHORIZED),
    ACCOUNT_LOST_DEVICE(160038, "This account has registered a lost device.", Response.Status.UNAUTHORIZED),
    ACCOUNT_NOT_APPROVED(160039, "This account has not yet been approved", Response.Status.UNAUTHORIZED),
    INVALID_EMAIL(160040, "Invalid email format.", Response.Status.BAD_REQUEST),
    INCORRECT_DEACTIVATION_LENGTH(160041, "The message should have a length between 5 and 500 characters",
      Response.Status.BAD_REQUEST),
    TMP_CODE_INVALID(160042, "The temporary code was wrong.", Response.Status.UNAUTHORIZED),
    INCORRECT_CREDENTIALS(160043, "Incorrect email or password.", Response.Status.UNAUTHORIZED),
    INCORRECT_VALIDATION_KEY(160044, "Incorrect validation key", Response.Status.UNAUTHORIZED),
    ACCOUNT_ALREADY_VERIFIED(160045, "User is already verified", Response.Status.BAD_REQUEST),
    TWO_FA_ENABLE_ERROR(160046, "Cannot enable 2-factor authentication.", Response.Status.INTERNAL_SERVER_ERROR),
    ACCOUNT_REGISTRATION_ERROR(160047, "Cannot enable 2-factor authentication.", Response.Status.INTERNAL_SERVER_ERROR),
    TWO_FA_DISABLED(160048, "2FA is not enabled.", Response.Status.PRECONDITION_FAILED),
    TRANSITION_STATUS_ERROR(160049, "The user can't transition from current status to requested status",
      Response.Status.BAD_REQUEST);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    UserErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum DelaErrorCode implements RESTErrorCode {
    
    //response for validation error
    THIRD_PARTY_ERROR(170000, "Generic third party error.", Response.Status.EXPECTATION_FAILED),
    CLUSTER_NOT_REGISTERED(170001, "Cluster not registered.", Response.Status.UNAUTHORIZED),
    HEAVY_PING(170002, "Heavy ping required.", Response.Status.BAD_REQUEST),
    USER_NOT_REGISTERED(170003, "User not registered.", Response.Status.UNAUTHORIZED),
    DATASET_EXISTS(170004, "Dataset exists.", Response.Status.CONFLICT),
    DATASET_DOES_NOT_EXIST(170005, "Dataset does not exist.", Response.Status.NOT_FOUND),
    DATASET_PUBLISH_PERMISSION_ERROR(17006, "Dataset shared - only owner can publish.", Response.Status.FORBIDDEN),
    ILLEGAL_ARGUMENT(170007, "Illegal Argument.", Response.Status.BAD_REQUEST),
    README_RETRIEVAL_FAILED(170008, "Readme retrieval failed.", Response.Status.EXPECTATION_FAILED),
    README_ACCESS_PROBLEM(170009, "Readme access problem.", Response.Status.BAD_REQUEST),
    COMMUNICATION_FAILURE(170010, "Communication failure.", Response.Status.EXPECTATION_FAILED),
    DATASET_EMPTY(170011, "Dataset empty.", Response.Status.BAD_REQUEST),
    SUBDIRS_NOT_SUPPORTED(170012, "Subdirectories not supported.", Response.Status.BAD_REQUEST),
    ACCESS_ERROR(170013, "Access error.", Response.Status.BAD_REQUEST),
    MISCONFIGURED(170014, "misconfigured", Response.Status.EXPECTATION_FAILED),
    USER_NOT_FOUND(170015, "user not found.", Response.Status.FORBIDDEN),
    DATASET_DELETE_ERROR(170016, "Delete dataset error.", Response.Status.EXPECTATION_FAILED),
    DELA_NOT_AVAILABLE(170017, "Dela not available.", Response.Status.BAD_REQUEST),
    HOPSSITE_NOT_AVAILABLE(170018, "hopssite not available", Response.Status.BAD_REQUEST),
    REMOTE_DELA_NOT_AVAILABLE(170019, "Remote dela not available.", Response.Status.BAD_REQUEST),
    DELA_TRANSFER_NOT_AVAILABLE(170020, "Dela transfer not available.", Response.Status.BAD_REQUEST),
    MANIFEST_ENCODING_ERROR(170021, "Manifest cannot be read as UTF", Response.Status.BAD_REQUEST),
    DATASET_NOT_PUBLIC(170022, "dataset not public - no manifest", Response.Status.BAD_REQUEST),
    INODE_NOT_FOUND(170023, "Inode not found.", Response.Status.BAD_REQUEST);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    DelaErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  
  public enum ZeppelinErrorCode implements RESTErrorCode {
    
    // Zeppelin
    PROJECT_NOT_FOUND(210000, "Could not find project. Make sure cookies are enabled.", Response.Status.FORBIDDEN),
    ROLE_NOT_FOUND(210001, "No role in this project.", Response.Status.FORBIDDEN),
    WEB_SOCKET_ERROR(210002, "Could not connect to websocket", Response.Status.BAD_REQUEST),
    USER_NOT_FOUND(210003, "Could not find remote user", Response.Status.FORBIDDEN),
    INTERPRETER_CLOSE_ERROR(210004, "Could not close interpreter. Make sure it is not running.",
      Response.Status.BAD_REQUEST),
    RESTART_ERROR(210005, "This service has been restarted recently. Please wait a few minutes before trying again.",
      Response.Status.BAD_REQUEST),
    STOP_SESSIONS_ERROR(210006, "You can't stop sessions in another project.", Response.Status.BAD_REQUEST),
    CREATE_NOTEBOOK_ERROR(210007, "Could not create notebook.", Response.Status.BAD_REQUEST);
    
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    ZeppelinErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum CAErrorCode implements RESTErrorCode {
    
    BADSIGNREQUEST(220000, "No CSR provided", Response.Status.BAD_REQUEST),
    BADREVOKATIONREQUEST(220001, "No certificate identifier provided", Response.Status.BAD_REQUEST),
    CERTNOTFOUND(220002, "Certificate file not found", Response.Status.NO_CONTENT);
    
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
    
    CAErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
    
  }
  
  public enum DelaCSRErrorCode implements RESTErrorCode {
  
    BADREQUEST(220001, "User or CS not set", Response.Status.BAD_REQUEST),
    EMAIL(220002, "CSR email not set or does not match user", Response.Status.UNAUTHORIZED),
    CN(220003, "CSR common name not set", Response.Status.BAD_REQUEST),
    O(220004, "CSR organization name not set", Response.Status.BAD_REQUEST),
    OU(220005, "CSR organization unit name not set", Response.Status.BAD_REQUEST),
    NOTFOUND(220006, "No cluster registered with the given organization name and organizational unit",
      Response.Status.BAD_REQUEST),
    SERIALNUMBER(220007, "Cluster has already a signed certificate", Response.Status.BAD_REQUEST),
    CNNOTFOUND(220008, "No cluster registered with the CSR common name", Response.Status.BAD_REQUEST),
    AGENTIDNOTFOUND(220009, "No cluster registered for the user", Response.Status.UNAUTHORIZED);
  
    private Integer code;
    private String message;
    private Response.StatusType respStatus;
  
    DelaCSRErrorCode(Integer code, String message, Response.StatusType respStatus) {
      this.code = code;
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
  
}
