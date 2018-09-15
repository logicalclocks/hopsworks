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
 * 1. All error codes must be a total of 6 digits, first two indicate error type and last 2 indicate error id.
 * 2. Service error codes start  with "10".
 * 3. Dataset error codes start  with "11".
 * 4. Generic error codes start  with "12".
 * 5. Job error codes start  with "13".
 * 6. Request error codes start  with "14".
 * 7. Project error codes start  with "15".
 * 8. Security error codes start  with "16".
 * 9. Dela error codes start with "17"
 * 10. Template error codes start with "18"
 */
@XmlRootElement
public class RESTCodes {
  
  public interface RESTErrorCode {
    Response.Status getRespStatus();
    
    Integer getCode();
    
    String getMessage();
  }
  
  public enum ProjectErrorCode implements RESTErrorCode {
    
    //project error response
    NO_ROLE_FOUND(150000, "No valid role found for this user", Response.Status.BAD_REQUEST),
    PROJECT_EXISTS(150001, "Project with the same name already exists.", Response.Status.BAD_REQUEST),
    NUM_PROJECTS_LIMIT_REACHED(150002, "You have reached the maximum number of projects you could create."
      + " Contact an administrator to increase your limit.", Response.Status.BAD_REQUEST),
    INVALID_PROJECT_NAME(150003, "Invalid project name, valid characters: [a-z,0-9].", Response.Status.BAD_REQUEST),
    PROJECT_NOT_FOUND(150004, "Project wasn't found.", Response.Status.BAD_REQUEST),
    PROJECT_NOT_REMOVED(150005, "Project wasn't removed.", Response.Status.BAD_REQUEST),
    PROJECT_NAME_EXIST(150006, "A Project with the same name already exists in the system!",
      Response.Status.BAD_REQUEST),
    PROJECT_FOLDER_NOT_CREATED(150007, "Project folder could not be created in HDFS.", Response.Status.BAD_REQUEST),
    STARTER_PROJECT_BAD_REQUEST(150008, "Type of starter project is not valid", Response.Status.BAD_REQUEST),
    PROJECT_FOLDER_NOT_REMOVED(150009, "Project folder could not be removed from HDFS.", Response.Status.BAD_REQUEST),
    PROJECT_REMOVAL_NOT_ALLOWED(150010, "Project can only be deleted by its owner.", Response.Status.BAD_REQUEST),
    PROJECT_MEMBER_NOT_REMOVED(150011, "Failed to remove team member.", Response.Status.BAD_REQUEST),
    MEMBER_REMOVAL_NOT_ALLOWED(150012, "Your project role does not allow to remove other members of this project.",
      Response.Status.BAD_REQUEST),
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
    PROJECT_QUOTA_ERROR(150029, "This project is out of credits.", Response.Status.BAD_REQUEST),
    
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
    MEMBER_REMOVED_FROM_TEAM(150041, "Member removed from team.", Response.Status.OK);
    
    private Integer code;
    private String message;
    private Response.Status respStatus;
    
    ProjectErrorCode(Integer code, String message, Response.Status respStatus) {
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
    public Response.Status getRespStatus() {
      return respStatus;
    }
    
    public void setCode(Integer code) {
      this.code = code;
    }
    
    public void setMessage(String message) {
      this.message = message;
    }
    
    public void setRespStatus(Response.Status respStatus) {
      this.respStatus = respStatus;
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
    COMPRESSION_ERROR(110024, "Error while performing a (un)compression operation",
      Response.Status.INTERNAL_SERVER_ERROR),
    DATASET_OWNER_ERROR(110025, "You cannot perform this action on a dataset you are not the owner",
      Response.Status.BAD_REQUEST),
    DATASET_PUBLIC_IMMUTABLE(110026, "Public datasets are immutable.", Response.Status.BAD_REQUEST),
    DATASET_NAME_INVALID(110028, "Name of dir is invalid", Response.Status.BAD_REQUEST),
    IMAGE_SIZE_INVALID(110029, "Image is too big to display please download it by double-clicking it instead",
      Response.Status.BAD_REQUEST),
    FILE_PREVIEW_ERROR(110030, "Error while retrieving file content for preview", Response.Status.BAD_REQUEST),
    DATASET_PARAMETERS_INVALID(110031, "Invalid parameters for requested dataset operation",
      Response.Status.BAD_REQUEST),
    EMPTY_PATH(110032, "Empty path requested", Response.Status.BAD_REQUEST),
    
    UPLOAD_PATH_NOT_SPECIFIED(110035, "The path to upload the template was not specified",
      Response.Status.BAD_REQUEST);
    
    
    private Integer code;
    private String message;
    private Response.Status respStatus;
    
    DatasetErrorCode(Integer code, String message, Response.Status respStatus) {
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
    public Response.Status getRespStatus() {
      return respStatus;
    }
    
    public void setCode(Integer code) {
      this.code = code;
    }
    
    public void setMessage(String message) {
      this.message = message;
    }
    
    public void setRespStatus(Response.Status respStatus) {
      this.respStatus = respStatus;
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
    private Response.Status respStatus;
  
    MetadataErrorCode(Integer code, String message, Response.Status respStatus) {
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
    public Response.Status getRespStatus() {
      return respStatus;
    }
    
    public void setCode(Integer code) {
      this.code = code;
    }
    
    public void setMessage(String message) {
      this.message = message;
    }
    
    public void setRespStatus(Response.Status respStatus) {
      this.respStatus = respStatus;
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
    
    JOB_SCHEDULE_UPDATE(130021, "Could not update schedule.", Response.Status.INTERNAL_SERVER_ERROR);
    
    private Integer code;
    private String message;
    private Response.Status respStatus;
    
    JobErrorCode(Integer code, String message, Response.Status respStatus) {
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
    public Response.Status getRespStatus() {
      return respStatus;
    }
    
    public void setCode(Integer code) {
      this.code = code;
    }
    
    public void setMessage(String message) {
      this.message = message;
    }
    
    public void setRespStatus(Response.Status respStatus) {
      this.respStatus = respStatus;
    }
    
  }
  
  public enum RequestErrorCode implements RESTErrorCode {
    
    // JOBS Error Messages
    EMAIL_EMPTY(140001, "Email cannot be empty.", Response.Status.BAD_REQUEST),
    EMAIL_INVALID(140002, "Not a valid email address.", Response.Status.BAD_REQUEST),
    EMAIL_SENDING_FAILURE(140003, "Could not send email", Response.Status.BAD_REQUEST),
    DATASET_REQUEST_ERROR(140004, "Error while submitting dataset request", Response.Status.BAD_REQUEST),
    REQUEST_UNKNOWN_ACTION(140005, "Unknown request action", Response.Status.BAD_REQUEST);
    
    
    private Integer code;
    private String message;
    private Response.Status respStatus;
    
    RequestErrorCode(Integer code, String message, Response.Status respStatus) {
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
    public Response.Status getRespStatus() {
      return respStatus;
    }
    
    public void setCode(Integer code) {
      this.code = code;
    }
    
    public void setMessage(String message) {
      this.message = message;
    }
    
    public void setRespStatus(Response.Status respStatus) {
      this.respStatus = respStatus;
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
    ELASTIC_SERVER_NOT_FOUND(100003, "Problem when reaching the Elasticsearch server", Response.Status.BAD_REQUEST),
    
    //Hive
    HIVE_ADD_FAILURE(100004, "Failed to create the Hive database", Response.Status.BAD_REQUEST),
    // LLAP
    LLAP_STATUS_INVALID(100005, "Unrecognized new LLAP status", Response.Status.BAD_REQUEST),
    LLAP_CLUSTER_ALREADY_UP(100006, "LLAP cluster already up", Response.Status.BAD_REQUEST),
    LLAP_CLUSTER_ALREADY_DOWN(100007, "LLAP cluster already down", Response.Status.BAD_REQUEST),
    
    //Database
    DATABASE_UNAVAILABLE(100008, "The database is temporarily unavailable. Please try again later",
      Response.Status.SERVICE_UNAVAILABLE),
    
    TENSORBOARD_CLEANUP_ERROR(100009, "Could not delete tensorboard", Response.Status.INTERNAL_SERVER_ERROR);
    
    private Integer code;
    private String message;
    private Response.Status respStatus;
    
    ServiceErrorCode(Integer code, String message, Response.Status respStatus) {
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
    public Response.Status getRespStatus() {
      return respStatus;
    }
    
    public void setCode(Integer code) {
      this.code = code;
    }
    
    public void setMessage(String message) {
      this.message = message;
    }
    
    public void setRespStatus(Response.Status respStatus) {
      this.respStatus = respStatus;
    }
    
  }
  
  public enum GenericErrorCode implements RESTErrorCode {
    
    UNKNOWN_ERROR(120000, "A generic error occured.", Response.Status.INTERNAL_SERVER_ERROR),
    ILLEGAL_ARGUMENT(120001, "A wrong argument was provided.", Response.Status.EXPECTATION_FAILED),
    ILLEGAL_STATE(120002, "A runtime error occurred.", Response.Status.EXPECTATION_FAILED),
    ROLLBACK(120003, "The last transaction did not complete as expected", Response.Status.INTERNAL_SERVER_ERROR),
    WEBAPPLICATION(120004, "Web application exception occurred", null),
    PERSISTENCE_ERROR(120005, "Persistence error occured", Response.Status.INTERNAL_SERVER_ERROR),
    UNKNOWN_ACTION(120006, "This action can not be applied on this resource.", Response.Status.BAD_REQUEST),
    INCOMPLETE_REQUEST(120007, "Some parameters were not provided or were not in the required format.",
      Response.Status.BAD_REQUEST),
    SECURITY_EXCEPTION(120008, "A Java security error occurred.", Response.Status.INTERNAL_SERVER_ERROR);
    
    private Integer code;
    private String message;
    private Response.Status respStatus;
    
    GenericErrorCode(Integer code, String message, Response.Status respStatus) {
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
    public Response.Status getRespStatus() {
      return respStatus;
    }
    
    public void setCode(Integer code) {
      this.code = code;
    }
    
    public void setMessage(String message) {
      this.message = message;
    }
    
    public void setRespStatus(Response.Status respStatus) {
      this.respStatus = respStatus;
    }
    
  }
  
  
  public enum SecurityErrorCode implements RESTErrorCode {
    
    //response for validation error
    INTERNAL_SERVER_ERROR(160000, "Internal Server Error. Please report a bug.", Response.Status.BAD_REQUEST),
    USER_DOES_NOT_EXIST(160001, "User does not exist.", Response.Status.BAD_REQUEST),
    USER_WAS_NOT_FOUND(160002, "Ops! The operation failed. User not found", Response.Status.NOT_FOUND),
    USER_EXISTS(160003, "There is an existing account associated with this email", Response.Status.BAD_REQUEST),
    ACCOUNT_REQUEST(160004, "Your account has not yet been approved.", Response.Status.BAD_REQUEST),
    ACCOUNT_DEACTIVATED(160005, "This account have been deactivated.", Response.Status.BAD_REQUEST),
    ACCOUNT_VERIFICATION(160006, "You need to verify your account.", Response.Status.BAD_REQUEST),
    ACCOUNT_BLOCKED(160007, "Your account hsd been blocked. Contact the administrator.", Response.Status.BAD_REQUEST),
    AUTHENTICATION_FAILURE(160008, "Authentication failed", Response.Status.BAD_REQUEST),
    LOGOUT_FAILURE(160009, "Logout failed on backend.", Response.Status.BAD_REQUEST),
    
    SEC_Q_EMPTY(160010, "Security Question cannot be empty.", Response.Status.BAD_REQUEST),
    SEC_A_EMPTY(160011, "Security Answer cannot be empty.", Response.Status.BAD_REQUEST),
    SEC_Q_NOT_IN_LIST(160011, "Choose a Security Question from the list.", Response.Status.BAD_REQUEST),
    SEC_QA_INCORRECT(160012, "Security question or answer did not match", Response.Status.BAD_REQUEST),
    PASSWORD_EMPTY(160013, "Password cannot be empty.", Response.Status.BAD_REQUEST),
    PASSWORD_TOO_SHORT(160014, "Password too short.", Response.Status.BAD_REQUEST),
    PASSWORD_TOO_LONG(160015, "Password too long.", Response.Status.BAD_REQUEST),
    PASSWORD_INCORRECT(160016, "Password incorrect", Response.Status.BAD_REQUEST),
    PASSWORD_PATTERN_NOT_CORRECT(160017, "Password should include one uppercase letter\n"
      + "one special character and/or alphanumeric characters.", Response.Status.BAD_REQUEST),
    INCORRECT_PASSWORD(160018, "The password is incorrect. Please try again", Response.Status.BAD_REQUEST),
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
    MASTER_ENCRYPTION_PASSWORD_CHANGE(160030,
      "Master password change procedure started. Check your inbox for final status", Response.Status.BAD_REQUEST),
    HDFS_ACCESS_CONTROL(160031, "Access error while trying to access hdfs resource", Response.Status.FORBIDDEN),
    EJB_ACCESS_LOCAL(160032, "EJB access local error", Response.Status.UNAUTHORIZED),
    AUTHORIZATION_FAILURE(160033, "Authorization failed", Response.Status.BAD_REQUEST);
  
  
    private Integer code;
    private String message;
    private Response.Status respStatus;
    
    SecurityErrorCode() {
    }
    
    SecurityErrorCode(Integer code, String message, Response.Status respStatus) {
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
    
    public Response.Status getRespStatus() {
      return respStatus;
    }
    
    public void setCode(Integer code) {
      this.code = code;
    }
    
    public void setMessage(String message) {
      this.message = message;
    }
    
    public void setRespStatus(Response.Status respStatus) {
      this.respStatus = respStatus;
    }
    
  }
  
  public enum DelaErrorCode implements RESTErrorCode {
    
    //response for validation error
    THIRD_PARTY_ERROR(170000, null, Response.Status.EXPECTATION_FAILED);
    
    
    private Integer code;
    private String message;
    private Response.Status respStatus;
    
    DelaErrorCode() {
    }
    
    DelaErrorCode(Integer code, String message, Response.Status respStatus) {
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
    
    public Response.Status getRespStatus() {
      return respStatus;
    }
    
    public void setCode(Integer code) {
      this.code = code;
    }
    
    public void setMessage(String message) {
      this.message = message;
    }
    
    public void setRespStatus(Response.Status respStatus) {
      this.respStatus = respStatus;
    }
    
    
  }
  
}
