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

package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;

import java.io.IOException;

public interface CertificateHandler {
  void generate(Project project, Users user, UserCerts userCerts) throws IOException;
  void revoke(Project project, Users user) throws IOException;

  String getClassName();
}
