/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.kmon.struct;

import java.io.Serializable;

public class HostInfo implements Serializable {

  private String name;
  private String ip;
  private String rack;

  public HostInfo(String name, String ip, String rack) {

    this.name = name;
    this.ip = ip;
    this.rack = rack;
  }

  public String getName() {
    return name;
  }

  public String getIp() {
    return ip;
  }

  public String getRack() {
    return rack;
  }

}
