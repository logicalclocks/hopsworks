package se.kth.bbc.activity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * Entity class mapped to the view "activity_details". Used to present the
 * activity in a complete way to the user in the activity log.
 * <p>
 * @author stig
 */
@Entity
@Table(name = "vangelis_kthfs.activity_details")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ActivityDetail.findAll",
          query = "SELECT a FROM ActivityDetail a ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.findById",
          query = "SELECT a FROM ActivityDetail a WHERE a.id = :id"),
  @NamedQuery(name = "ActivityDetail.findByPerformedByEmail",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.performedByEmail = :performedByEmail ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.countByPerformedByEmail",
          query
          = "SELECT COUNT(a) FROM ActivityDetail a WHERE a.performedByEmail = :performedByEmail "),
  @NamedQuery(name = "ActivityDetail.findByPerformedByName",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.performedByName = :performedByName ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.findByDescription",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.description = :description ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.findByProjectname",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.projectname = :projectname ORDER BY a.timestamp DESC"),
  @NamedQuery(name = "ActivityDetail.findByTimestamp",
          query
          = "SELECT a FROM ActivityDetail a WHERE a.timestamp = :timestamp ORDER BY a.timestamp DESC")})
public class ActivityDetail implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private int id;
  @Size(max = 255)
  @Column(name = "performed_by_email")
  private String performedByEmail;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "performed_by_name")
  private String performedByName;
  @Size(max = 128)
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "projectname")
  private String projectname;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;

  public ActivityDetail() {
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getPerformedByEmail() {
    return performedByEmail;
  }

  public void setPerformedByEmail(String performedByEmail) {
    this.performedByEmail = performedByEmail;
  }

  public String getPerformedByName() {
    return performedByName;
  }

  public void setPerformedByName(String performedByName) {
    this.performedByName = performedByName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getProjectname() {
    return projectname;
  }

  public void setProjectname(String projectname) {
    this.projectname = projectname;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

}
