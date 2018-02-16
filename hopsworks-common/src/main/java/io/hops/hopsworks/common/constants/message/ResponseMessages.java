/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.constants.message;

public class ResponseMessages {

  //response for validation error
  public final static String INTERNAL_SERVER_ERROR
          = "Internal Server Error. Please report a bug.";
  public final static String USER_DOES_NOT_EXIST = "User does not exist.";
  public final static String USER_WAS_NOT_FOUND
          = "Ops! The operation failed. User not found";
  public final static String USER_EXIST
          = "There is an existing account associated with this email";
  public final static String ACCOUNT_REQUEST
          = "Your account has not yet been approved.";
  public final static String ACCOUNT_DEACTIVATED
          = "This account have been deactivated.";
  public final static String ACCOUNT_VERIFICATION
          = "You need to verify your account.";
  public final static String ACCOUNT_BLOCKED
          = "Your account hsd been blocked. Contact the administrator.";
  public final static String AUTHENTICATION_FAILURE = "Authentication failed";
  public final static String NO_ROLE_FOUND = "No valid role found for this user";
  public final static String LOGOUT_FAILURE = "Logout failed on backend.";
  public final static String EMAIL_EMPTY = "Email cannot be empty.";
  public final static String EMAIL_INVALID = "Not a valid email address.";
  public final static String EMAIL_SENDING_FAILURE
          = "Could not send email to the given email address!";
  public final static String SEC_Q_EMPTY = "Security Question cannot be empty.";
  public final static String SEC_A_EMPTY = "Security Answer cannot be empty.";
  public final static String SEC_Q_NOT_IN_LIST
          = "Choose a Security Question from the list.";
  public final static String SEC_QA_INCORRECT
          = "Security question or answer did not match";
  public final static String PASSWORD_EMPTY = "Password cannot be empty.";
  public final static String PASSWORD_TOO_SHORT = "Password too short.";
  public final static String PASSWORD_TOO_LONG = "Password too long.";
  public final static String PASSWORD_INCORRECT = "Password incorrect";
  public final static String PASSWORD_PATTERN_NOT_CORRECT
          = "Password should include one uppercase letter,\n"
          + "one special character and/or alphanumeric characters.";
  public final static String INCORRECT_PASSWORD
          = "The password is incorrect. Please try again";
  public final static String PASSWORD_MISS_MATCH
          = "Passwords do not match - typo?";
  public final static String TOS_NOT_AGREED
          = "You must agree to our terms of use.";
  public final static String EMPTY_PATH
          = "Empty path requested";
  public static final String CERT_DOWNLOAD_DENIED
          = "Admin is not allowed to download certificates";
  

  //success response
  public final static String CREATED_ACCOUNT
          = "You have successfully created an account,\n"
          + "but you might need to wait until your account has been approved \n"
          + "before you can login.";
  public final static String PASSWORD_RESET_SUCCESSFUL
          = "Your password was successfully reset, your new password have been sent to your email.";
  public final static String PASSWORD_RESET_UNSUCCESSFUL
          = "Your password could not be reset. Please try again later or contact support.";
  public final static String PASSWORD_CHANGED
          = "Your password was successfully changed.";
  public final static String SEC_QA_CHANGED
          = "Your have successfully changed your security questions and answer.";
  public final static String PROFILE_UPDATED
          = "Your profile was updated successfully.";
  public final static String SSH_KEY_ADDED
          = "Your ssh key was added successfully.";
  public final static String SSH_KEY_REMOVED
          = "Your ssh key was deleted successfully.";
  public final static String SSH_KEYS_LISTED
          = "Your ssh keys were listed successfully.";
  public final static String NOTHING_TO_UPDATE
          = "Nothing to update";
  public final static String MASTER_ENCRYPTION_PASSWORD_CHANGE = "Master password change procedure started. Check " +
      "your inbox for final status";

  //project error response
  public final static String PROJECT_EXISTS
          = "Project with the same name already exists.";
  public final static String NUM_PROJECTS_LIMIT_REACHED
          = "You have reached the maximum number of projects you could create." +
            " Contact an administrator to increase your limit.";
  public final static String INVALID_PROJECT_NAME
          = "Invalid project name, valid characters: [a-z,0-9].";
  public final static String PROJECT_NOT_FOUND = "Project wasn't found.";
  public final static String PROJECT_NOT_ROOT_FOUND
          = "The project's root folder was not found in HDFS. You will not be unable to access its contents.";
  public final static String PROJECT_NOT_REMOVED = "Project wasn't removed.";
  public final static String PROJECT_NAME_EXIST
          = "A Project with the same name already exists in the system!";
  public final static String PROJECT_FOLDER_NOT_CREATED
          = "Project folder could not be created in HDFS.";
  public final static String STARTER_PROJECT_BAD_REQUEST
          = "Type of starter project is not valid";
  public final static String PROJECT_FOLDER_NOT_REMOVED
          = "Project folder could not be removed from HDFS.";
  public final static String PROJECT_REMOVAL_NOT_ALLOWED
          = "Project can only be deleted by its owner.";
  public final static String PROJECT_MEMBER_NOT_REMOVED
          = "Failed to remove team member.";
  public final static String MEMBER_REMOVAL_NOT_ALLOWED
          = "Your project role does not allow to remove other members of this project.";
  public final static String PROJECT_OWNER_NOT_ALLOWED
          = "Removing the project owner is not allowed.";
  public final static String PROJECT_OWNER_ROLE_NOT_ALLOWED
          = "Chaning the role of the project owner is not allowed.";
  public final static String FOLDER_INODE_NOT_CREATED
          = "Folder Inode could not be created in DB.";
  public final static String FOLDER_NAME_NOT_SET
          = "Name cannot be empty.";
  public final static String FOLDER_NAME_TOO_LONG
          = "Name cannot be longer than 88 characters.";
  public final static String FOLDER_NAME_CONTAIN_DISALLOWED_CHARS
          = "Name cannot contain any of the characters ";
  public final static String FOLDER_NAME_ENDS_WITH_DOT
          = "Name cannot end in a period.";
  public final static String FOLDER_NAME_EXIST
          = "A directory with the same name already exists. "
          + "If you want to replace it delete it first then try recreating.";
  public final static String FILE_NAME_EXIST
          = "File with the same name already exists.";
  public final static String FILE_NOT_FOUND = "File not found.";
  public final static String PROJECT_SERVICE_NOT_FOUND
          = " service was not found. ";
  public final static String NO_MEMBER_TO_ADD = " No member to add.";
  public final static String NO_MEMBER_ADD = " No member added.";
  public final static String TEAM_MEMBER_NOT_FOUND
          = " The selected user is not a team member in this project.";
  public final static String ROLE_NOT_SET = "Role cannot be empty.";
  public final static String PROJECT_NOT_SELECTED = "No project selected";
  public final static String QUOTA_NOT_FOUND = "Quota information not found.";
  public final static String QUOTA_REQUEST_NOT_COMPLETE = "Please specify both namespace and space quota.";
  public final static String QUOTA_ERROR = "Quota update error.";

  //project success messages
  public final static String PROJECT_CREATED = "Project created successfully.";
  public final static String PROJECT_DESCRIPTION_CHANGED
          = "Project description changed.";
  public final static String PROJECT_RETENTON_CHANGED
          = "Project retention period changed.";

  public final static String PROJECT_SERVICE_ADDED = "Project service added: ";
  public final static String PROJECT_SERVICE_ADD_FAILURE = "Failure adding service: ";
  public final static String PROJECT_REMOVED
          = "The project and all related files were removed successfully.";
  public final static String PROJECT_REMOVED_NOT_FOLDER
          = "The project was removed successfully. But its datasets have not been deleted.";
  public final static String PROJECT_MEMBER_REMOVED
          = "Member removed successfully";
  public final static String PROJECT_MEMBERS_ADDED
          = "Members added successfully";
  public final static String PROJECT_MEMBER_ADDED
          = "One member added successfully";
  public final static String MEMBER_ROLE_UPDATED = "Role updated successfully.";
  public final static String MEMBER_REMOVED_FROM_TEAM
          = "Member removed from team.";

  //DataSet
  public final static String DATASET_NAME_EMPTY
          = "DataSet name cannot be empty.";
  public final static String FILE_CORRUPTED_REMOVED_FROM_HDFS
          = "Corrupted file removed from hdfs.";
  public final static String DATASET_REMOVED_FROM_HDFS
          = "DataSet removed from hdfs.";
  public final static String SHARED_DATASET_REMOVED
          = "The shared dataset has been removed from this project.";
  public final static String DATASET_NOT_FOUND
          = "DataSet not found.";
  public final static String DATASET_ALREADY_PUBLIC
          = "DataSet is already public.";
  public final static String DATASET_NOT_PUBLIC
          = "DataSet is not public.";
  public final static String DATASET_NOT_EDITABLE
      = "DataSet is not editable.";
  public final static String DATASET_PENDING
          = "DataSet is not yet accessible. Accept the share request to access it.";
  public final static String PATH_NOT_FOUND
      = "Path not found";
  public final static String PATH_NOT_DIRECTORY
      = "Requested path is not a directory";
  public final static String PATH_IS_DIRECTORY
      = "Requested path is a directory";
  public final static String DOWNLOAD_ERROR
      = "You cannot download from a non public shared dataset";
  public final static String DOWNLOAD_PERMISSION_ERROR
      = "Your role does not allow to download this file";
  public final static String COMPRESS_ERROR
      = "You cannot compress files in a shared dataset";
  public final static String DATASET_OWNER_ERROR
      = "You cannot perform this action on a dataset you are not the owner";

  //Metadata
  public final static String TEMPLATE_INODEID_EMPTY
          = "The template id is empty";
  public final static String TEMPLATE_NOT_ATTACHED
          = "The template could not be attached to a file";
  public final static String UPLOAD_PATH_NOT_SPECIFIED
          = "The path to upload the template was not specified";
  public final static String ELASTIC_SERVER_NOT_FOUND
          = "Problem when reaching the Elasticsearch server";
  public final static String ELASTIC_INDEX_NOT_FOUND
          = "Elasticsearch indices do not exist";
  public final static String ELASTIC_TYPE_NOT_FOUND
          = "Elasticsearch type does not exist";
  public final static String ELASTIC_SERVER_NOT_AVAILABLE
          = "The Elasticsearch Server is either down or misconfigured.";

  //Workflow
  public final static String WORKFLOW_NOT_FOUND
          = "Workflow not found.";
  public final static String NODE_NOT_FOUND
          = "Node not found.";
  public final static String EDGE_NOT_FOUND
          = "Edge not found.";
  public final static String WOKFLOW_EXECUTION_NOT_FOUND
          = "Execution not found.";
  public final static String WOKFLOW_JOB_NOT_FOUND
          = "Job not found.";

  public final static String JOB_DETAILS
          = "Details for a job";

  // Services
  public final static String ZEPPELIN_ADD_FAILURE = "Failed to create Zeppelin notebook dir. "
      + "Zeppelin will not work properly. "
      + "Try recreating the following dir manually:";
  public final static String JUPYTER_ADD_FAILURE = "Failed to create Jupyter notebook dir. "
      + "Jupyter will not work properly. "
      + "Try recreating the following dir manually:";
  public final static String HIVE_ADD_FAILURE = "Failed to create the Hive database";

  // LLAP
  public final static String LLAP_STATUS_INVALID = "Unrecognized new LLAP status";
  public final static String LLAP_CLUSTER_ALREADY_UP = "LLAP cluster already up";
  public final static String LLAP_CLUSTER_ALREADY_DOWN = "LLAP cluster already down";

}
