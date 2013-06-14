package se.kth.kthfsdashboard.mysql;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.struct.NodesTableItem;
import se.kth.kthfsdashboard.util.WebCommunication;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class MySqlController implements Serializable {

   @EJB
   RoleEJB roleEjb;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   private boolean flag;
   private List<NodesTableItem> info;
   private UploadedFile file;
   MySQLAccess mysql = new MySQLAccess();

   public MySqlController() {
   }

   public String getService() {
      return service;
   }

   public void setService(String service) {
      this.service = service;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getCluster() {
      return cluster;
   }

   public List<NodesTableItem> getInfo() throws Exception {

      info = loadItems();
      return info;
   }

   public boolean getFlag() {
      return flag;
   }

   public void setFlag(boolean flag) {
      this.flag = flag;
   }

   public UploadedFile getFile() {
      return file;
   }

   public void setFile(UploadedFile file) {
      this.file = file;
   }

   public void load(ActionEvent event) {

      System.err.println("Loading...");
      if (info != null) {
         return;
      }
      info = loadItems();
      setFlag(true);
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
      try {
         Thread.sleep(3000);
      } catch (InterruptedException ex) {
         Logger.getLogger(MySqlController.class.getName()).log(Level.SEVERE, null, ex);
      }

      FacesContext.getCurrentInstance().addMessage(null, msg);
   }

   private List<NodesTableItem> loadItems() {
      System.err.println("Load Items");      
      // Finds hostname of mysqld
      // Role=mysqld , Service=MySQLCluster, Cluster=cluster
      final String ROLE = "mysqld";
      List<NodesTableItem> results;
      try {
         String host = roleEjb.findRoles(cluster, service, ROLE).get(0).getHostname();
         WebCommunication wc = new WebCommunication();      
         results = wc.getNdbinfoNodesTable(host);
      } catch(Exception ex) {
         results = new ArrayList<NodesTableItem>();
      }
      return results;
   }

   public void upload() {
      if (file != null) {
         FacesMessage msg;
         boolean result = mysql.restore(file);
         if (result) {
            msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Data restored sucessfully.");
         } else {
            msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Failure", "Restore failed. Try again.");
         }
         FacesContext.getCurrentInstance().addMessage(null, msg);
      }

   }
}