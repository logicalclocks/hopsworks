package se.kth.kthfsdashboard.struct;

import java.io.Serializable;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class InstanceFullInfo implements Serializable {

    private String name;
    private String host;
    private String ip;
    private String cluster;
    private String service;
    private String role;
    private Status status;
    private String health;
    private int pid;
    private String uptime;
    private Integer webPort;

    public InstanceFullInfo(String cluster, String service, String role, String host, String ip, Integer webPort, Status status, String health) {

        this.name = role + " @" + host;
        this.host = host;
        this.ip = ip;
        this.cluster = cluster;
        this.role = role;
        this.service = service;
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

    public Status getStatus() {
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

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getWebUiLink() {
//      String url = "http://" + ip + ":" + webPort;
        
//  TODO Only if using private IP address        
        String path = ((HttpServletRequest) FacesContext.getCurrentInstance()
                .getExternalContext().getRequest()).getContextPath();
        String url = path + "/rest/web/" + ip + "/" + webPort + "/";
        return url;
    }

    public String getWebUiLabel() {
//      return host + ":" + webPort;
        return ip + ":" + webPort;
    }
}