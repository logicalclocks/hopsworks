package se.kth.kthfsdashboard.command;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import se.kth.kthfsdashboard.util.Formatter;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Entity
@Table(name = "Commands")
@NamedQueries({
   @NamedQuery(name = "Commands.find", query = "SELECT c FROM Command c"),
   @NamedQuery(name = "Commands.findRecentByCluster", query = "SELECT c FROM Command c WHERE c.cluster = :cluster AND (NOT c.status = :status)  ORDER BY c.startTime DESC"),
   @NamedQuery(name = "Commands.findRunningByCluster", query = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.status = :status  ORDER BY c.startTime DESC"),
   @NamedQuery(name = "Commands.findRecentByCluster-Group", query = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.serviceGroup = :group AND (NOT c.status = :status)  ORDER BY c.startTime DESC"),
   @NamedQuery(name = "Commands.findRunningByCluster-Group", query = "SELECT c FROM Command c WHERE c.cluster = :cluster AND c.serviceGroup = :group AND c.status = :status  ORDER BY c.startTime DESC")   
})
public class Command implements Serializable {

   public enum commandStatus {

      Running, Succeeded, Failed
   }
   @Id
   @GeneratedValue(strategy = GenerationType.SEQUENCE)
   private Long id;
   @Column(name = "command", nullable = false, length = 256)
   private String command;
   @Column(name = "host_name", nullable = false, length = 128)
   private String hostname;
   @Column(nullable = false, length = 48)
   private String serviceGroup;
   @Column(nullable = false, length = 48)
   private String role;
   @Column(nullable = false, length = 48)
   private String cluster;
   @Column(name = "start_time")
   @Temporal(javax.persistence.TemporalType.TIMESTAMP)
   private Date startTime;
   @Column(name = "end_time")
   @Temporal(javax.persistence.TemporalType.TIMESTAMP)
   private Date endTime;
   private commandStatus status;

   public Command() {
   }

   public Command(String command, String hostname, String serviceGroup, String role, String cluster) {
      this.command = command;
      this.hostname = hostname;
      this.serviceGroup = serviceGroup;
      this.role = role;
      this.cluster = cluster;

      this.startTime = new Date();
      this.status = commandStatus.Running;
   }

   public Long getId() {
      return id;
   }

   public String getCommand() {
      return command;
   }

   public String getHostname() {
      return hostname;
   }

   public String getServiceGroup() {
      return serviceGroup;
   }

   public String getRole() {
      return role;
   }

   public String getCluster() {
      return cluster;
   }

   public Date getStartTime() {
      return startTime;
   }

   public String getStartTimeShort() {
      return Formatter.date(startTime);
   }

   public Date getEndTime() {
      return endTime;
   }

   public String getEndTimeShort() {
      return Formatter.date(endTime);
   }

   public commandStatus getStatus() {
      return status;
   }

   public void succeeded() {

      this.endTime = new Date();
      this.status = commandStatus.Succeeded;

   }

   public void failed() {
      this.endTime = new Date();
      this.status = commandStatus.Failed;
   }
}