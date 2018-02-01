/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.zeppelin.socket;

import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import io.hops.hopsworks.api.zeppelin.server.ZeppelinConfigFactory;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.websocket.Session;

import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.Settings;
import org.sonatype.aether.RepositoryException;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class NotebookServerImplFactory {

  @EJB
  private ProjectFacade projectBean;
  @EJB
  private ZeppelinConfigFactory zeppelinConfigFactory;
  @EJB
  private Settings settings;
  @EJB
  private CertsFacade certsFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  
  private Map<String, NotebookServerImpl> notebookServerImpls = new HashMap<>();

  public NotebookServerImpl getNotebookServerImpl(String projectName){
    return notebookServerImpls.get(projectName);
  }
  
  public NotebookServerImpl getNotebookServerImps(String projectName, Session session) throws IOException,
      RepositoryException,
      TaskRunnerException {
    Project project = projectBean.findByName(projectName);
    if (project == null) {
      return null;
    }
    NotebookServerImpl impl = notebookServerImpls.get(projectName);
    if (impl == null) {
      impl = new NotebookServerImpl(project, zeppelinConfigFactory, certsFacade,
          settings, projectTeamFacade, activityFacade, certificatesMgmService);
      notebookServerImpls.put(projectName, impl);
    }
    impl.addConnectedSocket(session);
    return impl;
  }

  public void removeNotebookServerImpl(String projectName) {
    if (notebookServerImpls.get(projectName) != null && 
        notebookServerImpls.get(projectName).connectedSocketsIsEmpty()) {
      notebookServerImpls.remove(projectName);
      zeppelinConfigFactory.removeFromCache(projectName);
    }
  }
}
