/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.project;

import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.junit.Assert;
import org.junit.Test;

public class TestProjectFacade {

  @Test
  public void testNumberOfProjectLimitIsReached() {
    ProjectFacade pf = new ProjectFacade();
    Users user = new Users();
    user.setNumCreatedProjects(2);
    user.setMaxNumProjects(3);

    Assert.assertFalse(pf.numProjectsLimitReached(user));
    user.setNumCreatedProjects(3);
    Assert.assertTrue(pf.numProjectsLimitReached(user));

    user.setNumCreatedProjects(0);
    user.setMaxNumProjects(0);
    Assert.assertTrue(pf.numProjectsLimitReached(user));

    user.setNumActiveProjects(10);
    user.setMaxNumProjects(-1);
    Assert.assertFalse(pf.numProjectsLimitReached(user));
  }
}
