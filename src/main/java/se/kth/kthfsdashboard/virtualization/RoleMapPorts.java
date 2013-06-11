/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class RoleMapPorts {

    private final HashMap<String, int[]> roleMappings;

    public enum PortType {

        TCP, UDP, COMMON
    }

    public RoleMapPorts(PortType type) {
        roleMappings = new HashMap();
        //TCP rolemappings for KTHFS
        int[] namenodeTCP = {6000, 6001, 60190, 60113};
        int[] datanodeTCP = {6002, 6003, 6004, 6007, 40102, 34244, 60186, 60231, 60676};
        int[] mgmTCP = {3306, 4848, 8080,1186};
        int[] ndbdTCP = {1186, 10000, 11211};
        int[] mysqldTCP = {3306,1186};
        int[] kthfsagent = {8090};
        //UDP rolemappings for KTHFS
        int[] namenodeUDP = {25826};
        int[] datanodeUDP = {25826};
        int[] mgmUDP = {25826};
        int[] ndbdUDP = {25826};
        int[] mysqldUDP = {25826};
        //common Roles
        int[] ssh = {22};
        int[] webserver = {8080, 8181};
        int[] chefClient = {4000};
        int[] chefServer = {4000, 443, 4040, 444, 8983};
        int[] httphttps = {80, 443};

        switch (type) {
            case TCP:

                roleMappings.put("KTHFS-namenode", namenodeTCP);
                roleMappings.put("KTHFS-datanode", datanodeTCP);
                roleMappings.put("MySQLCluster-mgm", mgmTCP);
                roleMappings.put("MySQLCluster-ndb", ndbdTCP);
                roleMappings.put("MySQLCluster-mysqld", mysqldTCP);
                break;
            case UDP:
                roleMappings.put("KTHFS-namenode", namenodeUDP);
                roleMappings.put("KTHFS-datanode", datanodeUDP);
                roleMappings.put("MySQLCluster-mgm", mgmUDP);
                roleMappings.put("MySQLCluster-ndb", ndbdUDP);
                roleMappings.put("MySQLCluster-mysqld", mysqldUDP);
                break;
            case COMMON:
                roleMappings.put("kthfsagent", kthfsagent);
                roleMappings.put("ssh", ssh);
                roleMappings.put("webServer", webserver);
                roleMappings.put("chefClient", chefClient);
                roleMappings.put("chefServer", chefServer);
                roleMappings.put("http&https", httphttps);
                break;
        }
    }

    public int[] get(String key) {
        return roleMappings.get(key);
    }

    public boolean containsKey(String key) {
        return roleMappings.containsKey(key);
    }

    public Set<String> keySet() {
        return roleMappings.keySet();
    }

    public Collection<int[]> values() {
        return roleMappings.values();
    }
}
