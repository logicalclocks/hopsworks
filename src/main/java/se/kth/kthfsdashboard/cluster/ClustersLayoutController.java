package se.kth.kthfsdashboard.cluster;

import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.RoleEJB;

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
      loadClusters();
   }

   public List<String> getClusters() {
      return clusters;
   }

   private void loadClusters() {
      clusters = roleEjb.findClusters();      
   }
}