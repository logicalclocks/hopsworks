/*
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
 */

package io.hops.hopsworks.common.dao.command;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "system_commands_args",
        catalog = "hopsworks")
public class SystemCommandArguments implements Serializable {
  private static final long serialVersionUID = 1L;
  // It should be the same as in MySQL. Consider max row size of ndb
  public static final int ARGUMENT_COLUMN_LENGTH = 13900;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  
  @ManyToOne
  @JoinColumn(name = "command_id", referencedColumnName = "id", nullable = false)
  private SystemCommand command;
  
  private String arguments;
  
  public SystemCommandArguments() {}
  
  public SystemCommandArguments(String arguments) {
    this.arguments = arguments;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public SystemCommand getCommand() {
    return command;
  }
  
  public void setCommand(SystemCommand command) {
    this.command = command;
  }
  
  public String getArguments() {
    return arguments;
  }
  
  public void setArguments(String arguments) {
    this.arguments = arguments;
  }
}
