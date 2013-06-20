package se.kth.kthfsdashboard.struct;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ClusterInfo {

   private String name;
   private String status;
   private String health;
   private Map roleCounts;
   private Long numberOfMachines;
   private Map<String, String> services;
   private Map<String, String> rolesHealth;   

   public ClusterInfo(String name, Long numberOfMachines) {
      roleCounts = new HashMap<String, Integer>();
      this.name = name;
      this.numberOfMachines = numberOfMachines;

   }

   public String getName() {
      return name;
   }

   public void setStatus(String status) {
      this.status = status;
   }   
   
   public String getStatus() {
      return status;
   }

   public void setHealth(String health) {
      this.health = health;
   }   
   
   public String getHealth() {
      return health;
   }

   public Map getRoleCounts() {
      return roleCounts;
   }
   
   public Long getNumberOfMachines() {
      return numberOfMachines;
   }

   public void putToRoleCounts(String service, Integer count) {
      this.roleCounts.put(service, count);
   }

   public Map<String, String> getServices() {
      return services;
   }

   public void setServices(Map<String, String> services) {
      this.services = services;
   }
   
   public Map<String, String> getRolesHealth() {
      return rolesHealth;
   }

   public void setRolesHealth(Map<String, String> rolesHealth) {
      this.rolesHealth = rolesHealth;
   }   
}