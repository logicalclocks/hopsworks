package se.kth.kthfsdashboard.terminal;

import se.kth.kthfsdashboard.struct.ServiceType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import static org.ocpsoft.urlbuilder.util.Decoder.path;
import se.kth.kthfsdashboard.communication.WebCommunication;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.struct.RoleType;
import se.kth.kthfsdashboard.struct.Status;

/**
 *
 * @author Jim Dowling
 */
@ManagedBean
@RequestScoped
public class TerminalControllerGeneric {

    @ManagedProperty("#{param.cluster}")
    private String cluster;
    @ManagedProperty("#{param.role}")
    private String role;
    @ManagedProperty("#{param.service}")
    private String service;
    @EJB
    private HostEJB hostEjb;
    private static final Logger logger = Logger.getLogger(TerminalControllerGeneric.class.getName());
    private static final String welcomeMessage;

    static {
        welcomeMessage = ("Hop commands: hdfs, yarn, mysql, ndb_mgm\n" //                + "       __  ______  ______  \n"
                //                + "      / / / / __ \\/ __ /\n"
                //                + "     / /_/ / / / / /_/ /\n"
                //                + "    / __  / /_/ / ____/   \n"
                //                + "   /_/ /_/\\____/_/       \n"
                //                + "                                 \n"
                );
    }

    public TerminalControllerGeneric() {
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
//      TODO: Check special characters like ";" to avoid injection attacks
        String roleName;
        cluster = getClusterFromPath();
        if (command.equals("hdfs")) {
            roleName = RoleType.datanode.toString();
            service = ServiceType.HDFS.toString();
        } else if (command.equals("mysql")) {
            roleName = RoleType.mysqld.toString();
            service = ServiceType.NDB.toString();
        } else if (command.equals("ndb_mgm")) {
            roleName = RoleType.mgmserver.toString();
            service = ServiceType.NDB.toString();
        } else if (command.equals("yarn")) {
            service = ServiceType.YARN.toString();
            roleName = RoleType.resourcemanager.toString();
        } else {
            return "Unknown command. Accepted commands are: hdfs, yarn, mysql, ndb_mgm";
        }
        try {
            List<Host> hosts = hostEjb.find(cluster, service, roleName, Status.Started);
            if (hosts.isEmpty()) {
                throw new RuntimeException("No live node available.");
            }
            WebCommunication web = new WebCommunication();
            String result = web.executeRun(hosts.get(0).getPublicOrPrivateIp(),
                    cluster, service, roleName, command, params);
            return result;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return "Error: Could not contact a node: " + ex.getMessage();
        }
    }

    public boolean isClusterActive() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        String url = req.getRequestURL().toString();
        Pattern pattern = Pattern.compile("hop-dashboard/sauron/clusters/");
        Matcher matcher = pattern.matcher(url);
        return matcher.find();
    }
    
    private String getClusterFromPath() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        String url = req.getRequestURL().toString();
        int pos = url.indexOf("/clusters/");
        if (pos == -1) {
            return "No cluster found";
        }
        return url.substring(pos+"/clusters/".length());
    }        
}
