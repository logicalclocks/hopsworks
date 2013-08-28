package se.kth.kthfsdashboard.mysql;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class MySqlController implements Serializable {

   private static final Logger logger = Logger.getLogger(MySqlController.class.getName());
   MySQLAccess mysql = new MySQLAccess();

   public MySqlController() {
      logger.info("MysqlController");
   }

   public StreamedContent getBackup() throws IOException, InterruptedException {
      StreamedContent content = mysql.getBackup();
      if (content == null) {
         FacesContext context = FacesContext.getCurrentInstance();
         context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Backup Failed", "Backup failed. Tray again later."));
      }
      return content;
   }

   public void handleRestoreFileUpload(FileUploadEvent event) {
      FacesMessage msg;
      boolean result = mysql.restore(event.getFile());
      if (result) {
         msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Data restored sucessfully.");
      } else {
         msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Failure", "Restore failed. Try again.");
      }
      FacesContext.getCurrentInstance().addMessage(null, msg);
   }

}