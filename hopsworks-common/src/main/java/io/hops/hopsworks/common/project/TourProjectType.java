/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.common.project;

import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;

public enum TourProjectType {
  SPARK("spark", new ProjectServiceEnum[]{ProjectServiceEnum.JOBS}),
  KAFKA("kafka", new ProjectServiceEnum[]{ProjectServiceEnum.JOBS, ProjectServiceEnum.KAFKA}),
  TENSORFLOW("tensorflow", new ProjectServiceEnum[]{ProjectServiceEnum.JOBS});

  private final String tourName;
  private final ProjectServiceEnum[] activeServices;

  TourProjectType(String tourName, ProjectServiceEnum[] activeServices) {
    this.tourName = tourName;
    this.activeServices = activeServices;
  }

  public String getTourName() {
    return tourName;
  }

  public ProjectServiceEnum[] getActiveServices() {
    return activeServices;
  }
}
