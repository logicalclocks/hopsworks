package se.kth.kthfsdashboard.url;

import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class UrlController {

    @ManagedProperty("#{param.hostid}")
    private String hostId;
    @ManagedProperty("#{param.role}")
    private String role;
    @ManagedProperty("#{param.service}")
    private String service;
    @ManagedProperty("#{param.cluster}")
    private String cluster;
    @ManagedProperty("#{param.status}")
    private String status;
    @ManagedProperty("#{param.target}")
    private String target;
    private static final Logger logger = Logger.getLogger(UrlController.class.getName());

    public UrlController() {
        logger.info("UrlController");
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

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String host() {
        return "host?faces-redirect=true&hostid=" + hostId;
    }

    public String clusterStatus(String cluster) {
        return "cluster-status?faces-redirect=true&cluster=" + cluster;
    }

    public String serviceInstance() {
        return "services-instances-status?faces-redirect=true&hostid="
                + hostId + "&cluster=" + cluster + "&role=" + role;
    }

    public String clusterActionHistory() {
        return "cluster-actionhistory?faces-redirect=true&cluster=" + cluster;
    }

    public String serviceStatus() {
        return "service-status?faces-redirect=true&cluster=" + cluster + "&service=" + service;
    }

    public String serviceInstances() {
        String url = "service-instances?faces-redirect=true";
        if (hostId != null) {
            url += "&hostid=" + hostId;
        }
        if (cluster != null) {
            url += "&cluster=" + cluster;
        }
        if (service != null) {
            url += "&service=" + service;
        }
        if (role != null) {
            url += "&r=" + role;
        }
        if (status != null) {
            url += "&s=" + status;
        }
        return url;
    }

    public String serviceActionHistory() {
        return "service-actionhistory?faces-redirect=true&cluster=" + cluster + "&service=" + service;
    }
    
    public String serviceTerminal() {
        return "service-terminal?faces-redirect=true&cluster=" + cluster + "&service=" + service;
    }    

    public String roleStatus() {
        return "role-status?faces-redirect=true&hostid=" + hostId + "&cluster=" + cluster
                + "&service=" + service + "&role=" + role;
    }

    public String roleActionHistory() {
        return "role-actionhistory?faces-redirect=true&hostid=" + hostId + "&cluster=" + cluster
                + "&service=" + service + "&role=" + role;
    }

    public String editGraphsUrl() {
        String url = "edit-graphs?faces-redirect=true&";
        if (target != null) {
            url += url + "target=" + target;
        }
        return url;
    }
}
