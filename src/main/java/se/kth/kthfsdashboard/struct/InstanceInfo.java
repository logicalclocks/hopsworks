package se.kth.kthfsdashboard.struct;

import java.io.Serializable;
import se.kth.kthfsdashboard.role.Role;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class InstanceInfo implements Serializable {

    private String name;
    private String host;
    private String cluster;
    private String role;
    private String serviceGroup;
    private String rack;
    private Role.Status status;
    private String health;

    public InstanceInfo(String cluster, String serviceGroup, String role, String host, String rack, Role.Status status, String health) {

        this.name = role + " (" + host + ")";
        this.host = host;
        this.cluster = cluster;
        this.serviceGroup = serviceGroup;
        this.role = role;
        this.rack = rack;
        this.status = status;
        this.health = health;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getRack() {
        return rack;
    }

    public Role.Status getStatus() {
        return status;
    }

    public String getHealth() {
        return health;
    }
    
    public String getRole() {
        return role;
    }
    
    public String getServiceGroup(){
       return serviceGroup;
    }

   public String getCluster() {
      return cluster;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }
            
}
