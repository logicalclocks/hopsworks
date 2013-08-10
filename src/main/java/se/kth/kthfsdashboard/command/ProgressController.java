package se.kth.kthfsdashboard.command;

import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
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
public class ProgressController {

    @EJB
    private CommandEJB commandEJB;
    @ManagedProperty("#{param.hostid}")
    private String hostId;
    @ManagedProperty("#{param.role}")
    private String role;
    @ManagedProperty("#{param.service}")
    private String service;
    @ManagedProperty("#{param.cluster}")
    private String cluster;
    private static final Logger logger = Logger.getLogger(ProgressController.class.getName());
    private List<Command> commands;

    public ProgressController() {
    }

    @PostConstruct
    public void init() {
        logger.info("init CommandProgressController");
        setCommands(getLatestCommandByClusterServiceInstanceHost());
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

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getCluster() {
        return cluster;
    }

    public List<Command> getLatestCommandByClusterServiceInstanceHost() {
        return commandEJB.findLatestByClusterServiceRoleHostId(cluster, service, role, hostId);
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }


}
