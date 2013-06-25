package se.kth.kthfsdashboard.struct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.Status;

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
   private Map<String, Integer> rolesCount = new HashMap<String, Integer>();
   private Map<String, String> rolesServicesMap = new HashMap<String, String>();   
   private Integer started;
   private Integer stopped;

   public ClusterInfo(String name) {
      started = 0;
      stopped = 0;
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
      return Health.None;
   }

   public Health roleHealth(String role) {
      if (badRoles.contains(role)) {
         return Health.Bad;
      }
      return Health.None;
   }

   public String getStatus() {
      return String.format("%d Started, %d Stopped", started, stopped);
   }

   public void addRoles(List<Role> rolesList) {
      for (Role role : rolesList) {
         services.add(role.getService());
         roles.add(role.getRole());
         rolesServicesMap.put(role.getRole(), role.getService());
         if (role.getStatus() == Status.Started) {
            started += 1;
         } else {
            badServices.add(role.getService());
            badRoles.add(role.getRole());
            stopped += 1;
         }
         addRole(role.getRole());
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
