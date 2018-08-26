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

package io.hops.hopsworks.common.serving.tf;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.dao.user.Users;

import java.util.List;

public interface TfServingController {

  List<TfServingWrapper> getTfServings(Project project) throws TfServingException;

  TfServingWrapper getTfServing(Project project, Integer id) throws TfServingException;

  void deleteTfServing(Project project, Integer id) throws TfServingException;

  void deleteTfServings(Project project) throws TfServingException;

  void createOrUpdate(Project project, Users user, TfServing newTfServing) throws TfServingException;

  void startOrStop(Project project, Users user, Integer tfServingId, TfServingCommands command)
      throws TfServingException;

  int getMaxNumInstances();

  String getClassName();
}
