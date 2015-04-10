package se.kth.bbc.study;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Entity class mapping to the view StudyDetails. It represents details for
 * a study that are always presented together in the studies box.
 * <p>
 * @author stig
 */
@Entity
@Table(name = "study_details")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "StudyDetail.findAll",
          query = "SELECT s FROM StudyDetail s"),
  @NamedQuery(name = "StudyDetail.findByStudyName",
          query = "SELECT s FROM StudyDetail s WHERE s.studyName = :studyName"),
  @NamedQuery(name = "StudyDetail.findByEmail",
          query = "SELECT s FROM StudyDetail s WHERE s.email = :email"),
  @NamedQuery(name = "StudyDetail.findByCreator",
          query = "SELECT s FROM StudyDetail s WHERE s.creator = :creator")})
public class StudyDetail implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "studyName")
  private String studyName;
  // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "email")
  private String email;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "creator")
  private String creator;

  public StudyDetail() {
  }

  public StudyDetail(String studyName, String email, String creatorName) {
    this.creator = creatorName;
    this.studyName = studyName;
    this.email = email;
  }

  public String getStudyName() {
    return studyName;
  }

  public void setStudyName(String studyName) {
    this.studyName = studyName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

}
