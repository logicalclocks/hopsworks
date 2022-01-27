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
package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.git.config.GitProvider;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GitProviderSecretsDTO extends RestDTO<GitProviderSecretsDTO> {
  private GitProvider gitProvider;
  private String username;
  private String token;

  public GitProviderSecretsDTO() {}

  public GitProviderSecretsDTO(GitProvider gitProvider, String username, String token) {
    this.gitProvider = gitProvider;
    this.username = username;
    this.token = token;
  }

  public GitProvider getGitProvider() { return gitProvider; }

  public void setGitProvider(GitProvider gitProvider) { this.gitProvider = gitProvider; }

  public String getUsername() { return username; }

  public void setUsername(String username) { this.username = username; }

  public String getToken() { return token; }

  public void setToken(String token) { this.token = token; }
}
