package se.kth.bbc.security.ua;

import com.google.zxing.WriterException;
import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.FacesException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.apache.commons.codec.digest.DigestUtils;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.AccountsAuditActions;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.audit.AuditUtil;
import se.kth.bbc.security.audit.UserAuditActions;
import se.kth.bbc.security.auth.AuthenticationConstants;
import se.kth.bbc.security.auth.QRCodeGenerator;
import se.kth.hopsworks.user.model.Users;


@ManagedBean
@SessionScoped
public class UserRegistration implements Serializable {

  @EJB
  private UserManager mgr;

  @EJB
  private AuditManager am;

  @EJB
  private EmailBean emailBean;

  @Resource
  private UserTransaction userTransaction;

  private String fname;
  private String lname;
  private String username;
  private String mail;
  private String mobile;
  private String org;
  private String orcid;
  private SecurityQuestion security_question;
  private String security_answer;
  private String title;
  private String password;
  private String passwordAgain;
  private String address1;
  private String address2;
  private String address3;
  private String city;
  private String state;
  private String country;
  private String postalcode;
  private boolean tos;
  private String department;

  private int qrEnabled = -1;

  public int getQrEnabled() {
    return qrEnabled;
  }

  public void setQrEnabled(int qrEnabled) {
    this.qrEnabled = qrEnabled;
  }
  
  
  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public SecurityQuestion[] getQuestions() {
    return SecurityQuestion.values();
  }

  public boolean isTos() {
    return tos;
  }

  public void setTos(boolean tos) {
    this.tos = tos;
  }

  public String getAddress1() {
    return address1;
  }

  public void setAddress1(String address1) {
    this.address1 = address1;
  }

  public String getAddress2() {
    return address2;
  }

  public void setAddress2(String address2) {
    this.address2 = address2;
  }

  public String getAddress3() {
    return address3;
  }

  public void setAddress3(String address3) {
    this.address3 = address3;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getPostalcode() {
    return postalcode;
  }

  public void setPostalcode(String postalcode) {
    this.postalcode = postalcode;
  }

  public SecurityQuestion getSecurity_question() {
    return security_question;
  }

  public void setSecurity_question(SecurityQuestion security_question) {
    this.security_question = security_question;
  }

  public String getSecurity_answer() {
    return security_answer;
  }

  public void setSecurity_answer(String security_answer) {
    this.security_answer = security_answer;
  }
  // Quick response code URL
  private String qrUrl = "Pass";

  // To send the user the QR code image
  private StreamedContent qrCode = null;

  public StreamedContent getQrCode() {
    return qrCode;
  }

  public void setQrCode(StreamedContent qrCode) {
    this.qrCode = qrCode;
  }

  public UserManager getMgr() {
    return mgr;
  }

  public void setMgr(UserManager mgr) {
    this.mgr = mgr;
  }

  public String getQrUrl() {
    return qrUrl;
  }

  public String getFname() {
    return fname;
  }

  public void setFname(String fname) {
    this.fname = fname;
  }

  public String getLname() {
    return lname;
  }

  public void setLname(String lname) {
    this.lname = lname;
  }

  public void setQrUrl(String qrUrl) {
    this.qrUrl = qrUrl;
  }

  public String getTel() {
    return tel;
  }

  public void setTel(String tel) {
    this.tel = tel;
  }
  private String tel;

  public String getOrcid() {
    return orcid;
  }

  public void setOrcid(String orcid) {
    this.orcid = orcid;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMail() {
    return mail;
  }

  public void setMail(String mail) {
    this.mail = mail;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getOrg() {
    return org;
  }

  public void setOrg(String org) {
    this.org = org;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPasswordAgain() {
    return passwordAgain;
  }

  public void setPasswordAgain(String passwordAgain) {
    this.passwordAgain = passwordAgain;
  }

  /**
   * Register new mobile users.
   *
   * @return
   * @throws java.net.UnknownHostException
   * @throws java.net.SocketException
   */
  public String registerMobileUser() throws UnknownHostException,
          SocketException {
    String ip = AuditUtil.getIPAddress();
    String browser = AuditUtil.getBrowserInfo();
    String os = AuditUtil.getOSInfo();
    String macAddress = AuditUtil.getMacAddress(ip);
    Users user = null;
    qrCode = null;
    try {

      String otpSecret = SecurityUtils.calculateSecretKey();
      String activationKey = SecurityUtils.getRandomPassword(64);
				// 
      // Generates a UNIX compliant account
      int uid = mgr.lastUserID() + 1;

      // Register the new request in the platform
      userTransaction.begin();

      user = mgr.register(fname,
              lname,
              mail,
              title,
              tel,
              orcid,
              uid,
              DigestUtils.sha256Hex(password),
              otpSecret,
              security_question,
              DigestUtils.sha256Hex(security_answer),
              PeopleAccountStatus.ACCOUNT_VERIFICATION.getValue(),
              PeopleAccountStatus.MOBILE_USER.getValue(),
              activationKey);

      username = user.getUsername();

      // Register group
      mgr.registerGroup(user, BBCGroup.BBC_GUEST.getValue());

      // Create address entry
      mgr.registerAddress(user);

      mgr.registerOrg(user, org, department);

      // Generate qr code to be displayed to user
      qrCode = QRCodeGenerator.getQRCode(mail, AuthenticationConstants.ISSUER,
              otpSecret);

      am.registerLoginInfo(user, AccountsAuditActions.REGISTRATION.getValue(), ip,
              browser, os, macAddress, AccountsAuditActions.SUCCESS.name());

      userTransaction.commit();

      // Notify user about the request
      emailBean.sendEmail(mail,
              UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
              UserAccountsEmailMessages.buildMobileRequestMessage(
                      getApplicationUri(), user.getUsername() + activationKey));
      
      // Reset the values
      fname = "";
      lname = "";
      mail = "";
      title = "";
      org = "";
      department = "";
      tel = "";
      orcid = "";
      security_answer = "";
      security_question = null;
      password = "";
      passwordAgain = "";
      tos = false;
      qrEnabled = 1;
    } catch (NotSupportedException | SystemException |
            IOException | WriterException | MessagingException |
            RollbackException | HeuristicMixedException | 
            HeuristicRollbackException | SecurityException |
            IllegalStateException e) {
      MessagesController.addSecurityErrorMessage("Technical Error");

      am.registerLoginInfo(user, AccountsAuditActions.REGISTRATION.getValue(), ip,
              browser, os, macAddress, AccountsAuditActions.FAILED.name());

      return ("");

    }

    return ("qrcode");
  }

  /**
   * Register new Yubikey users.
   *
   * @return
   * @throws java.net.UnknownHostException
   * @throws java.net.SocketException
   */
  public String registerYubikey() throws UnknownHostException, SocketException {

    String ip = AuditUtil.getIPAddress();
    String browser = AuditUtil.getBrowserInfo();
    String os = AuditUtil.getOSInfo();
    String macAddress = AuditUtil.getMacAddress(ip);
    Users user = null;
    try {

      // Generates a UNIX compliant account
      int uid = mgr.lastUserID() + 1;

      String activationKey = SecurityUtils.getRandomPassword(64);

      // Register the request in the platform
      userTransaction.begin();

      user = mgr.register(fname,
              lname,
              mail,
              title,
              tel,
              orcid,
              uid,
              DigestUtils.sha256Hex(password),
              "-1",
              security_question, DigestUtils.sha256Hex(security_answer),
              PeopleAccountStatus.ACCOUNT_VERIFICATION.getValue(),
              PeopleAccountStatus.YUBIKEY_USER.getValue(),
              activationKey);

      mgr.registerGroup(user, BBCGroup.BBC_GUEST.getValue());

      mgr.registerAddress(user,
              address1,
              address2,
              address3,
              city,
              state,
              country,
              postalcode);
      mgr.registerOrg(user, org, department);

      mgr.registerYubikey(user);

      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.getValue(), AccountsAuditActions.SUCCESS.name(), "", user);

      // Send email to the user to get notified about the account request
      emailBean.sendEmail(mail,
              UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
              UserAccountsEmailMessages.buildYubikeyRequestMessage(
                      getApplicationUri(), user.getUsername() + activationKey));

      userTransaction.commit();

      // Reset the values
      fname = "";
      lname = "";
      mail = "";
      title = "";
      org = "";
      tel = "";
      orcid = "";
      security_answer = "";
      security_question = null;
      password = "";
      passwordAgain = "";
      address1 = "";
      address2 = "";
      address3 = "";
      city = "";
      state = "";
      country = "";
      postalcode = "";
      tos = false;
      department = "";

    } catch (NotSupportedException | SystemException | MessagingException |
            RollbackException | HeuristicMixedException | 
            HeuristicRollbackException | SecurityException |
            IllegalStateException e) {

      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.getValue(), UserAuditActions.FAILED.name() , "", user);

      MessagesController.addSecurityErrorMessage("Technical Error");
      return ("");
    }
    return ("yubico");
  }

  public String getApplicationUri() {
    try {
      FacesContext ctxt = FacesContext.getCurrentInstance();
      ExternalContext ext = ctxt.getExternalContext();
      URI uri = new URI(ext.getRequestScheme(),
              null, ext.getRequestServerName(), ext.getRequestServerPort(),
              ext.getRequestContextPath(), null, null);
      return uri.toASCIIString();
    } catch (URISyntaxException e) {
      throw new FacesException(e);
    }
  }
  
  public String returnMenu(){
  
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpSession sess = (HttpSession) ctx.getExternalContext().getSession(false);
    qrCode = null;
    if (null != sess) {
      sess.invalidate();
    }
    return ("welcome");
  
  }
}
