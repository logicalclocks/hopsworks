package se.kth.kthfsdashboard.host;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.context.RequestContext;
import se.kth.kthfsdashboard.command.CommandEJB;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class AddHostController implements Serializable {

   @EJB
   private HostEJB hostEJB;
   @EJB
   private CommandEJB commandEJB;
   private String hostId;
   private String privateIp;
   private String publicIp;
   private static final Logger logger = Logger.getLogger(AddHostController.class.getName());

   public AddHostController() {
      logger.info("AddHostController");
   }

   public void addHost(ActionEvent actionEvent) {
         FacesContext context = FacesContext.getCurrentInstance();
         FacesMessage msg;
      if (hostEJB.hostExists(hostId)) {
         logger.log(Level.INFO, "Host with id {0} already exists.", hostId);
         msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Host Exists", 
                 "A host with id " + hostId + " already exists.");
         context.addMessage(null, msg);    
      } else {
         Host host = new Host();
         host.setHostId(hostId);
         host.setPrivateIp(privateIp);
         host.setPublicIp(publicIp);
         host.setHostname("");
         hostEJB.storeHost(host, true);
         RequestContext reqInstace = RequestContext.getCurrentInstance();
         reqInstace.addCallbackParam("hostadded", true);
         msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Host Added", 
                 "Host " + hostId + " added successfully.");
         context.addMessage(null, msg);
         resetValues();
      }
   }
   
   private void resetValues() {
      hostId = "";
      privateIp = "";
      publicIp = "";
   }

   public String getHostId() {
      return hostId;
   }

   public void setHostId(String hostId) {
      this.hostId = hostId;
   }

   public String getPrivateIp() {
      return privateIp;
   }

   public void setPrivateIp(String privateIp) {
      this.privateIp = privateIp;
   }

   public String getPublicIp() {
      return publicIp;
   }

   public void setPublicIp(String publicIp) {
      this.publicIp = publicIp;
   }
}