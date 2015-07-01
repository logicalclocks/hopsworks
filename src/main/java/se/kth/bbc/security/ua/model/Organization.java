package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "vangelis_kthfs.organization")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Organization.findAll",
          query = "SELECT o FROM Organization o"),
  @NamedQuery(name = "Organization.findById",
          query = "SELECT o FROM Organization o WHERE o.id = :id"),
  @NamedQuery(name = "Organization.findByOrgName",
          query = "SELECT o FROM Organization o WHERE o.orgName = :orgName"),
  @NamedQuery(name = "Organization.findByWebsite",
          query = "SELECT o FROM Organization o WHERE o.website = :website"),
  @NamedQuery(name = "Organization.findByContactPerson",
          query
          = "SELECT o FROM Organization o WHERE o.contactPerson = :contactPerson"),
  @NamedQuery(name = "Organization.findByContactEmail",
          query
          = "SELECT o FROM Organization o WHERE o.contactEmail = :contactEmail"),
  @NamedQuery(name = "Organization.findByDepartment",
          query
          = "SELECT o FROM Organization o WHERE o.department = :department"),
  @NamedQuery(name = "Organization.findByPhone",
          query = "SELECT o FROM Organization o WHERE o.phone = :phone"),
  @NamedQuery(name = "Organization.findByFax",
          query = "SELECT o FROM Organization o WHERE o.fax = :fax")})
public class Organization implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Size(max = 100)
  @Column(name = "org_name")
  private String orgName;
  @Size(max = 200)
  @Column(name = "website")
  private String website;
  @Size(max = 100)
  @Column(name = "contact_person")
  private String contactPerson;
  @Size(max = 100)
  @Column(name = "contact_email")
  private String contactEmail;
  @Size(max = 100)
  @Column(name = "department")
  private String department;
  // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax number consider using this annotation to enforce field validation
  @Size(max = 20)
  @Column(name = "phone")
  private String phone;
  // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax number consider using this annotation to enforce field validation
  @Size(max = 20)
  @Column(name = "fax")
  private String fax;
  @JoinColumn(name = "uid",
          referencedColumnName = "uid")
  @OneToOne(optional = false)
  private User uid;

  public Organization() {
  }

  public Organization(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public void setContactPerson(String contactPerson) {
    this.contactPerson = contactPerson;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getFax() {
    return fax;
  }

  public void setFax(String fax) {
    this.fax = fax;
  }

  @XmlTransient
  @JsonIgnore
  public User getUid() {
    return uid;
  }

  public void setUid(User uid) {
    this.uid = uid;
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
    if (!(object instanceof Organization)) {
      return false;
    }
    Organization other = (Organization) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.Organization[ id=" + id + " ]";
  }

}
