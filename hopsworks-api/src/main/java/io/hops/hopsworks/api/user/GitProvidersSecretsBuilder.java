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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.git.util.GitCommandOperationUtil;
import io.hops.hopsworks.common.git.BasicAuthSecrets;
import io.hops.hopsworks.persistence.entity.git.config.GitProvider;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GitProvidersSecretsBuilder {
  private static final Logger LOGGER = Logger.getLogger(GitProvidersSecretsBuilder.class.getName());

  @EJB
  private GitCommandOperationUtil gitCommandOperationUtil;

  public URI uri(UriInfo uriInfo) {
    return uriInfo.getAbsolutePathBuilder()
        .build();
  }

  public GitProviderSecretsDTO build(UriInfo uriInfo, Users user) {
    GitProviderSecretsDTO dto = new GitProviderSecretsDTO();
    dto.setHref(uri(uriInfo));
    List<GitProviderSecretsDTO> dtos = new ArrayList<>();
    Arrays.stream(GitProvider.values()).forEach(provider -> {
      BasicAuthSecrets secrets = gitCommandOperationUtil.getAuthenticationSecrets(user, provider);
      if (!Strings.isNullOrEmpty(secrets.getUsername())) {
        dtos.add(new GitProviderSecretsDTO(provider, secrets.getUsername(), secrets.getPassword()));
      }
    });
    dto.setItems(dtos);
    dto.setCount((long) dtos.size());
    return dto;
  }
}
