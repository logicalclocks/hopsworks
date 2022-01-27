/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.git.util;

public class Constants {
  //GITLAB
  public static final String GITLAB_TOKEN_SECRET_NAME = "gitlab_token";
  public static final String GITLAB_USERNAME_SECRET_NAME = "gitlab_username";

  //GITHUB
  public static final String GITHUB_TOKEN_SECRET_NAME = "github_token";
  public static final String GITHUB_USERNAME_SECRET_NAME = "github_username";

  //BITBUCKET
  public static final String BITBUCKET_TOKEN_SECRET_NAME = "bitbucket_token";
  public static final String BITBUCKET_USERNAME_SECRET_NAME = "bitbucket_username";

  public static final String REPOSITORY_DEFAULT_REMOTE_NAME = "origin";
}
