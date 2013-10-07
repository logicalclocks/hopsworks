package se.kth.kthfsdashboard.service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import se.kth.kthfsdashboard.communication.WebCommunication;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.role.RoleType;
import se.kth.kthfsdashboard.struct.Status;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class TerminalController {

    @ManagedProperty("#{param.cluster}")
    private String cluster;
    @ManagedProperty("#{param.role}")
    private String role;
    @ManagedProperty("#{param.service}")
    private String service;
    @EJB
    private HostEJB hostEjb;
    private static final Logger logger = Logger.getLogger(TerminalController.class.getName());
    
//    @Inject
    private TerminalController2 s;

    public TerminalController() {
    }

    @PostConstruct
    public void init() {
        logger.info("init TerminalController");
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

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getCluster() {
        return cluster;
    }

    public String handleCommand(String command, String[] params) {
//      TODO: Check special characters like ";" to avoid injection
        String roleName;
        if (service.equalsIgnoreCase(ServiceType.KTHFS.toString())) {
            if (command.equals("hdfs")) {
                roleName = RoleType.datanode.toString();
            } else {
                return "Unknown command. Accepted commands are: hdfs";
            }

        } else if (service.equalsIgnoreCase(ServiceType.MySQLCluster.toString())) {
            if (command.equals("mysql")) {
                roleName = RoleType.mysqld.toString();
            } else if (command.equals("ndb_mgm")) {
                roleName = RoleType.mgmserver.toString();
            } else {
                return "Unknown command. Accepted commands are: mysql, ndb_mgm";
            }
        } else {
            return null;
        }
        try {
//          TODO: get only one host
            List<Host> hosts = hostEjb.find(cluster, service, roleName, Status.Started);
            if (hosts.isEmpty()) {
                throw new RuntimeException("No live node available.");
            }
            WebCommunication web = new WebCommunication();
            return web.executeRun(hosts.get(0).getPublicOrPrivateIp(), cluster, service, roleName, command, params);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return "Error: Could not contact a node";
        }
    }
}