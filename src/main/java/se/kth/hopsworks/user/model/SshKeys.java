/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.hopsworks.user.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;


@Entity
@Table(name = "hopsworks.ssh_keys")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "SshKeys.findByUid", query = "SELECT s FROM SshKeys s WHERE s.sshKeysPK.uid = :uid")})
public class SshKeys implements Serializable {
  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected SshKeysPK sshKeysPK;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 2000)
  @Column(name = "public_key")
  private String publicKey;

  public SshKeys() {
  }

  public SshKeys(SshKeysPK sshKeysPK) {
    this.sshKeysPK = sshKeysPK;
  }

  public SshKeys(SshKeysPK sshKeysPK, String publicKey) {
    this.sshKeysPK = sshKeysPK;
    this.publicKey = publicKey;
  }

  public SshKeys(int uid, String name) {
    this.sshKeysPK = new SshKeysPK(uid, name);
  }

  public SshKeysPK getSshKeysPK() {
    return sshKeysPK;
  }

  public void setSshKeysPK(SshKeysPK sshKeysPK) {
    this.sshKeysPK = sshKeysPK;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (sshKeysPK != null ? sshKeysPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof SshKeys)) {
      return false;
    }
    SshKeys other = (SshKeys) object;
    if ((this.sshKeysPK == null && other.sshKeysPK != null) || (this.sshKeysPK != null && !this.sshKeysPK.equals(other.sshKeysPK))) {
      return false;
    }
    
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.user.model.SshKeys[ sshKeysPK=" + sshKeysPK + " ]";
  }

}
