/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.hopsworks.common.dao.kagent;

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
 * @author jdowling
 */
@Entity
@Table(name = "kagent_commands", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "KagentCommands.findAll", query = "SELECT k FROM KagentCommands k"),
  @NamedQuery(name = "KagentCommands.findById", query = "SELECT k FROM KagentCommands k WHERE k.id = :id"),
  @NamedQuery(name = "KagentCommands.findByJsonCommand",
    query = "SELECT k FROM KagentCommands k WHERE k.jsonCommand = :jsonCommand"),
  @NamedQuery(name = "KagentCommands.findByCreated",
    query = "SELECT k FROM KagentCommands k WHERE k.created = :created")})
public class KagentCommands implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 1024)
  @Column(name = "json_command")
  private String jsonCommand;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  public KagentCommands() {
  }

  public KagentCommands(Integer id) {
    this.id = id;
  }

  public KagentCommands(Integer id, String jsonCommand, Date created) {
    this.id = id;
    this.jsonCommand = jsonCommand;
    this.created = created;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getJsonCommand() {
    return jsonCommand;
  }

  public void setJsonCommand(String jsonCommand) {
    this.jsonCommand = jsonCommand;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
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
    if (!(object instanceof KagentCommands)) {
      return false;
    }
    KagentCommands other = (KagentCommands) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.pysparkDeps.KagentCommands[ id=" + id + " ]";
  }

}
