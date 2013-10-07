package se.kth.kthfsdashboard.struct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ServiceInfo {

    private String name;
    private Health health;
    private Set<String> roles = new HashSet<String>();
    private Map<String, Integer> rolesCount = new HashMap<String, Integer>();
    private Set<String> badRoles = new HashSet<String>();
    private int started;
    private int stopped;
    private int timedOut;

    public ServiceInfo(String name) {
        started = 0;
        stopped = 0;
        timedOut = 0;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map getStatus() {

        Map<Status, Integer> statusMap = new TreeMap<Status, Integer>();
        if (started > 0) {
            statusMap.put(Status.Started, started);
        }
        if (stopped > 0) {
            statusMap.put(Status.Stopped, stopped);
        }
        if (timedOut > 0) {
            statusMap.put(Status.TimedOut, timedOut);
        }
        return statusMap;
    }

    public Health getHealth() {
        return health;
    }

    public Integer roleCount(String role) {
        return rolesCount.get(role);
    }

    public String[] getRoles() {
        return roles.toArray(new String[rolesCount.size()]);
    }

    public Health roleHealth(String role) {
        if (badRoles.contains(role)) {
            return Health.Bad;
        }
//      return Health.None;
        return Health.Good;
    }

    public Health addRoles(List<RoleHostInfo> roles) {
        for (RoleHostInfo roleHostInfo : roles) {
            if (roleHostInfo.getRole().getRole().equals("")) {
                continue;
            }
            this.roles.add(roleHostInfo.getRole().getRole());
            if (roleHostInfo.getStatus() == Status.Started) {
                started += 1;
            } else {
                badRoles.add(roleHostInfo.getRole().getRole());
                if (roleHostInfo.getStatus() == Status.Stopped) {
                    stopped += 1;
                } else {
                    timedOut += 1;
                }
            }
            addRole(roleHostInfo.getRole().getRole());
        }
        health = (stopped + timedOut > 0) ? Health.Bad : Health.Good;
        return health;
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
