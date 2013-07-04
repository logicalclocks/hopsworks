package se.kth.kthfsdashboard.host;

import com.sun.jersey.api.client.ClientResponse;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.ws.rs.core.Response.Status.Family;
import org.codehaus.jettison.json.JSONObject;
import se.kth.kthfsdashboard.command.Command;
import se.kth.kthfsdashboard.command.CommandEJB;
import se.kth.kthfsdashboard.communication.WebCommunication;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.struct.Status;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class HostController implements Serializable {

   @EJB
   private HostEJB hostEJB;
   @EJB
   private RoleEJB roleEjb;
   @EJB
   private CommandEJB commandEJB;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   @ManagedProperty("#{param.hostid}")
   private String hostId;
   @ManagedProperty("#{param.command}")
   private String command;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.role}")
   private String role;
   private Host host;
   private boolean found;
   private static final Logger logger = Logger.getLogger(HostController.class.getName());

   public HostController() {
   }

   @PostConstruct
   public void init() {
      logger.info("init HostController");
      loadHost();
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public String getCommand() {
      return command;
   }

   public void setRole(String role) {
      this.role = role;
   }

   public String getRole() {
      return role;
   }

   public void setHostId(String hostId) {
      this.hostId = hostId;
   }

   public void setService(String service) {
      this.service = service;
   }

   public String getService() {
      return service;
   }

   public String getHostId() {
      return hostId;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getCluster() {
      return cluster;
   }
   
   public boolean isFound() {
      return found;
   }

   public void setFound(boolean found) {
      this.found = found;
   }   

   public Host getHost() {
      return host;
   }

   private void loadHost() {
      try {
         host = hostEJB.findHostById(hostId);
         if (host != null) {
            found = true;
         }
      } catch (Exception ex) {
         logger.log(Level.WARNING, "Host {0} not found.", hostId);
      }
   }

   public void doCommand(ActionEvent actionEvent) throws NoSuchAlgorithmException, Exception {

      //  TODO: If the web application server craches, status will remain 'Running'.
      Command c = new Command(command, hostId, service, role, cluster);
      commandEJB.persistCommand(c);
      FacesMessage message;

      Host h = hostEJB.findHostById(hostId);
      String ip = h.getPublicIp();
      try {
         WebCommunication webComm = new WebCommunication();
         ClientResponse response = webComm.doCommand(ip, cluster, service, role, command);

         if (response.getClientResponseStatus().getFamily() == Family.SUCCESSFUL) {
            c.succeeded();
            String messageText = "";
            Role s = roleEjb.find(hostId, cluster, service, role);

            if (command.equalsIgnoreCase("init")) {
//               Todo:
            } else if (command.equalsIgnoreCase("start")) {
               JSONObject json = new JSONObject(response.getEntity(String.class));
               messageText = json.getString("msg");
               s.setStatus(Status.Started);

            } else if (command.equalsIgnoreCase("stop")) {
               messageText = command + ": " + response.getEntity(String.class);
               s.setStatus(Status.Stopped);
            }
            roleEjb.store(s);
            message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", messageText);

         } else {
            c.failed();
            if (response.getStatus() == 400) {
               message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", command + ": " + response.getEntity(String.class));
            } else {
               message = new FacesMessage(FacesMessage.SEVERITY_FATAL, "Server Error", "");
            }
         }
      } catch (Exception e) {
         c.failed();
         message = new FacesMessage(FacesMessage.SEVERITY_FATAL, "Communication Error", e.toString());
      }
      commandEJB.updateCommand(c);
      FacesContext.getCurrentInstance().addMessage(null, message);
   }

}