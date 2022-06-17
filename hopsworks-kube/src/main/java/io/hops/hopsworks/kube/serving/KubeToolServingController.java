/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.hops.hopsworks.common.serving.ServingLogs;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.util.List;

public abstract class KubeToolServingController {
  abstract void createInstance(Project project, Users user, Serving serving) throws ServingException;
  abstract void updateInstance(Project project, Users user, Serving serving) throws ServingException;
  abstract void deleteInstance(Project project, Serving serving) throws ServingException;
  abstract KubeServingInternalStatus getInternalStatus(Project project, Serving serving) throws ServingException;
  abstract List<ServingLogs> getLogs(Project project, Serving serving, String component, Integer tailingLines);
}
