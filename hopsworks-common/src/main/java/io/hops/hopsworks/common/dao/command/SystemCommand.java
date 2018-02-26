/*
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
 *
 */

package io.hops.hopsworks.common.dao.command;

import io.hops.hopsworks.common.dao.host.Hosts;

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
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "system_commands",
        catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SystemCommand.findAll",
                query = "SELECT c FROM SystemCommand c"),
    @NamedQuery(name = "SystemCommand.findByHost",
                query = "SELECT C FROM SystemCommand c WHERE c.host = :host")
  })
public class SystemCommand implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  
  @JoinColumn(name = "host_id",
              referencedColumnName = "id",
              nullable = false)
  @ManyToOne(optional = false)
  private Hosts host;
  
  @Column(name = "op",
          nullable = false,
          length = 50)
  @Basic(optional = false)
  @Enumerated(EnumType.STRING)
  private SystemCommandFacade.OP op;
  
  @Column(name = "arguments")
  private String arguments;
  
  @Column(name = "status",
          nullable = false,
          length = 20)
  @Basic(optional = false)
  @Enumerated(EnumType.STRING)
  private SystemCommandFacade.STATUS status;
  
  @Column(name = "priority",
          nullable = false)
  @Basic(optional = false)
  private Integer priority;
  
  @Column(name = "exec_user",
          length = 50)
  private String execUser;
  
  public SystemCommand() {
    this.status = SystemCommandFacade.STATUS.ONGOING;
    this.priority = 0;
  }
  
  public Integer getId() {
    return id;
  }
  
  public Hosts getHost() {
    return host;
  }
  
  public void setHost(Hosts host) {
    this.host = host;
  }
  
  public SystemCommandFacade.OP getOp() {
    return op;
  }
  
  public void setOp(SystemCommandFacade.OP op) {
    this.op = op;
  }
  
  public String getArguments() {
    return arguments;
  }
  
  public void setArguments(String arguments) {
    this.arguments = arguments;
  }
  
  public SystemCommandFacade.STATUS getStatus() {
    return status;
  }
  
  public void setStatus(SystemCommandFacade.STATUS status) {
    this.status = status;
  }
  
  public Integer getPriority() {
    return priority;
  }
  
  public void setPriority(Integer priority) {
    this.priority = priority;
  }
  
  public String getExecUser() {
    return execUser;
  }
  
  public void setExecUser(String execUser) {
    this.execUser = execUser;
  }
  
  @Override
  public String toString() {
    return "SystemCommand with ID " + id + " OP: " + op + " STATUS: " + status;
  }
}
