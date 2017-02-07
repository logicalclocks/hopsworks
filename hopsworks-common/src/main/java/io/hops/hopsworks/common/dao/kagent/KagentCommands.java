package io.hops.hopsworks.common.dao.kagent;

import io.hops.hopsworks.common.dao.host.Host;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "kagent_commands",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "KagentCommands.findAll",
          query
          = "SELECT k FROM KagentCommands k"),
  @NamedQuery(name = "KagentCommands.findById",
          query
          = "SELECT k FROM KagentCommands k WHERE k.id = :id"),
  @NamedQuery(name = "KagentCommands.findByCommand",
          query
          = "SELECT k FROM KagentCommands k WHERE k.command = :command"),
  @NamedQuery(name = "KagentCommands.findByArg",
          query
          = "SELECT k FROM KagentCommands k WHERE k.arg = :arg"),
  @NamedQuery(name = "KagentCommands.findByCreated",
          query
          = "SELECT k FROM KagentCommands k WHERE k.created = :created")})
public class KagentCommands implements Serializable {

  public enum Op {
    UNINSTALL_ENV,
    CLONE_ENV,
    CREATE_ENV,
    UNINSTALL_LIB,
    INSTALL_LIB,
    UPGRADE_LIB
  };

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Column(name = "command")
  @Enumerated(EnumType.STRING)
  @Basic(optional = false)
  @NotNull
  private Op command;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "arg")
  private String arg;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @JoinColumn(name = "host_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Host hostId;

  public KagentCommands() {
  }

  public KagentCommands(Integer id) {
    this.id = id;
  }

  public KagentCommands(Integer id, Op command, String arg, Date created) {
    this.id = id;
    this.command = command;
    this.arg = arg;
    this.created = created;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Op getCommand() {
    return command;
  }

  public void setCommand(Op command) {
    this.command = command;
  }

  public String getArg() {
    return arg;
  }

  public void setArg(String arg) {
    this.arg = arg;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Host getHostId() {
    return hostId;
  }

  public void setHostId(Host hostId) {
    this.hostId = hostId;
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
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.kagent.KagentCommands[ id=" + id + " ]";
  }

}
