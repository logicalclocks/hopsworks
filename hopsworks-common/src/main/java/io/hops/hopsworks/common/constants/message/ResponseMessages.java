/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.constants.message;

public class ResponseMessages {
  public final static String PASSWORD_RESET_SUCCESSFUL
          = "Your password was successfully reset, your new password have been sent to your email.";
  public final static String PASSWORD_CHANGED
          = "Your password was successfully changed.";
  public final static String SEC_QA_CHANGED
          = "Your have successfully changed your security questions and answer.";
  public final static String PROFILE_UPDATED
          = "Your profile was updated successfully.";
  public final static String SSH_KEY_REMOVED
          = "Your ssh key was deleted successfully.";
  public final static String NOTHING_TO_UPDATE
          = "Nothing to update";
  public final static String MASTER_ENCRYPTION_PASSWORD_CHANGE = "Master password change procedure started. Check " +
      "your inbox for final status";

  //project error response
  public final static String PROJECT_NOT_FOUND = "Project wasn't found.";
  public final static String FILE_NOT_FOUND = "File not found.";
  public final static String NO_MEMBER_ADD = " No member added.";

  //project success messages
  public final static String PROJECT_CREATED = "Project created successfully.";
  public final static String PROJECT_DESCRIPTION_CHANGED
          = "Project description changed.";
  public final static String PROJECT_RETENTON_CHANGED
          = "Project retention period changed.";

  public final static String PROJECT_SERVICE_ADDED = "Project service added: ";
  public final static String PROJECT_REMOVED
          = "The project and all related files were removed successfully.";
  public final static String PROJECT_MEMBERS_ADDED
          = "Members added successfully";
  public final static String PROJECT_MEMBER_ADDED
          = "One member added successfully";
  public final static String MEMBER_ROLE_UPDATED = "Role updated successfully.";
  public final static String MEMBER_REMOVED_FROM_TEAM
          = "Member removed from team.";

  //DataSet
  public final static String FILE_CORRUPTED_REMOVED_FROM_HDFS
          = "Corrupted file removed from hdfs.";
  public final static String DATASET_REMOVED_FROM_HDFS
          = "DataSet removed from hdfs.";
  public final static String SHARED_DATASET_REMOVED
          = "The shared dataset has been removed from this project.";
  public final static String DOWNLOAD_PERMISSION_ERROR
      = "Your role does not allow to download this file";

  public final static String JOB_DETAILS
          = "Details for a job";
}
