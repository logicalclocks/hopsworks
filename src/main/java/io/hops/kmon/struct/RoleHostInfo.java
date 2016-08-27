package io.hops.kmon.struct;

import io.hops.kmon.host.Host;
import io.hops.kmon.role.Role;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class RoleHostInfo {

   private Role role;
   private Host host;

   public RoleHostInfo(Role role, Host host) {
      this.role = role;
      this.host = host;
   }

   public Role getRole() {
      return role;
   }

   public Host getHost() {
      return host;
   }
   
   public Health getHealth() {
//      if (host.getHealth() == Health.Good && role.getHealth() == Health.Good) {
//         return Health.Good;
//      }
      if (role.getHealth() == Health.Good) {
         return Health.Good;
      }      
      return Health.Bad;
   }
   
   public Status getStatus() {

      
//      if(host.getHealth() == Health.Good) {
         return role.getStatus();
//      }
//      return Status.TimedOut;
   }
}