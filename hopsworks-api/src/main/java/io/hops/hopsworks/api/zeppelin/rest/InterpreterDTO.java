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

package io.hops.hopsworks.api.zeppelin.rest;

import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.zeppelin.interpreter.InterpreterSetting;

@XmlRootElement
public class InterpreterDTO {

  private String id;
  private String name;
  private String group;
  private List<LivyMsg.Session> sessions;
  private boolean notRunning;
  private boolean defaultInterpreter;

  public InterpreterDTO() {
  }

  public InterpreterDTO(String id, String name, String group, boolean notRunning) {
    this.id = id;
    this.name = name;
    this.group = group;
    this.notRunning = notRunning;
    sessions = new ArrayList<>();
  }

  public InterpreterDTO(InterpreterSetting interpreter, boolean notRunning) {
    this.id = interpreter.getId();
    this.name = interpreter.getName();
    this.group = null;
    this.notRunning = notRunning;
    sessions = new ArrayList<>();
  }

  public InterpreterDTO(InterpreterSetting interpreter, boolean notRunning,
      List<LivyMsg.Session> runningLivySessions) {
    this.id = interpreter.getId();
    this.name = interpreter.getName();
    this.group = null;
    this.notRunning = notRunning;
    sessions = runningLivySessions;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public List<LivyMsg.Session> getSessions() {
    return sessions;
  }

  public void setSessions(List<LivyMsg.Session> sessions) {
    this.sessions = sessions;
  }

  public boolean isNotRunning() {
    return notRunning;
  }

  public void setNotRunning(boolean notRunning) {
    this.notRunning = notRunning;
  }

  public boolean isDefaultInterpreter() {
    return defaultInterpreter;
  }

  public void setDefaultInterpreter(boolean defaultInterpreter) {
    this.defaultInterpreter = defaultInterpreter;
  }

}
