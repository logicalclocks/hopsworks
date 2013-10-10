package se.kth.kthfsdashboard.struct;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class RoleInstancesInfo {

    private String fullName;
    private String roleName;
    private SortedMap<Status, Integer> statusMap = new TreeMap<Status, Integer>();
    private SortedMap<Health, Integer> healthMap = new TreeMap<Health, Integer>();

    public RoleInstancesInfo(String fullName, RoleType role) {
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

    public SortedMap<Status, Integer> getStatusMap() {
        return statusMap;
    }

    public SortedMap<Health, Integer> getHealthMap() {
        return healthMap;
    }

    public void addInstanceInfo(Status status, Health health) {
        if (statusMap.containsKey(status)) {
            statusMap.put(status, statusMap.get(status) + 1);
        } else {
            statusMap.put(status, 1);
        }
        if (healthMap.containsKey(health)) {
            Integer count = healthMap.get(health) + 1;
            healthMap.put(health, count);
        } else {
            healthMap.put(health, 1);
        }
    }
    
    public Health getOverallHealth() {
        if (healthMap.containsKey(Health.Bad)) {
            return Health.Bad;
        } else {
            return Health.Good;
        }
    }
}