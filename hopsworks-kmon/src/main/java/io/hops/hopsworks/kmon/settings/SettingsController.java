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

package io.hops.hopsworks.kmon.settings;

import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import io.hops.hopsworks.common.dao.host.Status;

@ManagedBean
@SessionScoped
public class SettingsController implements Serializable {

  private static final Logger logger = Logger.getLogger(
          SettingsController.class.getName());
  private String name;
  private int logLines;

  public SettingsController() {
    logger.info("SettingsController");
    name = "Hopsworks";
    logLines = 2;
  }

  public String getName() {
    return name;
  }

  public String tooltip(String id, int n) {

    if (id.equals(Status.TimedOut.toString())) {
      return n > 1 ? n
              + " services have been timed out (No heartbeat from the host)"
              : "1 service has been timed out (No heartbeat from the host)";
    }
    if (id.equals(Status.Stopped.toString())) {
      return n > 1 ? n + " services are not running" : "1 service is not running";
    }
    if (id.equals(Status.Started.toString())) {
      return n > 1 ? n + " services are running" : "1 service is running";
    }
    return "";
  }

  public String tooltip(String id) {

    if (id.equals(Status.TimedOut.toString())) {
      return "The service instance has been timed out: no heartbeat from the host";
    }
    if (id.equals(Status.Stopped.toString())) {
      return "service is not running";
    }
    if (id.equals(Status.Started.toString())) {
      return "service is running";
    }
    return "";
  }

  public String tooltipDisabledAction(String action) {
    if (action.equalsIgnoreCase("start")) {
      return "You may only start a stopped service";
    }
    if (action.equalsIgnoreCase("stop")) {
      return "You may only stop a started service";
    }
    return "";
  }

}
