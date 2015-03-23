package se.kth.kthfsdashboard.user;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import javax.xml.bind.DatatypeConverter;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@Entity
@Table(name = "USERS")
//@SqlResultSetMappings( { @SqlResultSetMapping(name = "activityList", entities = {
//    @EntityResult(entityClass = UserActivity.class), @EntityResult(entityClass = Username.class) })
//})
//@XmlRootElement
@Cacheable(false)
@NamedQueries({
  @NamedQuery(name = "Username.findAll",
          query = "SELECT r FROM Username r"),
  @NamedQuery(name = "Username.findAllByName",
          query = "SELECT r.name FROM Username r"),
  @NamedQuery(name = "Username.findByEmail",
          query = "SELECT r FROM Username r WHERE r.email = :email"),
  @NamedQuery(name = "Username.findAllByStatus",
          query = "SELECT r FROM Username r WHERE r.status = :status"),
  @NamedQuery(name = "Username.findByPassword",
          query = "SELECT r FROM Username r WHERE r.password = :password"),
  @NamedQuery(name = "Username.findByName",
          query = "SELECT r.email FROM Username r")})
public class Username implements Serializable {

  //Constants to reflect the request status of a user. At creation, status is set to request.
  public static final int STATUS_REQUEST = -1;
  public static final int STATUS_ALLOW = 0;

  // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "EMAIL")
  private String email;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "MOBILENUM")
  private String mobileNum;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "NAME")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "PASSWORD")
  private String password;
  @Basic(optional = false)
  @NotNull
  @Column(name = "REGISTEREDON")
  @Temporal(TemporalType.TIMESTAMP)
  private Date registeredOn;
  @Basic(optional = false)
  @NotNull
  @Lob
  @Column(name = "SALT")
  private byte[] salt;
  @Basic(optional = false)
  @NotNull
  @Column(name = "STATUS")
  private int status;

  @Transient
  private Group extraGroup;

  private static final long serialVersionUID = 1L;
  // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation

  @ElementCollection(targetClass = Group.class)
  @CollectionTable(name = "USERS_GROUPS",
          joinColumns = @JoinColumn(name = "email",
                  nullable = false),
          uniqueConstraints = {
            @UniqueConstraint(columnNames = {"email", "groupname"})})
  @Enumerated(EnumType.STRING)
  @Column(name = "groupname",
          length = 64,
          nullable = false)
  private List<Group> groups;

  public Username() {
  }

  public Username(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public Username(UserDTO dto) {

    if (dto.getPassword1() == null || dto.getPassword1().length() == 0
            || dto.getPassword1().compareTo(dto.getPassword2()) != 0) {
      throw new IllegalArgumentException(
              "Password 1 and Password 2 have to be equal (typo?)");
    }
    this.email = dto.getEmail();
    this.name = dto.getEmail();
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
    //this.password = DigestUtils.sha512Hex(password);
    this.password = DatatypeConverter.printHexBinary(password.getBytes());
  }

  public boolean encodePassword() {
    if (password == null) {
      return false;
    }
//      try {
    //Random r = new Random(System.currentTimeMillis());
    Random r = new SecureRandom();
    salt = new byte[8];
    r.nextBytes(salt);
//            MessageDigest digest = MessageDigest.getInstance("SHA-1");
//            digest.reset();
//            digest.update(salt);
//            this.password = digest.digest(password.getBytes("UTF-8")).toString();
    setSalt(salt);
//          String passwordInHex = String.format("%0128x", new BigInteger(1, password.getBytes(Charset.defaultCharset())));
//          if (passwordInHex != null) {
//              this.setPassword(passwordInHex);
//          } else {
//              return false;
//          }
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

  public String getMobileNum() {
    return mobileNum;
  }

  public void setMobileNum(String mobileNum) {
    this.mobileNum = mobileNum;
  }

  @Override
  public String toString() {
    return "kthfs.User[ email=" + email + " ]";
  }

  public Username(String email) {
    this.email = email;
  }

  public Username(String email, String mobilenum, String name, String password,
          Date registeredon, byte[] salt, int status) {
    this.email = email;
    this.mobileNum = mobilenum;
    this.name = name;
    this.password = password;
    this.registeredOn = registeredon;
    this.salt = salt;
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (email != null ? email.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Username)) {
      return false;
    }
    Username other = (Username) object;
    if ((this.email == null && other.email != null) || (this.email != null
            && !this.email.equals(other.email))) {
      return false;
    }
    return true;
  }

  public void addGroup(Group g) {
    if (groups == null || groups.isEmpty()) {
      groups = new ArrayList<>();
    }
    if (!groups.contains(g)) {
      groups.add(g);
    }
  }

  /*
   * Derived property to allow adding groups with dropdown list in frontend.
   */
  public void setExtraGroup(Group g) {
    addGroup(g);
    this.extraGroup = g;
  }

  public Group getExtraGroup() {
    return extraGroup;
  }
}
