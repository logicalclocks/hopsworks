package se.kth.kthfsdashboard.settings;

import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

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
   

}