package se.kth.kthfsdashboard.service;

import com.sun.jersey.api.client.ClientResponse;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.ws.rs.core.Response;
import se.kth.kthfsdashboard.communication.WebCommunication;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.role.RoleType;
import se.kth.kthfsdashboard.struct.Status;
import se.kth.kthfsdashboard.utils.FormatUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class TerminalController {

   @ManagedProperty("#{param.cluster}")
   private String cluster;
   @ManagedProperty("#{param.role}")
   private String role;
   @ManagedProperty("#{param.service}")
   private String service;
   @EJB
   private HostEJB hostEjb;
   private static final Logger logger = Logger.getLogger(TerminalController.class.getName());

   public TerminalController() {
   }

   @PostConstruct
   public void init() {
      logger.info("init TerminalController");
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

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getCluster() {
      return cluster;
   }

   public String handleCommand(String command, String[] params) {
      if (service.equals(ServiceType.KTHFS.toString())) {
         if (!command.equals("hdfs")) {
            return "Invalid command. Accepted commands are: hdfs";
         } else if (command.contains(";")) {
            return "Invalid character ;";
         } else {
            WebCommunication web = new WebCommunication();
            try {
//             TODO: get only one datanode
               List<Host> liveDatanodes = hostEjb.find(cluster, service, RoleType.datanode.toString(), Status.Started);
               if (liveDatanodes.isEmpty()) {
                  throw new RuntimeException("No live datanode available.");
               }
               String address = liveDatanodes.get(0).getPublicOrPrivateIp();
               return web.execute(address, cluster, service, RoleType.datanode.toString(), command, params);
            } catch (Exception ex) {
               logger.log(Level.SEVERE, null, ex);
               return "Error: Could not contact a KTHFS node";
            }
         }
      }
      return null;
   }
}