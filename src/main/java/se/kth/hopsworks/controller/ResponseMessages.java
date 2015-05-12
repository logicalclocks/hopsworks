/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
public class ResponseMessages {

  //response for validation error

  public final static String USER_DOES_NOT_EXIST = "User does not exist.";
  public final static String USER_WAS_NOT_FOUND
          = "Ops! The operation failed. User not found";
  public final static String USER_EXIST
          = "There is an existing account associated with this email";
  public final static String ACCOUNT_REQUEST
          = "Your request has not yet been acknowlegded.";
  public final static String ACCOUNT_DEACTIVATED
          = "This account have been deactivated.";
  public final static String ACCOUNT_VERIFICATION
          = "You need to verify your account.";
  public final static String ACCOUNT_BLOCKED = "Your account have been blocked.";
  public final static String AUTHENTICATION_FAILURE = "Authentication failed";
  public final static String LOGOUT_FAILURE = "Logout failed on backend";
  public final static String EMAIL_EMPTY = "Email can not be empty.";
  public final static String EMAIL_INVALID = "Not a valid email address.";
  public final static String EMAIL_SENDING_FAILURE
          = "Could not send email to the given email address!";
  public final static String SEC_Q_EMPTY = "Security Question can not be empty.";
  public final static String SEC_A_EMPTY = "Security Answer can not be empty.";
  public final static String SEC_Q_NOT_IN_LIST
          = "Choose Security Question from the list.";
  public final static String SEC_QA_INCORRECT
          = "Security question or answer did not match";
  public final static String PASSWORD_EMPTY = "Password can not be empty.";
  public final static String PASSWORD_TOO_SHORT = "Password too short.";
  public final static String PASSWORD_TOO_LONG = "Password too long.";
  public final static String PASSWORD_INCORRECT = "Password incorrect";
  public final static String PASSWORD_PATTERN_NOT_CORRECT
          = "Password should including one uppercase letter,\n"
          + "one special character and/or alphanumeric characters.";
  public final static String INCORRECT_PASSWORD
          = "The password is incorrect. Please try again";
  public final static String PASSWORD_MISS_MATCH
          = "Passwords do not match - typo?";
  public final static String TOS_NOT_AGREED
          = "You must agree to our terms of use.";

  //success response 
  public final static String CREATED_ACCOUNT
          = "You have successfully created an account,\n"
          + "but you might need to wait until your account is activated \n"
          + "before you can login.";
  public final static String PASSWORD_RESET_SUCCESSFUL
          = "Your password was successfully reset, your new password have been sent to your email.";
  public final static String PASSWORD_CHANGED
          = "Your password was successfully changed.";
  public final static String SEC_QA_CHANGED
          = "Your have successfully changed your security questions and answer.";
  public final static String PROFILE_UPDATED
          = "Your profile was updated successfully.";

  //project error response
  public final static String PROJECT_NOT_FOUND = "Project wasn't found.";
  public final static String PROJECT_NOT_ROOT_FOUND
          = "The project's root folder was not found in HDFS. You will not be unable to access its contents.";
  public final static String PROJECT_NOT_REMOVED = "Project wasn't removed.";
  public final static String PROJECT_NAME_EXIST
          = "A Project with this name already exists!";
  public final static String PROJECT_FOLDER_NOT_CREATED
          = "Project folder could not be created in HDFS.";
  public final static String PROJECT_FOLDER_NOT_REMOVED
          = "Project folder could not be removed from HDFS.";
  public final static String PROJECT_MEMBER_NOT_REMOVED
          = "Failed to remove team member.";
  public final static String PROJECT_INODE_NOT_CREATED
          = "Project Inode could not be created in DB.";
  public final static String PROJECT_NAME_NOT_SET
          = "The project name cannot be empty.";
  public final static String PROJECT_NAME_NOT_TOO_LONG
          = "The project name cannot be longer than 24 characters.";
  public final static String PROJECT_NAME_CONTAIN_DISALLOWED_CHARS
          = "The project name cannot contain any of the characters ";
  public final static String PROJECT_NAME_ENDS_WITH_DOT
          = "The project name cannot end in a period.";
  public final static String PROJECT_SERVICE_NOT_FOUND
          = " service was not found. ";
  public final static String NO_MEMBER_TO_ADD = " No member to add.";
  public final static String NO_MEMBER_ADD = " No member added.";
  public final static String TEAM_MEMBER_NOT_FOUND
          = " The selected user is not a team member in this project.";
  public final static String ROLE_NOT_SET = "Role can not be empty.";

  //project success messages
  public final static String PROJECT_CREATED = "Project created successfully.";
  public final static String PROJECT_DESCRIPTION_CHANGED = "Project description changed.";
  public final static String PROJECT_SERVICE_ADDED = "Project service added";
  public final static String PROJECT_REMOVED
          = "The project and all related files were removed successfully.";
  public final static String PROJECT_REMOVED_NOT_FOLDER
          = "The project was removed successfully. But failed to remove project files.";
  public final static String PROJECT_MEMBER_REMOVED
          = "Member removed successfully";
  public final static String PROJECT_MEMBERS_ADDED
          = "Members added successfully";
  public final static String PROJECT_MEMBER_ADDED
          = "One member added successfully";
  public final static String MEMBER_ROLE_UPDATED = "Role updated successfully.";
  public final static String MEMBER_REMOVED_FROM_TEAM
          = "Member removed from team.";
}
