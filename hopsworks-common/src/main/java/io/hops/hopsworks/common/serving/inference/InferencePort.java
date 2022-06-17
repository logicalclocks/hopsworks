/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.serving.inference;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InferencePort {
  
  private InferencePortName name;
  private Integer number;
  
  public InferencePort() { }
  
  public InferencePort(InferencePortName name, Integer number) {
    this.name = name;
    this.number = number;
  }
  
  public InferencePortName getName() {
    return name;
  }
  public void setName(InferencePortName name) { this.name = name; }
  
  public Integer getNumber() {
    return number;
  }
  public void setNumber(Integer number) { this.number = number; }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof InferencePort) {
      InferencePort port = (InferencePort) obj;
      return name.equals(port.name) && number.equals(port.number);
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return (name + number.toString()).hashCode();
  }
  
  
  public enum InferencePortName {
    HTTP("http"),
    HTTPS("https"),
    STATUS("status-port"),
    TLS("tls");
    
    private final String value;
  
    InferencePortName(String value) {
      this.value = value;
    }
    
    public static InferencePortName of(String value) {
      switch (value) {
        case "http2":
        case "http":
          return HTTP;
        case "https":
          return HTTPS;
        case "status-port":
          return STATUS;
        case "tls":
          return TLS;
        default:
          throw new IllegalArgumentException("Port name '" + value + "' not known");
      }
    }
    
    @Override
    public String toString() {
      return value;
    }
  }
}
