package se.kth.kthfsdashboard.struct;

import java.util.HashMap;
import java.util.List;
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
   private List<String> services;

   public ClusterInfo(String name, String status, String health, Long numberOfMachines) {
      roleCounts = new HashMap<String, Integer>();
      this.name = name;
      this.status = status;
      this.health = health;
      this.numberOfMachines = numberOfMachines;

   }

   public String getName() {
      return name;
   }

   public String getStatus() {
      return status;
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

   public List<String> getServices() {
      return services;
   }

   public void setServices(List<String> services) {
      this.services = services;
   }
}