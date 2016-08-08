package io.hops.kmon.terminal;

import io.hops.kmon.struct.ServiceType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.kmon.communication.WebCommunication;
import io.hops.kmon.host.Host;
import io.hops.kmon.host.HostEJB;
import io.hops.kmon.struct.RoleType;
import io.hops.kmon.struct.Status;

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
    @EJB
    private WebCommunication web;    
    private static final Logger logger = Logger.getLogger(TerminalController.class.getName());
    private static final String welcomeMessage;

    static {
        welcomeMessage = ("Welcome to \n"
+ "     __  __               \n"         
+ "    / / / /___  ____  _____       \n"
+ "   / /_/ / __ \\/ __ \\/ ___/       \n"
+ "  / __  / /_/ / /_/ (__  )        \n"
+ " /_/ /_/\\____/ .___/____/         \n"
+ "            /_/                   \n"    
+ "                                  \n")
                .replace(" ", "&nbsp;")
                .replace("\\", "&#92;")
                .replace("\n", "<br/>");
    }

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

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public String handleCommand(String command, String[] params) {
//      TODO: Check special characters like ";" to avoid injection
        String roleName;
        if (service.equalsIgnoreCase(ServiceType.HDFS.toString())) {
            if (command.equals("hdfs")) {
                roleName = RoleType.datanode.toString();
            } else {
                return "Unknown command. Accepted commands are: hdfs";
            }

        } else if (service.equalsIgnoreCase(ServiceType.NDB.toString())) {
            if (command.equals("mysql")) {
                roleName = RoleType.mysqld.toString();
            } else if (command.equals("ndb_mgm")) {
                roleName = RoleType.mgmserver.toString();
            } else {
                return "Unknown command. Accepted commands are: mysql, ndb_mgm";
            }
        } else if (service.equalsIgnoreCase(ServiceType.YARN.toString())) {
            if (command.equals("yarn")) {
                roleName = RoleType.resourcemanager.toString();
            } else {
                return "Unknown command. Accepted commands are: yarn";
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
            String result = web.executeRun(hosts.get(0).getPublicOrPrivateIp(), hosts.get(0).getAgentPassword(), 
                    cluster, service, roleName, command, params);
            return result;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return "Error: Could not contact a node";
        }
    }
}