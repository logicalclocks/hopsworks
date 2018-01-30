package io.hops.hopsworks.kmon.cluster;

import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.ArrayList;

@ManagedBean
@RequestScoped
public class ClustersLayoutController {

  @EJB
  private HostServicesFacade hostServicesFacade;
  private static final Logger logger = Logger.getLogger(ClustersLayoutController.class.getName());
  private List<String> clusters;

  public ClustersLayoutController() {
  }

  @PostConstruct
  public void init() {
    logger.info("init ClustersLayoutController");
//      loadClusters();
  }

  public List<String> getClusters() {
    loadClusters();
    return clusters;
  }

  private void loadClusters() {
    clusters = hostServicesFacade.findClusters();
    if (clusters == null) {
      clusters = new ArrayList<String>();
    }
  }
}
