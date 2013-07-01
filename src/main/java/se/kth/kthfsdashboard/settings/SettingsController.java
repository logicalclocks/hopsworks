package se.kth.kthfsdashboard.settings;

import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import se.kth.kthfsdashboard.struct.Status;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@SessionScoped
public class SettingsController implements Serializable{
   
   private static final Logger logger = Logger.getLogger(SettingsController.class.getName());   
   private String name;
   private int logLines;
   private int numOfGraphColumns;

   public SettingsController() {
      logger.info("SettingsController");      
      name = "Jumbo Hadoop Dashboard";
      logLines = 2;
      numOfGraphColumns = 3;
   }

   public String getName() {
      return name;
   }
   
   public int getNumOfGraphColumns(){
      return numOfGraphColumns;
   }
   
   public void setNumOfGraphColumns(int n) {
      this.numOfGraphColumns = n;
   }
   
   public String tooltip(String id, int n) {
      
      if (id.equals(Status.TimedOut.toString())) {
         return n > 1 ? n + " roles have been timed out (No heartbeat from the host)":
                 "1 role has been timed out (No heartbeat from the host)";
      }
      if (id.equals(Status.Stopped.toString())) {
         return n > 1 ? n + " roles are not running":
                 "1 role is not running";
      }
      if (id.equals(Status.Started.toString())) {
         return n > 1 ? n + " roles are running":
                 "1 role is running";
      }        
      return "";
   }
   public String tooltip(String id) {
      
      if (id.equals(Status.TimedOut.toString())) {
         return "role has been timed out (No heartbeat from the host)";
      }
      if (id.equals(Status.Stopped.toString())) {
         return "role is not running";
      }
      if (id.equals(Status.Started.toString())) {
         return "role is running";
      }        
      return "";
   }   

}