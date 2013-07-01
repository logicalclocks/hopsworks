package se.kth.kthfsdashboard.struct;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ClusterInfo {

   private String name;
   private Long numberOfMachines;
   private Set<String> services = new HashSet<String>();
   private Set<String> roles = new HashSet<String>();
   private Set<String> badServices = new HashSet<String>();
   private Set<String> badRoles = new HashSet<String>();
   private Map<String, Integer> rolesCount = new TreeMap<String, Integer>();
   private Map<String, String> rolesServicesMap = new TreeMap<String, String>();   
   private Integer started, stopped, timedOut;

   public ClusterInfo(String name) {
      started = 0;
      stopped = 0;
      timedOut = 0;
      this.name = name;
   }

   public void setNumberOfMachines(Long numberOfMachines) {
      this.numberOfMachines = numberOfMachines;
   }

   public Long getNumberOfMachines() {
      return numberOfMachines;
   }

   public String getName() {
      return name;
   }

   public String[] getServices() {
      return services.toArray(new String[services.size()]);
   }

   public String[] getRoles() {
      return roles.toArray(new String[roles.size()]);
   }

   public Integer roleCount(String role) {
      return rolesCount.get(role);
   }
   
   public String roleService(String role) {
      return rolesServicesMap.get(role);
   }

   public Health getClusterHealth() {
      if (badRoles.isEmpty()) {
         return Health.Good;
      }
      return Health.Bad;
   }

   public Health serviceHealth(String service) {
      if (badServices.contains(service)) {
         return Health.Bad;
      }
//      return Health.None;
      return Health.Good;      
   }

   public Health roleHealth(String role) {
      if (badRoles.contains(role)) {
         return Health.Bad;
      }
//      return Health.None;
      return Health.Good;
   }

   public Map getStatus() {
      
     Map<Status, Integer> statusMap = new TreeMap<Status, Integer>();
     if (started > 0 ) {
        statusMap.put(Status.Started, started);
     }
     if (stopped > 0 ) {
        statusMap.put(Status.Stopped, stopped);
     }     
     if (timedOut > 0 ) {
        statusMap.put(Status.TimedOut, timedOut);
     }     
     return statusMap;
   }

   public void addRoles(List<RoleHostInfo> roleHostList) {
      for (RoleHostInfo roleHost : roleHostList) {
         services.add(roleHost.getRole().getService());
         roles.add(roleHost.getRole().getRole());
         rolesServicesMap.put(roleHost.getRole().getRole(), roleHost.getRole().getService());
         if (roleHost.getStatus() == Status.Started) {
            started += 1;
         } else {
            badServices.add(roleHost.getRole().getService());
            badRoles.add(roleHost.getRole().getRole());
            if (roleHost.getStatus() == Status.Stopped) {
               stopped += 1;
            } else if (roleHost.getStatus() == Status.TimedOut) {
               timedOut += 1;
            }            
         }
         addRole(roleHost.getRole().getRole());
      }
   }

   private void addRole(String role) {
      if (rolesCount.containsKey(role)) {
         Integer current = rolesCount.get(role);
         rolesCount.put(role, current + 1);
      } else {
         rolesCount.put(role, 1);
      }
   }
}
