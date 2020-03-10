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

package io.hops.hopsworks.common.serving;

import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.serving.util.ServingCommands;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.UserException;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Interface for managing serving instances. Different type of serving controllers e.g (localhost or Kubernetes) should
 * implement this interface.
 */
public interface ServingController {

  List<ServingWrapper> getServings(Project project)
      throws ServingException, KafkaException, CryptoPasswordNotFoundException;

  ServingWrapper getServing(Project project, Integer id)
      throws ServingException, KafkaException, CryptoPasswordNotFoundException;

  void deleteServing(Project project, Integer id) throws ServingException;

  void deleteServings(Project project) throws ServingException;

  void startOrStop(Project project, Users user, Integer servingId, ServingCommands command)
      throws ServingException;

  void createOrUpdate(Project project, Users user, ServingWrapper newServing)
      throws KafkaException, UserException, ProjectException, ServiceException, ServingException,
    InterruptedException, ExecutionException;

  int getMaxNumInstances();

  String getClassName();
}
