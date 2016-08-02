package io.hops.kmon.cluster;

import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import io.hops.kmon.role.RoleEJB;
import java.util.ArrayList;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class ClustersLayoutController {

  @EJB
  private RoleEJB roleEjb;
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
    clusters = roleEjb.findClusters();
    if (clusters == null) {
      clusters = new ArrayList<String>();
    }
  }
}
