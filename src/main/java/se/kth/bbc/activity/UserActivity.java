/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.activity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "activity")
@XmlRootElement
//@SqlResultSetMappings( { @SqlResultSetMapping(name = "activityList", entities = {
//    @EntityResult(entityClass = UserActivity.class), @EntityResult(entityClass = Username.class) })
//})
//@NamedNativeQuery(name="nativeActivity", query="SELECT act.id, act.activity, act.performed_by, act.activity_on, u.EMAIL, u.NAME FROM activity act, USERS u WHERE act.performed_By = u.EMAIL", resultSetMapping = "allActivityList")
//@SqlResultSetMapping(name="allActivityList", entities = {@EntityResult(entityClass = UserActivity.class, fields = {
//                                                                                     @FieldResult(name="id", column = "id"),
//                                                                                     @FieldResult(name="activity", column = "activity"),
//                                                                                     @FieldResult(name="performed_by",column = "performed_by"),
//                                                                                     @FieldResult(name="timestamp",column = "timestamp"),
//                                                                                     @FieldResult(name="activity_on",column = "activity_on")}),
//    
//                                                         @EntityResult(entityClass = Username.class, fields = {
//                                                                                     @FieldResult(name="name",column = "NAME"),
//                                                                                     @FieldResult(name="email",column = "EMAIL")}) 
//                                                                                     })
@NamedQueries({
    @NamedQuery(name = "UserActivity.findAll", query = "SELECT u FROM UserActivity u ORDER BY u.timestamp DESC"),
    @NamedQuery(name = "UserActivity.findById", query = "SELECT u FROM UserActivity u WHERE u.id = :id"),
    @NamedQuery(name = "UserActivity.findByFlag", query = "SELECT u FROM UserActivity u WHERE u.flag = :flag"),
    @NamedQuery(name = "UserActivity.findByActivity", query = "SELECT u FROM UserActivity u WHERE u.activity = :activity"),
    @NamedQuery(name = "UserActivity.findByPerformedBy", query = "SELECT u FROM UserActivity u WHERE u.performedBy = :performedBy"),
    @NamedQuery(name = "UserActivity.findByTimestamp", query = "SELECT u FROM UserActivity u WHERE u.timestamp = :timestamp"),
    @NamedQuery(name = "UserActivity.findByActivityOn", query = "SELECT u FROM UserActivity u WHERE u.activityOn = :activityOn ORDER BY u.timestamp DESC"),
    @NamedQuery(name = "UserActivity.countAll", query = "SELECT COUNT(u) FROM UserActivity u"),
    @NamedQuery(name = "UserActivity.countStudy", query = "SELECT COUNT(u) FROM UserActivity u WHERE u.activityOn = :studyName")})
public class UserActivity implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "activity_on")
    private String activityOn;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 5)
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
    @Size(max = 255)
    @Column(name = "performed_By")
    private String performedBy;
    @Basic(optional = false)
    @NotNull
    @Column(name = "timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

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

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserActivity)) {
            return false;
        }
        UserActivity other = (UserActivity) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
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

    public String getActivityOn() {
        return activityOn;
    }

    public void setActivityOn(String activityOn) {
        this.activityOn = activityOn;
    }
    
}
