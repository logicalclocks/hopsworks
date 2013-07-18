package se.kth.kthfsdashboard.service;

import com.ocpsoft.pretty.faces.annotation.URLQueryParameter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.struct.Status;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class TestController {


   @ManagedProperty("#{param.cluster}")
   private String cluster;   
   @ManagedProperty("#{param.role}")
   private String role;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.status}")
   private String status;
   @EJB
   private RoleEJB roleEjb;   
   private SelectItem[] statusOptions;
   private SelectItem[] hdfsRoleOptions;
   private static String[] statusStates;
   private static String[] hdfsRoles;
   private List<Role> filteredInstances;
   private static final Logger logger = Logger.getLogger(TestController.class.getName());

   public TestController() {
      statusStates = new String[4];
      statusStates[0] = Status.Started.toString();
      statusStates[1] = Status.Stopped.toString();
      statusStates[2] = Status.Failed.toString();
      statusStates[3] = Status.TimedOut.toString();

      hdfsRoles = new String[]{"namenode", "datanode"};

      statusOptions = createFilterOptions(statusStates);
      hdfsRoleOptions = createFilterOptions(hdfsRoles);

   }

   @PostConstruct
   public void init() {
      logger.info("init TestController");
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

   public void setStatus(String status) {
      this.status = status;
   }

   public String getStatus() {
      return status;
   }


   public List<Role> getRoles() {

      System.err.println(cluster + " " + role);
      ArrayList<Role> roles = new ArrayList<Role>();
      Role r;

      r = new Role();
      r.setRole("namenode");
      r.setService("Stopped");
      roles.add(r);

      if (role == null || !role.equals("namenode")) {
         r = new Role();
         r.setRole("datanode");
         r.setService("Stopped");
         roles.add(r);

         r = new Role();
         r.setRole("datanode");
         r.setService("Started");
         roles.add(r);
      }

      return roles;
   }

   private SelectItem[] createFilterOptions(String[] data) {
      SelectItem[] options = new SelectItem[data.length + 1];

      options[0] = new SelectItem("", "Any");
      for (int i = 0; i < data.length; i++) {
         options[i + 1] = new SelectItem(data[i], data[i]);
      }
      return options;
   }

   public SelectItem[] getStatusOptions() {
      return statusOptions;
   }

   public SelectItem[] getRoleOptions() {
      return hdfsRoleOptions;
   }

   public List<Role> getFilteredInstances() {
      return filteredInstances;
   }

   public void setFilteredInstances(List<Role> filteredInstances) {
      this.filteredInstances = filteredInstances;
   }
   
   public String test() {
       
       Date d = new Date();
       System.err.println(">>>>>> " + d.toString());
       return d.toString();
   }
}