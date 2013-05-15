package se.kth.kthfsdashboard.host;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import javax.persistence.*;
import se.kth.kthfsdashboard.struct.MemoryInfo;
import se.kth.kthfsdashboard.util.CollectdTools;
import se.kth.kthfsdashboard.util.Formatter;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Entity
@Table(name = "Hosts")
@NamedQueries({
    @NamedQuery(name = "findAllHosts", query = "SELECT h FROM Host h"),
    @NamedQuery(name = "findHostByName", query = "SELECT h FROM Host h WHERE h.name = :name"),
    @NamedQuery(name = "findHostByRole", query = "SELECT h FROM Host h ")
})
public class Host implements Serializable {

    public static final int HOST_TIMEOUT = 20;

    public enum Health {

        Good, Bad
    }
    @Id
    @Column(name = "host_name", nullable = false, length = 128)
    private String name;
    @Column(name = "rack", nullable = false, updatable = true)
    private String rack;
    @Column(name = "ip", nullable = false, length = 15)
    private String ip;
    @Column(name = "last_heartbeat", nullable = false)
    private long lastHeartbeat;
    @Column(name = "cores", nullable = false)
    private int cores;
    @Column(name = "load1", nullable = false)
    private double load1;
    @Column(name = "load5", nullable = false)
    private double load5;
    @Column(name = "load15", nullable = false)
    private double load15;
    @Column(name = "disk_capacity", nullable = false)
    private long diskCapacity;
    @Column(name = "disk_used", nullable = false)
    private long diskUsed;
    @Column(name = "memory_capacity", nullable = false)
    private long memoryCapacity;
    @Column(name = "memory_used", nullable = false)
    private long memoryUsed;

    public Host() {
    }

    public Host(String name, String rack, String ip, long lastHeartbeat,
            int cores, double load1, double load5, double load15,
            long diskCapacity, long diskUsed, long memoryCapacity, long memoryUsed) {
        this.name = name;
        this.rack = rack;
        this.ip = ip;
        this.lastHeartbeat = lastHeartbeat;
        this.cores = cores;
        this.load1 = load1;
        this.load5 = load5;
        this.load15 = load15;
        this.diskCapacity = diskCapacity;
        this.diskUsed = diskUsed;
        this.memoryCapacity = memoryCapacity;
        this.memoryUsed = memoryUsed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public double getLoad1() {
        return load1;
    }

    public void setLoad1(double load1) {
        this.load1 = load1;
    }

    public double getLoad5() {
        return load5;
    }

    public void setLoad5(double load5) {
        this.load5 = load5;
    }

    public double getLoad15() {
        return load15;
    }

    public void setLoad15(double load15) {
        this.load15 = load15;
    }

    public long getDiskCapacity() {
        return diskCapacity;
    }

    public void setDiskCapacity(long diskCapacity) {
        this.diskCapacity = diskCapacity;
    }

    public long getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(long diskUsed) {
        this.diskUsed = diskUsed;
    }

    public long getMemoryCapacity() {
        return memoryCapacity;
    }

    public void setMemoryCapacity(long memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }

    public long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public String getLastHeartbeatInSeconds() {

        DecimalFormat df = new DecimalFormat("#,###,##0.0");
        return df.format(((double) ((new Date()).getTime() - getLastHeartbeat())) / 1000);
    }

    public String getLastHeartbeatFormatted() {

        return Formatter.time(((new Date()).getTime() - getLastHeartbeat()));
    }

    public MemoryInfo getDiskInfo() {

        return new MemoryInfo(diskCapacity, diskUsed);
    }

    public MemoryInfo getMemoryInfo() {

        return new MemoryInfo(memoryCapacity, memoryUsed);
    }

    public String getTest() {
        String h = name;
        CollectdTools ct = new CollectdTools();
        return ct.getTest(h, "free");
    }

    public Health getHealth() {

        long deltaInSec = ((new Date()).getTime() - lastHeartbeat) / 1000;

        if (deltaInSec < HOST_TIMEOUT) {
            return Health.Good;
        }
        return Health.Bad;
    }
}