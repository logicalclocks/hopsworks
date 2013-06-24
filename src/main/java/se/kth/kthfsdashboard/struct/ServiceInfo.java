package se.kth.kthfsdashboard.struct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.Status;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ServiceInfo {

   private String name;
   private Health health;
   private Map<String, Integer> rolesCount = new HashMap<String, Integer>();
   private Integer started;
   private Integer stopped;

   public ServiceInfo(String name) {
      started = 0;
      stopped = 0;
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public String getStatus() {
      return String.format("%d Started, %d Stopped", started, stopped);
   }

   public Health getHealth() {
      return health;
   }

   public Map getRoleCounts() {
      return rolesCount;
   }

   public Health addRoles(List<Role> roles) {
      for (Role role : roles) {
         if (role.getStatus() == Status.Started) {
            started += 1;
         } else {
            stopped += 1;
         }
         addRole(role.getRole());
      }
      health = (stopped > 0) ? Health.Bad : Health.Good;
      return health;
   }

   private void addRole(String service) {
      if (rolesCount.containsKey(service)) {
         Integer current = rolesCount.get(service);
         rolesCount.put(service, current + 1);
      } else {
         rolesCount.put(service, 1);
      }
   }
}