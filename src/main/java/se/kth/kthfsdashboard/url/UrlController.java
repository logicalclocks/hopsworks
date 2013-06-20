package se.kth.kthfsdashboard.url;

import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.*;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class UrlController {

   @ManagedProperty("#{param.hostname}")
   private String hostname;
   @ManagedProperty("#{param.role}")
   private String role;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   @ManagedProperty("#{param.status}")
   private String status;   

   private static final Logger logger = Logger.getLogger(UrlController.class.getName());

   public UrlController() {
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

   public String getHostname() {
      return hostname;
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
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

   public String gotoHost() {
      return "host?faces-redirect=true&hostname=" + hostname;
   }   

   public String doGotoClusterStatus() {
      return "cluster-status?faces-redirect=true&cluster=" + cluster;
   }

   public String gotoServiceInstance() {
      return "services-instances-status?faces-redirect=true&hostname="
              + hostname + "&cluster=" + cluster + "&role=" + role;
   }

   public String gotoClusterStatus() {
      return "cluster-status?faces-redirect=true&cluster=" + cluster;
   }

   public String gotoClusterCommandHistory() {
      return "cluster-commands?faces-redirect=true&cluster=" + cluster;
   }

   public String gotoServiceStatus() {
      return "service-status?faces-redirect=true&cluster=" + cluster + "&service=" + service;
   }

   public String gotoServiceInstances() {
      String url = "service-instances?faces-redirect=true";
      if (hostname != null) {
         url += "&hostname=" + hostname;
      }
      if (cluster != null) {
         url += "&cluster=" + cluster;
      }
      if (service != null) {
         url += "&service=" + service;
      }
      if (role != null) {
         url += "&role=" + role;
      }
      if (status != null) {
         url += "&status=" + status;
      }
      return url;
   }

   public String gotoServiceCommandHistory() {
      return "service-commands?faces-redirect=true&cluster=" + cluster + "&service=" + service;
   }

   public String gotoRole() {
      return "role?faces-redirect=true&hostname=" + hostname + "&cluster=" + cluster
              + "&service=" + service + "&role=" + role;
   }
   
   
}
