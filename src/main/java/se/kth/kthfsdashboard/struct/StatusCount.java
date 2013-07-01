package se.kth.kthfsdashboard.struct;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class StatusCount {

   private Status status;
   private Long count;

   public StatusCount(Status status, Long count) {
        this.status = status;
        this.count = count;
   }

   public Status getStatus() {
      return status;
   }

   public void setStatus(Status status) {
      this.status = status;
   }

   public Long getCount() {
      return count;
   }

   public void setCount(Long count) {
      this.count = count;
   }
   
}
