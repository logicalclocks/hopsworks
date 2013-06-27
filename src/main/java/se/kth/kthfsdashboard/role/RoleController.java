package se.kth.kthfsdashboard.role;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.struct.InstanceFullInfo;
import se.kth.kthfsdashboard.util.Formatter;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class RoleController {

   @EJB
   private HostEJB hostEJB;
   @EJB
   private RoleEJB roleEjb;
   @ManagedProperty("#{param.hostid}")
   private String hostId;
   @ManagedProperty("#{param.role}")
   private String role;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   private List<InstanceFullInfo> instanceInfoList;
   private String health;
   private boolean renderWebUi;
   private static final Logger logger = Logger.getLogger(RoleController.class.getName());

   public RoleController() {
   }

   @PostConstruct
   public void init() {
      logger.info("init RoleController");
      instanceInfoList = new ArrayList<InstanceFullInfo>();
      try {
         Role r = roleEjb.find(hostId, cluster, service, role);
         Host h = hostEJB.findHostById(hostId);
         String ip = h.getPublicIp();
         InstanceFullInfo info = new InstanceFullInfo(r.getCluster(), 
                 r.getService(), r.getRole(), r.getHostId(), ip, 
                 r.getWebPort(), r.getStatus(), r.getHealth().toString());
         info.setPid(r.getPid());
         info.setUptime(Formatter.time(r.getUptime() * 1000));
         instanceInfoList.add(info);
         renderWebUi = r.getWebPort() != null && r.getWebPort() != 0;
         health = r.getHealth().toString();
      } catch (Exception ex) {
         logger.warning("init: ".concat(ex.getMessage()));
      }
   }

   public String getHealth() {
      return health;
   }

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }

   public String getService() {
      return service;
   }

   public void setService(String service) {
      this.service = service;
   }

   public String getHostId() {
      return hostId;
   }

   public void setHostId(String hostId) {
      this.hostId = hostId;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getCluster() {
      return cluster;
   }
   
   public boolean getRenderWebUi(){
      return renderWebUi;
   }

   public List<InstanceFullInfo> getInstanceFullInfo() {
      return instanceInfoList;
   }
   
}
