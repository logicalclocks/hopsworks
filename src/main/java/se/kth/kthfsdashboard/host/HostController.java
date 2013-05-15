package se.kth.kthfsdashboard.host;

import com.sun.jersey.api.client.ClientResponse;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import se.kth.kthfsdashboard.log.Log;
import se.kth.kthfsdashboard.log.LogEJB;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.struct.DiskInfo;
import se.kth.kthfsdashboard.util.CollectdTools;
import se.kth.kthfsdashboard.util.WebCommunication;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
//@SessionScoped
@RequestScoped
public class HostController implements Serializable {

   @EJB
   private LogEJB logEJB;
   @EJB
   private HostEJB hostEJB;
   @EJB
   private RoleEJB roleEjb;
   @EJB
   private CommandEJB commandEJB;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   @ManagedProperty("#{param.hostname}")
   private String hostname;
   @ManagedProperty("#{param.command}")
   private String command;
   @ManagedProperty("#{param.servicegroup}")
   private String serviceGroup;
   @ManagedProperty("#{param.role}")
   private String role;
   
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

   public HostController() {
      commandsMap = new HashMap<String, List<String>>();
      commandsMap.put("all", Arrays.asList("install", "uninstall"));
   }

   public String gotoHost() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        Host h = context.getApplication().evaluateExpressionGet(context, "#{host}", Host.class);

      return "host?faces-redirect=true&hostname=" + hostname;
   }

   public void doSetRackId() {

      hostEJB.storeHostRackId(host);
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

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public void setServiceGroup(String serviceGroup) {
      this.serviceGroup = serviceGroup;
   }

   public String getServiceGroup() {
      return serviceGroup;
   }

   public String getHostname() {
      return hostname;
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
      host = hostEJB.findHostByName(hostname);
      return host;
   }

   public boolean isCurrentHostAvailable() {
      return currentHostAvailable;
   }

   public long getLastUpdate() {

      Long time = logEJB.findLatestLogTime(hostname).longValue();
      this.lastUpdate = (new Date()).getTime() / 1000 - time;
      return lastUpdate;
   }

   public int getMemoryUsed() {

      List<Log> logs = logEJB.findLatestLogForPlugin(hostname, "memory");

      double total = 0, used = 0, free, cached, buffered;
      DecimalFormat format = new DecimalFormat("#.##");

      for (Log l : logs) {

         String stringValue = l.getValues().substring(1, l.getValues().length() - 1);
         double doubleValue;
         try {
            doubleValue = format.parse(stringValue).doubleValue();
            total += doubleValue;
            if (l.getTypeInstance().equals("used")) {
               used = doubleValue;
            }
         } catch (Exception e) {
            System.err.println("Exception:" + e);
         }
      }

      this.memoryUsed = ((Long) (Math.round((used / total) * 100))).intValue();

      return memoryUsed;
   }

   public int getSwapUsed() {

      List<Log> logs = logEJB.findLatestLogForPlugin(hostname, "swap");

      double total = 0, used = 0;
      DecimalFormat format = new DecimalFormat("#.##");

      for (Log l : logs) {

         String stringValue = l.getValues().substring(1, l.getValues().length() - 1);
         double doubleValue;
         try {
            doubleValue = format.parse(stringValue).doubleValue();

            if (l.getType().equals("swap")) {
               total += doubleValue;
               if (l.getTypeInstance().equals("used")) {
                  used = doubleValue;
               }
            }
         } catch (Exception e) {
            System.err.println("Exception:" + e);
         }
      }

      this.swapUsed = ((Long) (Math.round((used / total) * 100))).intValue();
      return swapUsed;
   }

   public String getHealth() {
      return "Good!";
   }

   public String getLoad() {

      Log log = logEJB.findLatestLogForPluginAndType(hostname, "load", "load");

      String loads = "";
      for (String l : log.getValues().split("[\\[\\],]")) {
         if (!loads.isEmpty()) {
            loads += " ";
         }
         loads += l;
      }
      this.load = loads;

      return load;
   }

   public List<DiskInfo> getDf() throws ParseException {

      List<DiskInfo> diskInfoList = new ArrayList<DiskInfo>();
      List<Log> logs = logEJB.findLatestLog(hostname, "df", "df");
      for (Log log : logs) {
         diskInfoList.add(new DiskInfo(log.getTypeInstance(), log.getValues()));
      }
      return diskInfoList;
   }

   public String getInterfaces() {

      return collectdTools.typeInstances(hostname, "interface").toString();
   }
   
   public void doCommand(ActionEvent actionEvent) throws NoSuchAlgorithmException, Exception {

      //  TODO: If the web application server craches, status will remain 'Running'.
      Command c = new Command(command, hostname, serviceGroup, role, cluster);
      commandEJB.persistCommand(c);
      FacesMessage message;

      //Todo: does not work with hostname. Only works with IP address.
      Host h = hostEJB.findHostByName(hostname);      
      WebCommunication webComm = new WebCommunication(h.getIp(), cluster, serviceGroup ,role);
      
      try {
         ClientResponse response = webComm.doCommand(command);

         if (response.getClientResponseStatus().getFamily() == Family.SUCCESSFUL) {
            c.succeeded();
            String messageText = "";
            Role s = roleEjb.find(hostname, cluster, serviceGroup, role);
            
            if (command.equalsIgnoreCase("init")) {
//               Todo:
               
            } else if (command.equalsIgnoreCase("start")) {
               JSONObject json = new JSONObject(response.getEntity(String.class));
               messageText = json.getString("msg");
               s.setStatus(Role.Status.Started);

            } else if (command.equalsIgnoreCase("stop")) {
               messageText = command + ": " + response.getEntity(String.class);
               s.setStatus(Role.Status.Stopped);
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