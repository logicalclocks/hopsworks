package se.kth.bbc.activity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.study.Study;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "activity")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "UserActivity.findAll",
          query = "SELECT u FROM UserActivity u ORDER BY u.timestamp DESC"),
  @NamedQuery(name = "UserActivity.findById",
          query = "SELECT u FROM UserActivity u WHERE u.id = :id"),
  @NamedQuery(name = "UserActivity.findByFlag",
          query = "SELECT u FROM UserActivity u WHERE u.flag = :flag"),
  @NamedQuery(name = "UserActivity.findByActivity",
          query = "SELECT u FROM UserActivity u WHERE u.activity = :activity"),
  @NamedQuery(name = "UserActivity.findByUser",
          query
          = "SELECT u FROM UserActivity u WHERE u.user = :user"),
  @NamedQuery(name = "UserActivity.findByTimestamp",
          query = "SELECT u FROM UserActivity u WHERE u.timestamp = :timestamp"),
  @NamedQuery(name = "UserActivity.findByStudy",
          query
          = "SELECT u FROM UserActivity u WHERE u.study = :study ORDER BY u.timestamp DESC"),
  @NamedQuery(name = "UserActivity.countAll",
          query = "SELECT COUNT(u) FROM UserActivity u"),
  @NamedQuery(name = "UserActivity.countPerStudy",
          query
          = "SELECT COUNT(u) FROM UserActivity u WHERE u.study = :study")})
public class UserActivity implements Serializable {
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 5)
  @Column(name = "flag")
  private String flag;
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Size(max = 128)
  @Column(name = "activity")
  private String activity;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;
  @JoinColumn(name = "study_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Study study;
  @JoinColumn(name = "user_id",
          referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private User user;
  
  public UserActivity() {
  }

  public UserActivity(Integer id) {
    this.id = id;
  }

  public UserActivity(Integer id, Date timestamp) {
    this.id = id;
    this.timestamp = timestamp;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getActivity() {
    return activity;
  }

  public void setActivity(String activity) {
    this.activity = activity;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof UserActivity)) {
      return false;
    }
    UserActivity other = (UserActivity) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.activity.UserActivity[ id=" + id + " ]";
  }

  public String getFlag() {
    return flag;
  }

  public void setFlag(String flag) {
    this.flag = flag;
  }

  public Study getStudy() {
    return study;
  }

  public void setStudy(Study study) {
    this.study = study;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

}
