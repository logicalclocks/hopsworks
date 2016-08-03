package io.hops.kmon.host;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import javax.persistence.*;
import io.hops.kmon.struct.Health;
import io.hops.kmon.struct.MemoryInfo;
import io.hops.kmon.utils.FormatUtils;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Entity
@Table(name = "hopsworks.hosts")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Host.find", query = "SELECT h FROM Host h"),
    @NamedQuery(name = "Host.findBy-HostId", query = "SELECT h FROM Host h WHERE h.hostId = :hostId"),
    @NamedQuery(name = "Host.findBy-Hostname", query = "SELECT h FROM Host h WHERE h.hostname = :hostname"),
    @NamedQuery(name = "Host.findBy-Cluster.Service.Role.Status", query = "SELECT h FROM Host h, Role r WHERE h.hostId = r.hostId AND r.cluster = :cluster AND r.service = :service AND r.role = :role AND r.status = :status"),    
    @NamedQuery(name = "Host.findBy-Cluster.Service.Role", query = "SELECT h FROM Host h, Role r WHERE h.hostId = r.hostId AND r.cluster = :cluster AND r.service = :service AND r.role = :role"),    
})
public class Host implements Serializable {

    private static final int HEARTBEAT_INTERVAL = 10;
    @Id
    @Column(name="hostid", nullable = false, length = 128)
    private String hostId;
    
    @Column(name = "hostname_", nullable = false, length = 128)
    private String hostname;
    
    @Column(name="public_ip", length = 15)
    private String publicIp;
    
    @Column(name="private_ip", length = 15)
    private String privateIp;
    
    @Column(name="cores")
    private Integer cores;
    
    @Column(name="last_heartbeat")
    private Long lastHeartbeat;
    
    @Column(name="load1")    
    private Double load1;
    
    @Column(name="load5")    
    private Double load5;
    
    @Column(name="load15")    
    private Double load15;
    
    @Column(name="disk_capacity")    
    private Long diskCapacity;
    
    @Column(name="disk_used")    
    private Long diskUsed;
    
    @Column(name="memory_capacity")    
    private Long memoryCapacity;
    
    @Column(name="memory_used")    
    private Long memoryUsed;
        
    @Column(name="registered")
    private boolean registered;

    public Host() {
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public Long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public Integer getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public Double getLoad1() {
        return load1;
    }

    public void setLoad1(Double load1) {
        this.load1 = load1;
    }

    public Double getLoad5() {
        return load5;
    }

    public void setLoad5(Double load5) {
        this.load5 = load5;
    }

    public Double getLoad15() {
        return load15;
    }

    public void setLoad15(Double load15) {
        this.load15 = load15;
    }

    public Long getDiskCapacity() {
        return diskCapacity;
    }

    public void setDiskCapacity(Long diskCapacity) {
        this.diskCapacity = diskCapacity;
    }

    public Long getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(Long diskUsed) {
        this.diskUsed = diskUsed;
    }

    public Long getMemoryCapacity() {
        return memoryCapacity;
    }

    public void setMemoryCapacity(Long memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }

    public Long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(Long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
    
    public String getPublicOrPrivateIp() {
        if (publicIp == null || publicIp.isEmpty()) {
            return privateIp;
        }
        return publicIp;
    }

    public String getLastHeartbeatInSeconds() {

        DecimalFormat df = new DecimalFormat("#,###,##0.0");
        return df.format(((double) ((new Date()).getTime() - getLastHeartbeat())) / 1000);
    }

    public String getLastHeartbeatFormatted() {
        if (lastHeartbeat == null) {
            return "";
        }
        return FormatUtils.time(((new Date()).getTime() - lastHeartbeat));
    }

    public MemoryInfo getDiskInfo() {

        return new MemoryInfo(diskCapacity, diskUsed);
    }

    public MemoryInfo getMemoryInfo() {

        return new MemoryInfo(memoryCapacity, memoryUsed);
    }

    public Health getHealth() {

        int hostTimeout = HEARTBEAT_INTERVAL * 2 + 1;
        if (lastHeartbeat == null) {
            return Health.Bad;
        }
        long deltaInSec = ((new Date()).getTime() - lastHeartbeat) / 1000;
        if (deltaInSec < hostTimeout) {
            return Health.Good;
        }
        return Health.Bad;
    }
}