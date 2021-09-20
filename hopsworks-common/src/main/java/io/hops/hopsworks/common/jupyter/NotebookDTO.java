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

package io.hops.hopsworks.common.jupyter;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NotebookDTO extends RestDTO<NotebookDTO> {

  private String path;
  private String jupyterConfiguration;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getJupyterConfiguration() {
    return jupyterConfiguration;
  }

  public void setJupyterConfiguration(String jupyterConfiguration) {
    this.jupyterConfiguration = jupyterConfiguration;
  }
}