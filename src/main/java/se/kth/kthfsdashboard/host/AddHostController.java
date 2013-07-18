package se.kth.kthfsdashboard.host;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.context.RequestContext;
import se.kth.kthfsdashboard.command.CommandEJB;
import se.kth.kthfsdashboard.role.RoleEJB;

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
   private RoleEJB roleEjb;
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

      System.err.println("HERE");
      if (hostEJB.hostExists(hostId)) {

         System.err.println("Exists");
         RequestContext reqInstace = RequestContext.getCurrentInstance();
         reqInstace.addCallbackParam("exists", true);


         FacesContext context = FacesContext.getCurrentInstance();
         FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                 "Host Exists", "A host with id " + hostId + " already exists.");
         context.addMessage(null, msg);



      } else {
      }


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