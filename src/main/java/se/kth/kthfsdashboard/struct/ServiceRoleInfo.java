package se.kth.kthfsdashboard.struct;

import java.util.SortedMap;
import java.util.TreeMap;
import se.kth.kthfsdashboard.role.RoleType;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ServiceRoleInfo {

   private String fullName;
   private String roleName;
   private SortedMap<Status, Integer> statusMap = new TreeMap<Status, Integer>();
   private SortedMap<Health, Integer> healthMap = new TreeMap<Health, Integer>();

   public ServiceRoleInfo(String fullName, RoleType role) {
      this.fullName = fullName;
      this.roleName = role.toString();
   }

   public String getFullName() {
      return fullName;
   }

   public String getRoleName() {
      return roleName;
   }

   public Health getRoleHealth() {
      if (!healthMap.containsKey(Health.Bad)) {
         return Health.Good;
      }
      return Health.Bad;
   }

   public String[] getStatusEntries() {
      return statusMap.entrySet().toArray(new String[statusMap.size()]);
   }

   public Integer getStatusCount(Status status) {
      return statusMap.get(status);
   }

   public void setStatusMap(SortedMap<Status, Integer> statusMap) {
      this.statusMap = statusMap;
   }

   public SortedMap<Status, Integer> getStatusMap() {
      return statusMap;
   }

   public SortedMap<Health, Integer> getHealthMap() {
      return healthMap;
   }

   public void setHealthMap(SortedMap<Health, Integer> healthMap) {
      this.healthMap = healthMap;
   }
}