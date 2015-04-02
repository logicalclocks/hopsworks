/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.activity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author jdowling
 */
@Embeddable
public class UsersGroupsPK implements Serializable {

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
          max = 64)
  @Column(name = "groupname")
  private String groupname;

  public UsersGroupsPK() {
  }

  public UsersGroupsPK(String email, String groupname) {
    this.email = email;
    this.groupname = groupname;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getGroupname() {
    return groupname;
  }

  public void setGroupname(String groupname) {
    this.groupname = groupname;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (email != null ? email.hashCode() : 0);
    hash += (groupname != null ? groupname.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof UsersGroupsPK)) {
      return false;
    }
    UsersGroupsPK other = (UsersGroupsPK) object;
    if ((this.email == null && other.email != null) || (this.email != null
            && !this.email.equals(other.email))) {
      return false;
    }
    if ((this.groupname == null && other.groupname != null) || (this.groupname
            != null && !this.groupname.equals(other.groupname))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.activity.UsersGroupsPK[ email=" + email + ", groupname="
            + groupname + " ]";
  }

}
