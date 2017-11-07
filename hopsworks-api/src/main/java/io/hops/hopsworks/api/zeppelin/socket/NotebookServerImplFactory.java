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
      impl = new NotebookServerImpl(project, zeppelinConfigFactory, certsFacade, settings);
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
