package se.kth.kthfsdashboard.host;

import com.sun.jersey.api.client.ClientResponse;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import se.kth.kthfsdashboard.command.CommandMessageController;
import se.kth.kthfsdashboard.communication.WebCommunication;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.role.Status;
import se.kth.kthfsdashboard.struct.DiskInfo;
import se.kth.kthfsdashboard.util.CollectdTools;


/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
//@SessionScoped
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

   @ManagedProperty(value = "#{commandMessageController}")   
   private CommandMessageController messages;   
   
   private Host host;
   private boolean currentHostAvailable;
   private long lastUpdate;
   private int memoryUsed; //percentage
   private int swapUsed; //percentage
   private String load;
   private String health;
   private List<DiskInfo> df;
   private int cpuCount;
   private CollectdTools collectdTools = new CollectdTools();
   private HashMap<String, List<String>> commandsMap;
   
   private static final Logger logger = Logger.getLogger(HostController.class.getName());   

   public HostController() {
      commandsMap = new HashMap<String, List<String>>();
      commandsMap.put("all", Arrays.asList("install", "uninstall"));
   }
   
   @PostConstruct
   public void init() {
      logger.info("init HostController");
   }   

   public String gotoHost() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        Host h = context.getApplication().evaluateExpressionGet(context, "#{host}", Host.class);

      return "host?faces-redirect=true&hostid=" + hostId;
   }
   
    public CommandMessageController getMessages() {
        return messages;
    }

    public void setMessages(CommandMessageController messages) {
        this.messages = messages;
    }   

   public List<String> getCommands() {

      return commandsMap.get("all");
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

   public List<Host> getHosts() {
      return hostEJB.findHosts();
   }

   public Host getHost() {
      try {
         host = hostEJB.findHostById(hostId);
      } catch (Exception ex) {
         logger.warning("Host ".concat(hostId).concat(" not found."));
      }
      return host;
   }

   public boolean isCurrentHostAvailable() {
      return currentHostAvailable;
   }

   public String getHealth() {
      return "Good!";
   }


   public String getInterfaces() {

      return collectdTools.typeInstances(hostId, "interface").toString();
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