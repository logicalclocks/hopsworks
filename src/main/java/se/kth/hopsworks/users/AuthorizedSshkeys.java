/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.hopsworks.users;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;


@Entity
@Table(name = "hopsworks.authorized_sshkeys")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "AuthorizedSshkeys.findAll", query = "SELECT a FROM AuthorizedSshkeys a"),
  @NamedQuery(name
      = "AuthorizedSshkeys.findByProject", query
      = "SELECT a FROM AuthorizedSshkeys a WHERE a.authorizedSshkeysPK.project = :project"),
  @NamedQuery(name
      = "AuthorizedSshkeys.findByUser", query
      = "SELECT a FROM AuthorizedSshkeys a WHERE a.authorizedSshkeysPK.user = :user"),
  @NamedQuery(name
      = "AuthorizedSshkeys.findBySshkeyName", query
      = "SELECT a FROM AuthorizedSshkeys a WHERE a.authorizedSshkeysPK.sshkeyName = :sshkeyName")})
public class AuthorizedSshkeys implements Serializable {
  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected AuthorizedSshkeysPK authorizedSshkeysPK;

  public AuthorizedSshkeys() {
  }

  public AuthorizedSshkeys(AuthorizedSshkeysPK authorizedSshkeysPK) {
    this.authorizedSshkeysPK = authorizedSshkeysPK;
  }

  public AuthorizedSshkeys(String project, String user, String sshkeyName) {
    this.authorizedSshkeysPK = new AuthorizedSshkeysPK(project, user, sshkeyName);
  }

  public AuthorizedSshkeysPK getAuthorizedSshkeysPK() {
    return authorizedSshkeysPK;
  }

  public void setAuthorizedSshkeysPK(AuthorizedSshkeysPK authorizedSshkeysPK) {
    this.authorizedSshkeysPK = authorizedSshkeysPK;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (authorizedSshkeysPK != null ? authorizedSshkeysPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof AuthorizedSshkeys)) {
      return false;
    }
    AuthorizedSshkeys other = (AuthorizedSshkeys) object;
    if ((this.authorizedSshkeysPK == null && other.authorizedSshkeysPK != null) || (this.authorizedSshkeysPK != null && !this.authorizedSshkeysPK.equals(other.authorizedSshkeysPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.users.AuthorizedSshkeys[ authorizedSshkeysPK=" + authorizedSshkeysPK + " ]";
  }

}
