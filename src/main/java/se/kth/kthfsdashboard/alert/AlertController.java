package se.kth.kthfsdashboard.alert;

import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
//@ViewScoped
public class AlertController implements Serializable{

   @EJB
   private AlertEJB alertEJB;
   @ManagedProperty("#{param.hostname}")
   private String hostname;
   @ManagedProperty("#{param.role}")
   private String role;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   private Alert[] selectedAlerts;

   public AlertController() {
   }

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }

   public String getService() {
      return service;
   }

   public void setService(String service) {
      this.service = service;
   }

   public String getHostname() {
      return hostname;
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getCluster() {
      return cluster;
   }

   public List<Alert> getAlerts() {
      List<Alert> alert = alertEJB.findAll();
      return alert;
   }

   public Alert[] getSelectedAlerts() {
      return selectedAlerts;
   }

   public void setSelectedAlerts(Alert[] alerts) {
      selectedAlerts = alerts;
   }

   public void deleteSelectedAlerts() {
      for (Alert alert : selectedAlerts) {
         alertEJB.removeAlert(alert);
      }
      informAlertsDeleted(selectedAlerts.length + " alert(s) deleted." );
   }
   
   public void deleteAllAlerts() {
      alertEJB.removeAllAlerts();
      informAlertsDeleted("All alerts deleted.");
   }   
   
   private void informAlertsDeleted(String msg) {
      FacesContext context = FacesContext.getCurrentInstance();
      context.addMessage(null, new FacesMessage("Successful", msg));            
   }
}
