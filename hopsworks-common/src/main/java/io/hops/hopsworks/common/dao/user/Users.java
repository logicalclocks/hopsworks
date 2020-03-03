/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.user;

import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.user.security.Address;
import io.hops.hopsworks.common.dao.user.security.Organization;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import io.hops.hopsworks.common.user.ValidationKeyType;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "hopsworks.users")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Users.findAll",
      query = "SELECT u FROM Users u")
  ,
  @NamedQuery(name = "Users.findByUid",
      query = "SELECT u FROM Users u WHERE u.uid = :uid")
  ,
  @NamedQuery(name = "Users.findByUsername",
      query = "SELECT u FROM Users u WHERE u.username = :username")
  ,
  @NamedQuery(name = "Users.findByPassword",
      query = "SELECT u FROM Users u WHERE u.password = :password")
  ,
  @NamedQuery(name = "Users.findByEmail",
      query = "SELECT u FROM Users u WHERE u.email = :email")
  ,
  @NamedQuery(name = "Users.findByFname",
      query = "SELECT u FROM Users u WHERE u.fname = :fname")
  ,
  @NamedQuery(name = "Users.findByLname",
      query = "SELECT u FROM Users u WHERE u.lname = :lname")
  ,
  @NamedQuery(name = "Users.findByActivated",
      query = "SELECT u FROM Users u WHERE u.activated = :activated")
  ,
  @NamedQuery(name = "Users.findByTitle",
      query = "SELECT u FROM Users u WHERE u.title = :title")
  ,
  @NamedQuery(name = "Users.findByOrcid",
      query = "SELECT u FROM Users u WHERE u.orcid = :orcid")
  ,
  @NamedQuery(name = "Users.findByFalseLogin",
      query = "SELECT u FROM Users u WHERE u.falseLogin = :falseLogin")
  ,
  @NamedQuery(name = "Users.findByIsonline",
      query = "SELECT u FROM Users u WHERE u.isonline = :isonline")
  ,
  @NamedQuery(name = "Users.findBySecret",
      query = "SELECT u FROM Users u WHERE u.secret = :secret")
  ,
  @NamedQuery(name = "Users.findByValidationKey",
      query = "SELECT u FROM Users u WHERE u.validationKey = :validationKey")
  ,
  @NamedQuery(name = "Users.findBySecurityQuestion",
      query
      = "SELECT u FROM Users u WHERE u.securityQuestion = :securityQuestion")
  ,
  @NamedQuery(name = "Users.findBySecurityAnswer",
      query
      = "SELECT u FROM Users u WHERE u.securityAnswer = :securityAnswer")
  ,
  @NamedQuery(name = "Users.findByMode",
      query = "SELECT u FROM Users u WHERE u.mode = :mode")
  ,
  @NamedQuery(name = "Users.findByPasswordChanged",
      query
      = "SELECT u FROM Users u WHERE u.passwordChanged = :passwordChanged")
  ,
  @NamedQuery(name = "Users.findByNotes",
      query = "SELECT u FROM Users u WHERE u.notes = :notes")
  ,
  @NamedQuery(name = "Users.findByMobile",
      query = "SELECT u FROM Users u WHERE u.mobile = :mobile")
  ,
  @NamedQuery(name = "Users.findByStatus",
      query = "SELECT u FROM Users u WHERE u.status = :status")
  ,
  @NamedQuery(name = "Users.findByStatusAndMode",
      query = "SELECT u FROM Users u WHERE u.status = :status and u.mode = :mode")
  ,
  @NamedQuery(name = "Users.findByTwoFactor",
      query
      = "SELECT u FROM Users u WHERE u.twoFactor = :twoFactor")
  ,
  @NamedQuery(name = "Users.findAllInGroup",
      query
      = "SELECT u.uid FROM Users u WHERE u.bbcGroupCollection IN :roles")})
public class Users implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "uid")
  private Integer uid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 10)
  @Column(name = "username")
  private String username;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 128)
  @Column(name = "password")
  private String password;
  // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)
  //*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?",
  //message="Invalid email")//if the field contains email address consider using 
  //this annotation to enforce field validation
  @Size(max = 254)
  @Column(name = "email")
  private String email;
  @Size(max = 30)
  @Column(name = "fname")
  private String fname;
  @Size(max = 30)
  @Column(name = "lname")
  private String lname;
  @Basic(optional = false)
  @NotNull
  @Column(name = "activated")
  @Temporal(TemporalType.TIMESTAMP)
  private Date activated;
  @Size(max = 10)
  @Column(name = "title")
  private String title;
  @Size(max = 20)
  @Column(name = "orcid")
  private String orcid;
  @Basic(optional = false)
  @NotNull
  @Column(name = "false_login")
  private int falseLogin;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "status")
  private UserAccountStatus status;
  @Basic(optional = false)
  @NotNull
  @Column(name = "isonline")
  private int isonline;
  @Size(max = 20)
  @Column(name = "secret")
  private String secret;
  @Size(max = 128)
  @Column(name = "validation_key")
  private String validationKey;
  @Column(name = "validation_key_updated")
  @Temporal(TemporalType.TIMESTAMP)
  private Date validationKeyUpdated;
  @Enumerated(EnumType.STRING)
  @Column(name = "validation_key_type")
  private ValidationKeyType validationKeyType;
  @Enumerated(EnumType.STRING)
  @Column(name = "security_question")
  private SecurityQuestion securityQuestion;
  @Size(max = 128)
  @Column(name = "security_answer")
  private String securityAnswer;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "mode")
  private UserAccountType mode;
  @Basic(optional = false)
  @NotNull
  @Column(name = "password_changed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date passwordChanged;
  @Size(max = 500)
  @Column(name = "notes")
  private String notes;
  @Size(max = 15)
  @Column(name = "mobile")
  private String mobile;
  @Basic(optional = false)
  @Column(name = "max_num_projects")
  private Integer maxNumProjects;
  @Basic(optional = false)
  @Column(name = "num_created_projects")
  @NotNull
  private Integer numCreatedProjects = 0;
  @Basic(optional = false)
  @Column(name = "num_active_projects")
  @NotNull
  private Integer numActiveProjects = 0;
  @Basic(optional = false)
  @NotNull
  @Column(name = "two_factor")
  private boolean twoFactor;
  @Basic(optional = false)
  @NotNull
  @Column(name = "salt")
  private String salt;
  @Basic(optional = false)
  @NotNull
  @Column(name = "tours_state")
  private int toursState;

  @JoinTable(name = "hopsworks.user_group",
      joinColumns = {
        @JoinColumn(name = "uid",
            referencedColumnName = "uid")},
      inverseJoinColumns = {
        @JoinColumn(name = "gid",
            referencedColumnName = "gid")})
  @ManyToMany
  private Collection<BbcGroup> bbcGroupCollection;

  @OneToOne(cascade = CascadeType.ALL,
      mappedBy = "uid")
  private Address address;

  @OneToOne(cascade = CascadeType.ALL,
      mappedBy = "uid")
  private Organization organization;

  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "users")
  private Collection<JupyterSettings> jupyterSettingsCollection;

  @OneToMany(cascade = CascadeType.ALL,
      mappedBy = "users")
  private Collection<TensorBoard> tensorBoardCollection;
  
  //Only used for adding group by name to the used
  @Transient
  private String groupName;
  //Only used for viewing user groups as list
  @Transient
  private List<BbcGroup> groupNames;

  public Users() {
  }

  public Users(Integer uid, String username, String password, Date activated,
      int falseLogin, UserAccountStatus status, int isonline, int maxNumProjects, int numCreatedProjects,
      int numActiveProjects) {
    this.uid = uid;
    this.username = username;
    this.password = password;
    this.activated = activated;
    this.falseLogin = falseLogin;
    this.isonline = isonline;
    this.status = status;
    this.maxNumProjects = maxNumProjects;
    this.numCreatedProjects = numCreatedProjects;
    this.numActiveProjects  = numActiveProjects;
  }

  public Users(Integer uid) {
    this.uid = uid;
  }

  public Users(Integer uid, String username, String password, Date activated,
      int falseLogin, int isonline, UserAccountType mode,
      Date passwordChanged, UserAccountStatus status, int maxNumProjects) {
    this.uid = uid;
    this.username = username;
    this.password = password;
    this.activated = activated;
    this.falseLogin = falseLogin;
    this.isonline = isonline;
    this.mode = mode;
    this.passwordChanged = passwordChanged;
    this.status = status;
    this.maxNumProjects = maxNumProjects;
    this.numCreatedProjects = 0;
    this.numActiveProjects = 0;
  }

  public Users(String username, String password, String email, String fname, String lname, Date activated, String title,
    String orcid, UserAccountStatus status, String secret, String validationKey, Date validationKeyUpdated,
    ValidationKeyType validationKeyType, SecurityQuestion securityQuestion, String securityAnswer, UserAccountType mode,
    Date passwordChanged, String mobile, Integer maxNumProjects, boolean twoFactor, String salt, int toursState) {
    this.username = username;
    this.password = password;
    this.email = email;
    this.fname = fname;
    this.lname = lname;
    this.activated = activated;
    this.title = title;
    this.orcid = orcid;
    this.status = status;
    this.secret = secret;
    this.validationKey = validationKey;
    this.validationKeyUpdated = validationKeyUpdated;
    this.validationKeyType = validationKeyType;
    this.securityQuestion = securityQuestion;
    this.securityAnswer = securityAnswer;
    this.mode = mode;
    this.passwordChanged = passwordChanged;
    this.mobile = mobile;
    this.maxNumProjects = maxNumProjects;
    this.twoFactor = twoFactor;
    this.salt = salt;
    this.toursState = toursState;
    this.numCreatedProjects = 0;
    this.numActiveProjects = 0;
  }
  
  public Users(String username, String password, String email, String fname, String lname, Date activated,
    String title, String orcid, UserAccountStatus status, UserAccountType mode, Date passwordChanged,
    Integer maxNumProjects, String salt) {
    this.username = username;
    this.password = password;
    this.email = email;
    this.fname = fname;
    this.lname = lname;
    this.activated = activated;
    this.title = title;
    this.orcid = orcid;
    this.status = status;
    this.mode = mode;
    this.passwordChanged = passwordChanged;
    this.maxNumProjects = maxNumProjects;
    this.salt = salt;
    this.numCreatedProjects = 0;
    this.numActiveProjects = 0;
  }

  public Integer getUid() {
    return uid;
  }

  public void setUid(Integer uid) {
    this.uid = uid;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @XmlTransient
  @JsonIgnore
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
  
  @XmlTransient
  @JsonIgnore
  public String getSalt() {
    return salt;
  }

  public void setSalt(String salt) {
    this.salt = salt;
  }
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
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

  @XmlTransient
  @JsonIgnore
  public Date getActivated() {
    return activated;
  }

  public void setActivated(Date activated) {
    this.activated = activated;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @XmlTransient
  @JsonIgnore
  public String getOrcid() {
    return orcid;
  }

  public void setOrcid(String orcid) {
    this.orcid = orcid;
  }

  @XmlTransient
  @JsonIgnore
  public int getFalseLogin() {
    return falseLogin;
  }

  public void setFalseLogin(int falseLogin) {
    this.falseLogin = falseLogin;
  }

  public int getIsonline() {
    return isonline;
  }

  public void setIsonline(int isonline) {
    this.isonline = isonline;
  }

  @XmlTransient
  @JsonIgnore
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  @XmlTransient
  @JsonIgnore
  public String getValidationKey() {
    return validationKey;
  }

  public void setValidationKey(String validationKey) {
    this.validationKey = validationKey;
  }
  @XmlTransient
  @JsonIgnore
  public Date getValidationKeyUpdated() {
    return validationKeyUpdated;
  }
  
  public void setValidationKeyUpdated(Date validationKeyUpdated) {
    this.validationKeyUpdated = validationKeyUpdated;
  }
  @XmlTransient
  @JsonIgnore
  public ValidationKeyType getValidationKeyType() {
    return validationKeyType;
  }
  
  public void setValidationKeyType(ValidationKeyType validationKeyType) {
    this.validationKeyType = validationKeyType;
  }

  @XmlTransient
  @JsonIgnore
  public SecurityQuestion getSecurityQuestion() {
    return securityQuestion;
  }

  public void setSecurityQuestion(SecurityQuestion securityQuestion) {
    this.securityQuestion = securityQuestion;
  }

  @XmlTransient
  @JsonIgnore
  public String getSecurityAnswer() {
    return securityAnswer;
  }

  public void setSecurityAnswer(String securityAnswer) {
    this.securityAnswer = securityAnswer;
  }

  public UserAccountType getMode() {
    return mode;
  }

  public void setMode(UserAccountType mode) {
    this.mode = mode;
  }

  public Date getPasswordChanged() {
    return passwordChanged;
  }

  public void setPasswordChanged(Date passwordChanged) {
    this.passwordChanged = passwordChanged;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public UserAccountStatus getStatus() {
    return status;
  }

  public String getStatusName() {
    return status.name();
  }

  public void setStatus(UserAccountStatus status) {
    this.status = status;
  }

  public Collection<BbcGroup> getBbcGroupCollection() {
    return bbcGroupCollection;
  }

  public void setBbcGroupCollection(Collection<BbcGroup> bbcGroupCollection) {
    this.bbcGroupCollection = bbcGroupCollection;
  }

  public void setMaxNumProjects(Integer maxNumProjects) {
    this.maxNumProjects = maxNumProjects;
  }

  public Integer getMaxNumProjects() {
    return maxNumProjects;
  }

  public Integer getNumCreatedProjects() {
    return numCreatedProjects;
  }

  public void setNumCreatedProjects(Integer numCreatedProjects) {
    this.numCreatedProjects = numCreatedProjects;
  }

  public Integer getNumActiveProjects() {
    return numCreatedProjects;
  }

  public void setNumActiveProjects(Integer numActiveProjects) {
    this.numActiveProjects = numActiveProjects;
  }

  @XmlTransient
  @JsonIgnore
  public boolean getTwoFactor() {
    return twoFactor;
  }

  public void setTwoFactor(boolean twoFactor) {
    this.twoFactor = twoFactor;
  }

  public int getToursState() {
    return toursState;
  }

  public void setToursState(int toursState) {
    this.toursState = toursState;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TensorBoard> getTensorBoardCollection() {
    return tensorBoardCollection;
  }

  public void setTensorBoardCollection(
      Collection<TensorBoard> tensorBoardCollection) {
    this.tensorBoardCollection = tensorBoardCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<JupyterSettings> getJupyterSettingsCollection() {
    return jupyterSettingsCollection;
  }

  public void setJupyterSettingsCollection(
      Collection<JupyterSettings> jupyterSettingsCollection) {
    this.jupyterSettingsCollection = jupyterSettingsCollection;
  }
  
  public String getGroupName() {
    return groupName;
  }
  
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }
  
  public List<BbcGroup> getGroupNames() {
    this.groupNames = new ArrayList<>(this.bbcGroupCollection);
    return groupNames;
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (uid != null ? uid.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Users)) {
      return false;
    }
    Users other = (Users) object;
    if ((this.uid == null && other.uid != null) || (this.uid != null
        && !this.uid.equals(other.uid))) {
      return false;
    }
    return true;
  }

  public Users asUser() {
    return new Users(uid, username, password, activated, falseLogin, status,
        isonline, maxNumProjects, numCreatedProjects, numActiveProjects);
  }
  
  @Override
  public String toString() {
    return "Users{uid=" + uid + '}';
  }
}
