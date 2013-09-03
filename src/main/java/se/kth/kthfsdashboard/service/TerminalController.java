package se.kth.kthfsdashboard.service;

import com.sun.jersey.api.client.ClientResponse;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
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
        if (service.equals(ServiceType.KTHFS.toString())) {
            if (!command.equals("hdfs")) {
                return "Invalid command. Accepted commands are: hdfs";
            } else if (command.contains(";")) {
                return "Invalid character ;";
            } else {
                WebCommunication web = new WebCommunication();
                String result = "Error: Could not contact a KTHFS node";
                try {
//                  TODO: get only one datanode
                    List<Host> liveDatanodes = hostEjb.find(cluster, service, RoleType.datanode.toString(), Status.Started);
                    if (liveDatanodes.isEmpty()) {
                        return result;
                    }
                    String address = liveDatanodes.get(0).getPublicOrPrivateIp();
                    ClientResponse response = web.execute(address, cluster, service, RoleType.datanode.toString(), command, params);
                    if (response.getClientResponseStatus().getFamily() == Response.Status.Family.SUCCESSFUL) {
                        result = textToHtml(response.getEntity(String.class));
                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                return result;
            }
        }
        return null;
    }

    private String textToHtml(String text) {
        String html = StringEscapeUtils.escapeHtml(text);
        html = html.replaceAll("\n", "<br>");
        html = html.replaceAll("\t", StringUtils.repeat("&nbsp;", 8));
        html = html.replaceAll(" ", StringUtils.repeat("&nbsp;", 1));
        return html;
    }
}