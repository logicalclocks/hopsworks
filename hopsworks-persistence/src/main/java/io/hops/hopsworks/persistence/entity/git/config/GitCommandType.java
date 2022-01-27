/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.git.config;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum GitCommandType {
  @XmlEnumValue("add_remote")
  ADD_REMOTE("add_remote"),
  @XmlEnumValue("checkout")
  CHECKOUT("checkout"),
  @XmlEnumValue("clone")
  CLONE("clone"),
  @XmlEnumValue("create_branch")
  CREATE_BRANCH("create_branch"),
  @XmlEnumValue("create_repository")
  CREATE_REPOSITORY("create_repository"),
  @XmlEnumValue("commit")
  COMMIT("commit"),
  @XmlEnumValue("delete_branch")
  DELETE_BRANCH("delete_branch"),
  @XmlEnumValue("delete_remote")
  DELETE_REMOTE("delete_remote"),
  @XmlEnumValue("push")
  PUSH("push"),
  @XmlEnumValue("pull")
  PULL("pull"),
  @XmlEnumValue("status")
  STATUS("status"),
  @XmlEnumValue("file_checkout")
  FILE_CHECKOUT("file_checkout");

  private final String gitCommand;

  GitCommandType(String gitCommand) { this.gitCommand = gitCommand; }

  public static GitCommandType fromString(String name) {
    return valueOf(name.toUpperCase());
  }

  public String getGitCommand() {
    return this.gitCommand;
  }

  @Override
  public String toString() {
    return gitCommand;
  }
}
