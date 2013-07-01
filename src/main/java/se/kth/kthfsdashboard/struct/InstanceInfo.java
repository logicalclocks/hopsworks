package se.kth.kthfsdashboard.struct;

import java.io.Serializable;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class InstanceInfo implements Serializable {

    private String name;
    private String host;
    private String cluster;
    private String role;
    private String service;
    private Status status;
    private String health;

    public InstanceInfo(String cluster, String service, String role, String host, Status status, String health) {

        this.name = role + " @" + host;
        this.host = host;
        this.cluster = cluster;
        this.service = service;
        this.role = role;
        this.status = status;
        this.health = health;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public Status getStatus() {
        return status;
    }

    public String getHealth() {
        return health;
    }
    
    public String getRole() {
        return role;
    }
    
    public String getService(){
       return service;
    }

   public String getCluster() {
      return cluster;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }
            
}
