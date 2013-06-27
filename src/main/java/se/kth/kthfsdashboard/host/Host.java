package se.kth.kthfsdashboard.host;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import javax.persistence.*;
import se.kth.kthfsdashboard.struct.MemoryInfo;
import se.kth.kthfsdashboard.util.Formatter;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Entity
@Table(name = "Hosts")
@NamedQueries({
   @NamedQuery(name = "findAllHosts", query = "SELECT h FROM Host h"),
   @NamedQuery(name = "findHostById", query = "SELECT h FROM Host h WHERE h.hostId = :id"),
   @NamedQuery(name = "findHostByName", query = "SELECT h FROM Host h WHERE h.hostname = :hostname"),   
   @NamedQuery(name = "findHostByRole", query = "SELECT h FROM Host h ")
})
public class Host implements Serializable {

   private static final int HEARTBEAT_INTERVAL = 10;

   public enum Health {

      Good, Bad
   }
   @Id
   @Column(nullable = false, length = 128)
   private String hostId;
   @Column(name = "HOSTNAME_", nullable = false, length = 128)
   private String hostname;
   @Column(length = 15)
   private String publicIp;
   @Column(length = 15)
   private String privateIp;
   private int cores;
   private long lastHeartbeat;
   private double load1;
   private double load5;
   private double load15;
   private long diskCapacity;
   private long diskUsed;
   private long memoryCapacity;
   private long memoryUsed;

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

   public Health getHealth() {

      int hostTimeout = HEARTBEAT_INTERVAL * 2 + 1;
      long deltaInSec = ((new Date()).getTime() - lastHeartbeat) / 1000;

      if (deltaInSec < hostTimeout) {
         return Health.Good;
      }
      return Health.Bad;
   }
}