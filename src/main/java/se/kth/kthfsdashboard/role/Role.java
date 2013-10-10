package se.kth.kthfsdashboard.role;

import java.io.Serializable;
import java.text.DecimalFormat;
import javax.persistence.*;
import se.kth.kthfsdashboard.struct.Health;
import se.kth.kthfsdashboard.struct.Status;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Entity
@Table(name = "Roles")
@NamedQueries({
    @NamedQuery(name = "Role.findClusters", query = "SELECT DISTINCT r.cluster FROM Role r"),
    @NamedQuery(name = "Role.findServicesBy-Cluster", query = "SELECT DISTINCT r.service FROM Role r WHERE r.cluster = :cluster"),
    @NamedQuery(name = "Role.find", query = "SELECT r FROM Role r WHERE r.cluster = :cluster AND r.service = :service AND r.role = :role AND r.hostId = :hostId"),
    @NamedQuery(name = "Role.findBy-HostId", query = "SELECT r FROM Role r WHERE r.hostId = :hostId ORDER BY r.cluster, r.service, r.role"),
    @NamedQuery(name = "Role.findBy-Cluster-Service-Role", query = "SELECT r FROM Role r WHERE r.cluster = :cluster AND r.service = :service AND r.role = :role"),
    @NamedQuery(name = "Role.Count", query = "SELECT COUNT(r) FROM Role r WHERE r.cluster = :cluster AND r.service = :service AND r.role = :role"),
    @NamedQuery(name = "Role.Count-hosts", query = "SELECT count(DISTINCT r.hostId) FROM Role r WHERE r.cluster = :cluster"),
    @NamedQuery(name = "Role.Count-roles", query = "SELECT COUNT(r) FROM Role r WHERE r.cluster = :cluster AND r.service = :service"),
    @NamedQuery(name = "Role.findRoleHostBy-Cluster", query = "SELECT NEW se.kth.kthfsdashboard.struct.RoleHostInfo(r, h) FROM Role r, Host h WHERE r.hostId = h.hostId AND r.cluster = :cluster"),
    @NamedQuery(name = "Role.findRoleHostBy-Cluster-Service", query = "SELECT NEW se.kth.kthfsdashboard.struct.RoleHostInfo(r, h) FROM Role r, Host h WHERE r.hostId = h.hostId AND r.cluster = :cluster AND r.service = :service"),
    @NamedQuery(name = "Role.findRoleHostBy-Cluster-Service-Role", query = "SELECT NEW se.kth.kthfsdashboard.struct.RoleHostInfo(r, h) FROM Role r, Host h WHERE r.hostId = h.hostId AND r.cluster = :cluster AND r.service = :service AND r.role = :role"),
    @NamedQuery(name = "Role.findRoleHostBy-Cluster-Service-Role-Host", query = "SELECT NEW se.kth.kthfsdashboard.struct.RoleHostInfo(r, h) FROM Role r, Host h WHERE r.hostId = h.hostId AND r.cluster = :cluster AND r.service = :service AND r.role = :role AND r.hostId = :hostid"),
    @NamedQuery(name = "Role.DeleteBy-HostId", query = "DELETE FROM Role r WHERE r.hostId = :hostId"),
    @NamedQuery(name = "RoleHost.find.ClusterBy-Ip.WebPort", query = "SELECT r.cluster FROM Host h, Role r WHERE h.hostId = r.hostId AND (h.privateIp = :ip OR h.publicIp = :ip) AND r.webPort = :webPort"),       
    //TODO fix this: Hotname may be wrong. mysql nodes change hostname. May use hostid ?    
    @NamedQuery(name = "RoleHost.find.PrivateIpBy-Cluster.Hostname.WebPort", query = "SELECT h.privateIp FROM Host h, Role r WHERE h.hostId = r.hostId AND r.cluster = :cluster AND (h.hostname = :hostname OR h.hostId = :hostname) AND r.webPort = :webPort"),
    @NamedQuery(name = "RoleHost.TotalCores", query="SELECT SUM(h2.cores) FROM Host h2 WHERE h2.hostId IN (SELECT h.hostId FROM Role r, Host h WHERE r.hostId = h.hostId AND r.cluster = :cluster GROUP BY h.hostId)"),
    @NamedQuery(name = "RoleHost.TotalMemoryCapacity", query="SELECT SUM(h2.memoryCapacity) FROM Host h2 WHERE h2.hostId IN (SELECT h.hostId FROM Role r, Host h WHERE r.hostId = h.hostId AND r.cluster = :cluster GROUP BY h.hostId)"),
    @NamedQuery(name = "RoleHost.TotalDiskCapacity", query="SELECT SUM(h2.diskCapacity) FROM Host h2 WHERE h2.hostId IN (SELECT h.hostId FROM Role r, Host h WHERE r.hostId = h.hostId AND r.cluster = :cluster GROUP BY h.hostId)"),    
})
public class Role implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(nullable = false, length = 128)
    private String hostId;
    @Column(nullable = false, length = 48)
    private String service;
    @Column(name = "ROLE_", nullable = false, length = 48)
    private String role;
    @Column(nullable = false, length = 48)
    private String cluster;
    private long uptime;
    @Column(nullable = false)
    private Status status;
    private int pid;
    private Integer webPort;

    public Role() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public Integer getWebPort() {
        return webPort;
    }

    public void setWebPort(Integer webPort) {
        this.webPort = webPort;
    }

    public Health getHealth() {
        if (status == Status.Failed || status == Status.Stopped) {
            return Health.Bad;
        }
        return Health.Good;
    }

    public String uptimeInSeconds() {

        DecimalFormat df = new DecimalFormat("#,###,##0.0");
        return df.format(uptime / 1000);
    }

    @Override
    public String toString() {
        return String.format("%s/%s/%s @ %s", cluster, service, role, hostId);
    }
}