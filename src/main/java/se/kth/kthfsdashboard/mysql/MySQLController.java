package se.kth.kthfsdashboard.mysql;

import java.io.IOException;
import java.io.Serializable;
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

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class MySQLController implements Serializable {

   @EJB
   RoleEJB roleEjb;
   @ManagedProperty("#{param.servicegroup}")
   private String serviceGroup;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   private boolean flag;
   private List<NodesTableItem> info;
   private UploadedFile file;
   MySQLAccess mysql = new MySQLAccess();

   public MySQLController() {
   }

   public String getServiceGroup() {
      return serviceGroup;
   }

   public void setServiceGroup(String serviceGroup) {
      this.serviceGroup = serviceGroup;
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
         Logger.getLogger(MySQLController.class.getName()).log(Level.SEVERE, null, ex);
      }

      FacesContext.getCurrentInstance().addMessage(null, msg);
   }

   private List<NodesTableItem> loadItems() {
      MySQLAccess dao = new MySQLAccess();
      try {
         // Finds hostname of mysqld
         // Role=mysqld , Service=MySQLCluster, Cluster=cluster
         final String ROLE = "mysqld";
         String host = roleEjb.findRoles(cluster, serviceGroup, ROLE).get(0).getHostname();
         return dao.readNodesFromNdbinfo(host);
      } catch (Exception e) {
         return null;
      }
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