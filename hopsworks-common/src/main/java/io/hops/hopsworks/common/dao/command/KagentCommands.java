package io.hops.hopsworks.common.dao.command;
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

import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlRootElement
public class KagentCommands implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private List<SystemCommand> systemCommands;
  private List<CondaCommands> condaCommands;
  
  public KagentCommands() {
  }
  
  public KagentCommands(List<SystemCommand> systemCommands, List<CondaCommands> condaCommands) {
    this.systemCommands = systemCommands;
    this.condaCommands = condaCommands;
  }
  
  public List<SystemCommand> getSystemCommands() {
    return systemCommands;
  }
  
  public void setSystemCommands(List<SystemCommand> systemCommands) {
    this.systemCommands = systemCommands;
  }
  
  public List<CondaCommands> getCondaCommands() {
    return condaCommands;
  }
  
  public void setCondaCommands(List<CondaCommands> condaCommands) {
    this.condaCommands = condaCommands;
  }
}
