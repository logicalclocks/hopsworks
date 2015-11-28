package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
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
@SessionScoped
public class YubikeyActivator implements Serializable{

  private static final long serialVersionUID = 1L;
  

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
  
  @ManagedProperty("#{yUser}")
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
   
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String,String> params = fc.getExternalContext().getRequestParameterMap();
        this.selectedYubikyUser = userManager.findByEmail(params.get("yUser"));
   }

   
  // user activation groups to exclude guest and bbcuser
  List<String> actGroups;
  
  
  public List<String> getActGroups() {
    return actGroups;
  }

  public void setActGroups(List<String> actGroups) {
    this.actGroups = actGroups;
  }

   
  public String activateYubikeyUser() {
    this.address = this.selectedYubikyUser.getAddress();
    return "activate_yubikey";
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

      if (this.selectedYubikyUser.getYubikey().getStatus()
              == PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue()) {
        // Set stauts to active
        yubi.setStatus(PeopleAccountStatus.ACCOUNT_ACTIVE.getValue());

        userManager.updateYubikey(yubi);
        if ("#".equals(this.sgroup) && this.sgroup!= null && !this.sgroup.equals(BBCGroup.BBC_GUEST.name()) &&  !this.sgroup.equals(BBCGroup.BBC_USER.name())) {

          userManager.registerGroup(this.selectedYubikyUser, BBCGroup.valueOf(
                  this.sgroup).getValue());
        }else{
          MessagesController.addSecurityErrorMessage(this.sgroup +" granting role is not allowed.");
          return ("");
        }
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
