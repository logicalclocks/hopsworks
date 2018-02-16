/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
