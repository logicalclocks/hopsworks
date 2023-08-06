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
package io.hops.hopsworks.common.airflow;

import freemarker.template.TemplateException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.TemplateEngine;
import io.hops.hopsworks.common.util.templates.airflow.AirflowDAG;
import io.hops.hopsworks.common.util.templates.airflow.AirflowJobLaunchOperator;
import io.hops.hopsworks.common.util.templates.airflow.AirflowJobSuccessSensor;
import io.hops.hopsworks.exceptions.AirflowException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.AclEntryScope;
import org.apache.hadoop.fs.permission.AclEntryType;
import org.apache.hadoop.fs.permission.FsAction;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AirflowController {

  @EJB
  private DistributedFsService dfs;
  @EJB
  private TemplateEngine templateEngine;
  @EJB
  private Settings settings;

  public void composeDAG(Project project, Users user, AirflowDagDTO dagDefinition) throws AirflowException {
    AirflowDAG dag = AirflowDagDTO.toAirflowDagTemplate(dagDefinition, user, project);

    Map<String, Object> dataModel = new HashMap<>(4);
    dataModel.put(AirflowJobLaunchOperator.class.getSimpleName(), AirflowJobLaunchOperator.class);
    dataModel.put(AirflowJobSuccessSensor.class.getSimpleName(), AirflowJobSuccessSensor.class);
    dataModel.put("dag", dag);
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps(project, user);
      StringWriter stringWriter = new StringWriter();
      String dagStr = templateEngine.template(dataModel, stringWriter, AirflowDAG.TEMPLATE_NAME);
      Path dagPath = new Path(Utils.getProjectPath(project.getName()) +
          Settings.ServiceDataset.AIRFLOW.getName() + File.separator + dagDefinition.getName() + ".py");
      dfso.create(dagPath, dagStr);
    } catch (IOException | TemplateException ex) {
      throw new AirflowException(RESTCodes.AirflowErrorCode.DAG_NOT_TEMPLATED, Level.SEVERE,
          "Could not template DAG file for Project " + project.getName(),
          ex.getMessage(), ex);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }

  public void grantAirflowPermissions(Project project, DistributedFileSystemOps udfso) throws ProjectException {
    try {
      final String airflowUser = settings.getAirflowUser();
      List<AclEntry> aclEntries = new ArrayList<>();
      AclEntry accessAcl = new AclEntry.Builder()
          .setType(AclEntryType.USER)
          .setName(airflowUser)
          .setScope(AclEntryScope.ACCESS)
          .setPermission(FsAction.READ_EXECUTE)
          .build();
      AclEntry defaultAcl = new AclEntry.Builder()
          .setType(AclEntryType.USER)
          .setName(airflowUser)
          .setScope(AclEntryScope.DEFAULT)
          .setPermission(FsAction.READ_EXECUTE)
          .build();
      aclEntries.add(accessAcl);
      aclEntries.add(defaultAcl);
      udfso.updateAcls(new Path(Utils.getProjectPath(project.getName())
              + Settings.ServiceDataset.AIRFLOW.getName()), aclEntries);
    } catch (IOException ex) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_SET_PERMISSIONS_ERROR, Level.SEVERE,
          "Failed to set permissions to airflow user on the Airflow dataset", ex.getMessage(), ex);
    }
  }
}
