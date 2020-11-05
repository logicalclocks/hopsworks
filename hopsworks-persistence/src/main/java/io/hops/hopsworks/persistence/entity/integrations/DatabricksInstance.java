/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.integrations;

import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "databricks_instance", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "DatabricksInstance.findAllByUser",
        query = "SELECT t FROM DatabricksInstance t WHERE :user = t.user"),
    @NamedQuery(name = "DatabricksInstance.findAllByUserAndUrl",
        query = "SELECT t FROM DatabricksInstance t WHERE :user = t.user AND :url = t.url ")})
public class DatabricksInstance implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "url")
  private String url;

  @NotNull
  @JoinColumns({
      @JoinColumn(name = "uid", referencedColumnName = "uid", updatable = false, insertable = false),
      @JoinColumn(name = "secret_name", referencedColumnName = "secret_name")})
  private Secret secret;

  @NotNull
  @JoinColumn(name = "uid", referencedColumnName = "uid")
  private Users user;

  public DatabricksInstance() {
  }

  public DatabricksInstance(String url, Secret secret, Users user) {
    this.url = url;
    this.secret = secret;
    this.user = user;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Secret getSecret() {
    return secret;
  }

  public void setSecret(Secret secret) {
    this.secret = secret;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }
}
