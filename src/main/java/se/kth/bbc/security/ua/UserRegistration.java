package se.kth.bbc.security.ua;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.apache.commons.codec.binary.Base32;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.security.auth.totp.Gauth;
import se.kth.bbc.security.auth.totp.QRCodeGenerator;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */

@ManagedBean
@SessionScoped
public class UserRegistration implements Serializable {

    private static final Logger logger = Logger.getLogger(UserRegistration.class.getName());

    @EJB
    private UserManager mgr;

    private String fname;
    private String lname;
    private String username;
    private String mail;
    private String mobile;
    private String org;
    private String orcid="";
    
    private String title= "";
    private String password;
    private String passwordAgain;
    private String qrUrl = "Pass";
    private StreamedContent qrCode;

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

    public String register() throws UnsupportedEncodingException, NoSuchAlgorithmException, IOException {

      if (!password.equals(passwordAgain)) {
            FacesMessage msg = new FacesMessage("Password do not match");
            FacesContext.getCurrentInstance().addMessage("form:password0", msg);
            return (null);
        }

         try {
             /* generates a UNIX compliant account*/
            int uid = mgr.lastUserID() + 1;
            String sec = calculateSecretKey();
            username = mgr.register(fname, lname, mail, title, org, tel, orcid, uid,converToSHA256(password), sec);
            
            mgr.registerGroup(uid, GroupsIf.BBC_GUEST);
            qrCode = QRCodeGenerator.getQRCode(username, Gauth.ISSUER, sec);
            fname =""; lname= ""; mail =""; title=""; org=""; tel=""; orcid="";
 
        } catch (Exception e) {
            logger.info("Error " + e.getMessage());
            return null;
        }
        return ("qrcode");
    }

    private String calculateSecretKey() {
        byte[] secretKey= new byte[10];
        new SecureRandom().nextBytes(secretKey);
        Base32 codec = new Base32();
        byte[] encodedKey = codec.encode(secretKey);
        return new String(encodedKey);
    }
    
    public static String converToSHA256(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data.getBytes("UTF-8"));
        return bytesToHex(md.digest());
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes) {
            result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}