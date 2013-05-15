package se.kth.kthfsdashboard.settings;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@SessionScoped
public class SettingsController implements Serializable{
   
   private String name;
   private int logLines;
   private int numOfGraphColumns;

   public SettingsController() {
      name = "Jumbo Hadoop Dashboard";
      logLines = 2;
      numOfGraphColumns = 5;
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