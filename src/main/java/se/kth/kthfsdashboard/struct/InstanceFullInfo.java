package se.kth.kthfsdashboard.struct;

import java.io.Serializable;
import se.kth.kthfsdashboard.role.Role;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class InstanceFullInfo implements Serializable {

    private String name;
    private String host;
    private String ip;    
    private String instance;
    private String service;
    private String role;
    private String rack;
    private Role.Status status;
    private String health;
    private int pid;
    private String uptime;
    private Integer webPort;

    public InstanceFullInfo(String instance, String service, String role, String host, String ip, Integer webPort, String rack, Role.Status status, String health) {

        this.name = role + " (" + host + ")";
        this.host = host;
        this.ip = ip;
        this.instance = instance;
        this.role = role;
        this.service = service;
        this.rack = rack;
        this.status = status;
        this.health = health;
        this.webPort = webPort;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getIp() {
        return ip;
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

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

   public Integer getWebPort() {
      return webPort;
   }

   public void setWebPort(Integer webPort) {
      this.webPort = webPort;
   }

   public String getInstance() {
      return instance;
   }

   public void setInstance(String instance) {
      this.instance = instance;
   }

   public String getService() {
      return service;
   }

   public void setService(String service) {
      this.service = service;
   }
   
   public String getWebUiLink() {
//      return "http://" + host + ":" + webPort;
      return "http://" + ip + ":" + webPort;      
   }
   
   public String getWebUiLabel() {
//      return host + ":" + webPort;
      return ip + ":" + webPort;      
   }   
     
}