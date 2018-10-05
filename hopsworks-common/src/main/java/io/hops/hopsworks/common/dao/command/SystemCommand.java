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

package io.hops.hopsworks.common.dao.command;

import io.hops.hopsworks.common.dao.host.Hosts;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
  
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "command", fetch = FetchType.LAZY)
  private List<SystemCommandArguments> commandArguments;
  
  public SystemCommand(Hosts host, SystemCommandFacade.OP op) {
    this.status = SystemCommandFacade.STATUS.NEW;
    this.priority = 0;
    this.host = host;
    this.op = op;
  }
  
  public SystemCommand() {
    this.status = SystemCommandFacade.STATUS.NEW;
    this.priority = 0;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
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
  
  public List<SystemCommandArguments> getCommandArguments() {
    return commandArguments;
  }
  
  public void setCommandArguments(List<SystemCommandArguments> commandArguments) {
    this.commandArguments = commandArguments;
  }
  
  public void setCommandArgumentsAsString(String commandArguments) {
    if (commandArguments != null) {
      String[] tokens = getSplitArguments(commandArguments, SystemCommandArguments.ARGUMENT_COLUMN_LENGTH);
      List<SystemCommandArguments> args = new ArrayList<>(tokens.length);
      for (String arg : tokens) {
        SystemCommandArguments sca = new SystemCommandArguments(arg);
        sca.setCommand(this);
        args.add(sca);
      }
      setCommandArguments(args);
    }
  }
  
  public String getCommandArgumentsAsString() {
    StringBuilder sb = new StringBuilder();
    if (commandArguments != null) {
      for (SystemCommandArguments sca : commandArguments) {
        sb.append(sca.getArguments());
      }
    }
    return sb.toString();
  }
  
  private String[] getSplitArguments(String arguments, int columnSize) {
    int buckets = arguments.length() / columnSize;
    if (arguments.length() % columnSize != 0) {
      buckets++;
    }
    
    String[] tokens = new String[buckets];
    int head = 0;
    int tail = arguments.length() > columnSize ? columnSize : arguments.length();
    for (int i = 0; i < buckets; i++) {
      tokens[i] = arguments.substring(head, tail);
      head = tail;
      tail = tail + columnSize < arguments.length() ? tail + columnSize : arguments.length();
    }
    return tokens;
  }
  
  @Override
  public String toString() {
    return "SystemCommand with ID " + id + " OP: " + op + " STATUS: " + status;
  }
}
