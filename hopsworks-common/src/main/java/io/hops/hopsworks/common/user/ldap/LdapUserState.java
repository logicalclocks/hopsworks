package io.hops.hopsworks.common.user.ldap;

import io.hops.hopsworks.common.dao.user.ldap.LdapUser;
import io.hops.hopsworks.common.dao.user.ldap.LdapUserDTO;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LdapUserState {
  
  private boolean saved;
  private LdapUser ldapUser;
  private LdapUserDTO userDTO;

  public LdapUserState(LdapUser ldapUser) {
    this.ldapUser = ldapUser;
  }

  public LdapUserState(boolean saved, LdapUser ldapUser) {
    this.saved = saved;
    this.ldapUser = ldapUser;
  }

  public LdapUserState(boolean saved, LdapUser ldapUser, LdapUserDTO userDTO) {
    this.saved = saved;
    this.ldapUser = ldapUser;
    this.userDTO = userDTO;
  }

  public boolean isSaved() {
    return saved;
  }

  public void setSaved(boolean saved) {
    this.saved = saved;
  }

  public LdapUser getLdapUser() {
    return ldapUser;
  }

  public void setLdapUser(LdapUser ldapUser) {
    this.ldapUser = ldapUser;
  }

  public LdapUserDTO getUserDTO() {
    return userDTO;
  }

  public void setUserDTO(LdapUserDTO userDTO) {
    this.userDTO = userDTO;
  }
  
}
