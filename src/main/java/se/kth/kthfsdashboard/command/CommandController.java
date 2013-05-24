package se.kth.kthfsdashboard.command;

import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class CommandController {

    @EJB
    private CommandEJB commandEJB;
    @ManagedProperty("#{param.hostname}")
    private String hostname;
    @ManagedProperty("#{param.role}")
    private String role;
    @ManagedProperty("#{param.service}")
    private String service;
    @ManagedProperty("#{param.cluster}")
    private String cluster;

    public CommandController() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getCluster() {
        return cluster;
    }


    public List<Command> getRecentCommandsByInstance() {
        List<Command> commands = commandEJB.findRecentByCluster(cluster);
        return commands;
    }

    public List<Command> getRunningCommandsByInstance() {
        List<Command> commands = commandEJB.findRunningByCluster(cluster);
        return commands;
    }

    public List<Command> getRecentCommandsByInstanceGroup() {
        List<Command> commands = commandEJB.findRecentByClusterGroup(cluster, service);
        return commands;
    }

    public List<Command> getRunningCommandsByInstanceGroup() {
        List<Command> commands = commandEJB.findRunningByClusterGroup(cluster, service);
        return commands;
    }    
}
