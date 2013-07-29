package se.kth.kthfsdashboard.host;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import javax.persistence.*;
import se.kth.kthfsdashboard.struct.Health;
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

   @Id
   @Column(nullable = false, length = 128)
   private String hostId;
   @Column(name = "HOSTNAME_", nullable = false, length = 128)
   private String hostname;
   @Column(length = 15)
   private String publicIp;
   @Column(length = 15)
   private String privateIp;
   private Integer cores;
   private Long lastHeartbeat;
   private Double load1;
   private Double load5;
   private Double load15;
   private Long diskCapacity;
   private Long diskUsed;
   private Long memoryCapacity;
   private Long memoryUsed;

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

   public String getLastHeartbeatInSeconds() {

      DecimalFormat df = new DecimalFormat("#,###,##0.0");
      return df.format(((double) ((new Date()).getTime() - getLastHeartbeat())) / 1000);
   }

   public String getLastHeartbeatFormatted() {
      if (lastHeartbeat == null) {
         return "";
      }
      return Formatter.time(((new Date()).getTime() - lastHeartbeat));
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