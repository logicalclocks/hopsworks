package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.Yubikey;
import se.kth.hopsworks.user.model.Users;

 
@ManagedBean
@ViewScoped
public class YubikeyActivator implements Serializable{

  private static final long serialVersionUID = 1L;
  
   private static final Logger logger = Logger.getLogger(YubikeyActivator.class.
      getName());

  @EJB
  private UserManager userManager;

  @EJB
  private AuditManager auditManager;

  @EJB
  private EmailBean emailBean;

  @Resource
  private UserTransaction userTransaction;

  private Users user;

  private Address address;

  // Yubikey public id. 12 chars: vviehlefjvcb
  private String pubid;

  

  // e.g. f1bda8c978766d50c25d48d72ed516e0
  private String secret;
  // to remove an existing group
   private String sgroup;
  
  // for yubikey administration page
  private Users selectedYubikyUser;
  

  public String getSgroup() {
    return sgroup;
  }

  public void setSgroup(String sgroup) {
    this.sgroup = sgroup;
  }

  
  public Users getSelectedYubikyUser() {
    return selectedYubikyUser;
  }

  public void setSelectedYubikyUser(Users selectedYubikyUser) {
    this.selectedYubikyUser = selectedYubikyUser;
  }

  @PostConstruct
   public void init(){
   
    this.selectedYubikyUser = (Users) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("yUser");
    this.address = this.selectedYubikyUser.getAddress();
    
    actGroups = new ArrayList<>();  
    
    // dont include BBCADMIN and BBCUSER roles for approving accounts as they are perstudy
    for (BBCGroup value : BBCGroup.values()) {
      if(value!= BBCGroup.BBC_GUEST && value!= BBCGroup.BBC_USER){
          actGroups.add(value.name());
        }
    }
   }

   
  // user activation groups to exclude guest and bbcuser
  List<String> actGroups;
  
  
  public List<String> getActGroups() {
    
    return actGroups;
  }

  public void setActGroups(List<String> actGroups) {
    this.actGroups = actGroups;
  }

   

  public String activateYubikey() throws MessagingException {
    
    

    try {
      // parse the creds  1486433,vviehlefjvcb,01ec8ce3dea6,f1bda8c978766d50c25d48d72ed516e0,,2014-12-14T23:16:09,
        
      if (this.selectedYubikyUser.getMode()
              != PeopleAccountStatus.YUBIKEY_USER.getValue()) {
        MessagesController.addSecurityErrorMessage(user.getEmail()
                + " is not a Yubikey user");
        return "";
      }

      Yubikey yubi = this.selectedYubikyUser.getYubikey();

      // Trim the input
      yubi.setPublicId(pubid.replaceAll("\\s", ""));
      yubi.setAesSecret(secret.replaceAll("\\s", ""));

      // Update the info
      yubi.setCreated(new Date());
      yubi.setAccessed(new Date());
      yubi.setCounter(0);
      yubi.setSessionUse(0);
      yubi.setHigh(0);
      yubi.setLow(0);

      userTransaction.begin();
        
      if (this.selectedYubikyUser.getStatus()
              == PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue() && 
              this.selectedYubikyUser.getYubikey().getStatus()!= PeopleAccountStatus.YUBIKEY_LOST.getValue() ) {
        // Set stauts to active
        yubi.setStatus(PeopleAccountStatus.ACCOUNT_ACTIVE.getValue());

        userManager.updateYubikey(yubi);
        if (!"#".equals(this.sgroup.trim()) &&( this.sgroup!=null || !this.sgroup.isEmpty())) {
          userManager.registerGroup(this.selectedYubikyUser, BBCGroup.valueOf(
                  this.sgroup).getValue());
        }else{
          MessagesController.addSecurityErrorMessage(" Role could not be granted.");
          return ("");
        }
      } 
      
      // for lost yubikey devices there is no need for role assignment
      if (this.selectedYubikyUser.getStatus()
              == PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue() && 
              this.selectedYubikyUser.getYubikey().getStatus()== PeopleAccountStatus.YUBIKEY_LOST.getValue() ) {
        
        // Set stauts to active
        yubi.setStatus(PeopleAccountStatus.ACCOUNT_ACTIVE.getValue());

        userManager.updateYubikey(yubi);
      } 
      

      userManager.updateStatus(this.selectedYubikyUser,
              PeopleAccountStatus.ACCOUNT_ACTIVE.getValue());
      userTransaction.commit();

      
      emailBean.sendEmail(this.selectedYubikyUser.getEmail(),
              UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
              UserAccountsEmailMessages.
              yubikeyAccountActivatedMessage(this.selectedYubikyUser.getEmail()));
      
      // Update the user management GUI
     // yRequests.remove(this.selectedYubikyUser);

    } catch (NotSupportedException | SystemException | RollbackException |
            HeuristicMixedException | HeuristicRollbackException |
            SecurityException | IllegalStateException ex) {
      MessagesController.addSecurityErrorMessage("Technical Error!");
      return ("");
    }

    return "print_address";
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }
  
  
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getPubid() {
    return pubid;
  }

  public void setPubid(String pubid) {
    this.pubid = pubid;
  }

  public String getsgroup() {
    return sgroup;
  }

  public void setsgroup(String sgroup) {
    this.sgroup = sgroup;
  }
  
}
