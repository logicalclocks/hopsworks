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
package io.hops.hopsworks.common.elastic;


import io.hops.hopsworks.common.dao.project.team.ProjectRoleTypes;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.restutils.RESTCodes;

import java.util.logging.Level;

public class ElasticUtils {
  
  public static String getKibanaTenantIndex(String project){
    // https://github.com/opendistro-for-elasticsearch/security-advanced
    // -modules/blob/v1.2.0.0/src/main/java/com/amazon/opendistroforelasticsearch
    // /security/configuration/PrivilegesInterceptorImpl.java#L276
    return Settings.KIBANA_INDEX_PREFIX + "_" + project.hashCode() + "_"
        + getProjectNameWithNoSpecialCharacters(project);
  }
  
  public static String getAllKibanaTenantIndex(String project){
    return getKibanaTenantIndex(project) + "*";
  }
  
  public static String getProjectNameWithNoSpecialCharacters(String project){
    return project.toLowerCase().replaceAll("[^a-z0-9]+", "");
  }
  
  public static String getValidRole(String role) throws ElasticException {
    if(role != null &&
        (role.equals(ProjectRoleTypes.DATA_OWNER.getRole())
            || role.equals(ProjectRoleTypes.DATA_SCIENTIST.getRole()))){
      return role.trim().toLowerCase().replace(" ", "_");
    }
    throw new ElasticException(RESTCodes.ElasticErrorCode.INVALID_ELASTIC_ROLE,
        Level.SEVERE, "Invalid security role : " + role);
  }
}
