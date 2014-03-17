/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@Entity
@Table(name = "USERS")
//@XmlRootElement
@Cacheable(false)
@NamedQueries({
    @NamedQuery(name = "Username.findAll", query = "SELECT r FROM Username r"),
    @NamedQuery(name = "Username.findByEmail", query = "SELECT r FROM Username r WHERE r.email = :email"),
    @NamedQuery(name = "Username.findByUsername", query = "SELECT r FROM Username r WHERE r.username = :username"),
    @NamedQuery(name = "Username.findByPassword", query = "SELECT r FROM Username r WHERE r.password = :password"),
    @NamedQuery(name = "Username.findByName", query = "SELECT r FROM Username r WHERE r.name like \":name%\" ")})
public class Username implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 4, max = 255)
    @Column(nullable = false, length = 255)
    private String email;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String name;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 2, max = 255)
    @Column(nullable = false, length = 255)
    private String username;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(nullable = false, length = 128)
    private String password;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 8)
    @Column(nullable = false, length = 8)
    private byte[] salt;

    @Basic(optional = true)
    @NotNull
    @Size(min = 8, max = 32)
    @Column(nullable = false, length = 255)
    private String mobileNum;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Date registeredOn;
          
    @ElementCollection(targetClass = Group.class)
    @CollectionTable(name = "USERS_GROUPS", 
                    joinColumns       = @JoinColumn(name = "email", nullable=false), 
                    uniqueConstraints = { @UniqueConstraint(columnNames={"email","groupname"}) } )
    @Enumerated(EnumType.STRING)
    @Column(name="groupname", length=64, nullable=false)
    private List<Group> groups;
    
    
    public Username() {
    }


    public Username(UserDTO dto) {
        
    if (dto.getPassword1() == null || dto.getPassword1().length() == 0
                || dto.getPassword1().compareTo(dto.getPassword2()) != 0 ) {
            throw new IllegalArgumentException("Password 1 and Password 2 have to be equal (typo?)");
    }
        this.email = dto.getEmail();
        this.name = dto.getEmail();
        this.username = dto.getUsername();
        this.mobileNum = dto.getMobileNum();
        setPassword(dto.getPassword1());
        this.registeredOn = new Date();        
    }

    public Date getRegisteredOn() {
        return registeredOn;
    }

    public void setRegisteredOn(Date registeredOn) {
        this.registeredOn = registeredOn;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public byte[] getSalt() {
        return salt;
    }

  public void setSalt(byte[] salt) {
        this.salt = salt;
  }
 

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = DigestUtils.sha512Hex(password);
    }
    
    public boolean encodePassword() {
        if (password == null) {
            return false;
        }
//      try {
          Random r = new Random(System.currentTimeMillis());
          salt = new byte[8];
          r.nextBytes(salt);
//            MessageDigest digest = MessageDigest.getInstance("SHA-1");
//            digest.reset();
//            digest.update(salt);
//            this.password = digest.digest(password.getBytes("UTF-8")).toString();
          setSalt(salt);
          String passwordInHex = String.format("%040x", new BigInteger(1, password.getBytes(Charset.defaultCharset())));
          if (passwordInHex != null) {
              this.setPassword(passwordInHex);
          } else {
              return false;
          }
          return true;
//        } catch (UnsupportedEncodingException ex) {
//            Logger.getLogger(Username.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (NoSuchAlgorithmException ex) {
//            Logger.getLogger(Username.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String user) {
        this.username = user;
    }

    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (email != null ? email.hashCode() : 0);
        return hash;
    }

    public String getMobileNum() {
        return mobileNum;
    }

    public void setMobileNum(String mobileNum) {
        this.mobileNum = mobileNum;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Username)) {
            return false;
        }
        Username other = (Username) object;
        if ((this.email == null && other.email != null) || (this.email != null && !this.email.equals(other.email))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "kthfs.User[ email=" + email + " ]";
    }
}
