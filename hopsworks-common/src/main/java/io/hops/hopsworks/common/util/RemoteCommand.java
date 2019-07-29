/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.util;

import java.nio.file.Path;
import java.util.Hashtable;

public class RemoteCommand {
  private final String host;
  private final String user;
  private final int port;
  private final String command;
  private final Path identity;
  private final Hashtable<String, String> sshConfig;
  private final int connectTimeout;
  private final int executionTimeout;
  
  private RemoteCommand(Builder builder) {
    this.host = builder.host;
    this.user = builder.user;
    this.port = builder.port;
    this.command = builder.command;
    this.identity = builder.identity;
    this.sshConfig = builder.sshConfig;
    this.connectTimeout = builder.connectTimeoutMS;
    this.executionTimeout = builder.executionTimeoutS;
  }
  
  public String getHost() {
    return host;
  }
  
  public String getUser() {
    return user;
  }
  
  public int getPort() {
    return port;
  }
  
  public String getCommand() {
    return command;
  }
  
  public Path getIdentity() {
    return identity;
  }
  
  public Hashtable<String, String> getSSHConfig() {
    return sshConfig;
  }
  
  public int getConnectTimeout() {
    return connectTimeout;
  }
  
  public int getExecutionTimeout() {
    return executionTimeout;
  }
  
  @Override
  public String toString() {
    return user + "@" + host + ":" + port + "/" + command;
  }
  
  public static class Builder{
    private String host;
    private String user;
    private int port = 22;
    private String command;
    private Path identity;
    private Hashtable<String, String> sshConfig;
    private int connectTimeoutMS = 10000;
    private int executionTimeoutS = 60;
    
    public Builder() {
      this.sshConfig = new Hashtable<>();
    }
    
    public Builder setHost(String host) {
      this.host = host;
      return this;
    }
    
    public Builder setUser(String user) {
      this.user = user;
      return this;
    }
    
    public Builder setPort(int port) {
      this.port = port;
      return this;
    }
    
    public Builder setCommand(String command) {
      this.command = command;
      return this;
    }
    
    public Builder setIdentity(Path identity) {
      this.identity = identity;
      return this;
    }
    
    public Builder addSSHConfig(String name, String value) {
      this.sshConfig.put(name, value);
      return this;
    }
    
    public Builder addSSHConfig(Hashtable<String, String> sshConfig) {
      this.sshConfig = sshConfig;
      return this;
    }
    
    public Builder setConnectTimeoutMS(int connectTimeoutMS) {
      this.connectTimeoutMS = connectTimeoutMS;
      return this;
    }
    
    public Builder setExecutionTimeoutS(int executionTimeoutS) {
      this.executionTimeoutS = executionTimeoutS;
      return this;
    }
    
    public RemoteCommand build() {
      checkForNullOrEmpty(host, "Host");
      checkForNullOrEmpty(user, "User");
      if (port < 1) {
        throw new IllegalArgumentException("Port cannot be less than 1");
      }
      checkForNullOrEmpty(command, "Command");
      checkForNull(identity, "Identity");
      checkForNull(sshConfig, "SSH config");
      if (connectTimeoutMS < 0) {
        throw new IllegalArgumentException("Connect timeout cannot be negative, use 0 to wait forever");
      }
      if (executionTimeoutS < 0) {
        throw new IllegalArgumentException("Execution timeout cannot be negative, use 0 to wait forever");
      }
      
      return new RemoteCommand(this);
    }
    
    private void checkForNullOrEmpty(String parameterValue, String parameterName) {
      checkForNull(parameterValue, parameterName);
      checkForEmpty(parameterValue, parameterName);
    }
    
    private <E> void checkForNull(E parameterValue, String parameterName) {
      if (parameterValue == null) {
        throw new IllegalArgumentException(parameterName + " cannot be null");
      }
    }
    
    private void checkForEmpty(String parameterValue, String parameterName) {
      if (parameterValue.isEmpty()) {
        throw new IllegalArgumentException(parameterName + " cannot be empty");
      }
    }
  }
}
