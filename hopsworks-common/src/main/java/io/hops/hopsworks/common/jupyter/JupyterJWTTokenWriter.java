/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.jupyter;

import com.google.common.collect.ImmutableSet;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Set;

public interface JupyterJWTTokenWriter {
  
  Set<PosixFilePermission> TOKEN_FILE_PERMISSIONS = ImmutableSet.of(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE,
    PosixFilePermission.OWNER_EXECUTE,
    PosixFilePermission.GROUP_READ,
    PosixFilePermission.GROUP_WRITE,
    PosixFilePermission.GROUP_EXECUTE);
  
  String readToken(Project project, Users user) throws IOException;
  
  default void writeToken(Settings settings, JupyterJWT jupyterJWT) throws IOException {
    FileUtils.writeStringToFile(jupyterJWT.tokenFile.toFile(), jupyterJWT.token);
    
    String groupName = settings.getJupyterGroup();
    UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
    GroupPrincipal group = lookupService.lookupPrincipalByGroupName(groupName);
    Files.getFileAttributeView(jupyterJWT.tokenFile, PosixFileAttributeView.class,
      LinkOption.NOFOLLOW_LINKS).setGroup(group);
    
    Files.setPosixFilePermissions(jupyterJWT.tokenFile, TOKEN_FILE_PERMISSIONS);
  }

  default void deleteToken(JupyterJWT jupyterJWT) {
    FileUtils.deleteQuietly(jupyterJWT.tokenFile.toFile());
  }
}
