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
package io.hops.hopsworks.persistence.entity.git;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

// Signature is used to identify who and when created a commit or tag.
// For now we will use the Users name and email
@XmlRootElement
public class CommitterSignature implements Serializable {
  private static final long serialVersionUID = 1L;
  private String name;
  private String email;

  public CommitterSignature() {}

  public CommitterSignature(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public String getName() { return name; }

  public void setName(String name) { this.name = name; }

  public String getEmail() { return email; }

  public void setEmail(String email) { this.email = email; }

}
