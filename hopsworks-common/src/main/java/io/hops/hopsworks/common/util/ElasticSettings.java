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


import java.util.ArrayList;
import java.util.List;

public class ElasticSettings {
  public final static String ELASTIC_IP_DEFAULT="127.0.0.1";
  public final static int ELASTIC_PORT_DEFAULT = 9300;
  public final static int ELASTIC_REST_PORT_DEFAULT = 9200;
  public final static boolean ELASTIC_OPENDISTRO_SECURTIY_ENABLED_DEFAULT = false;
  public final static boolean ELASTIC_HTTPS_ENABLED_DEFAULT = false;
  public final static String ELASTIC_ADMIN_USER_DEFAULT="admin";
  public final static String ELASTIC_ADMIN_PASSWORD_DEFAULT="adminpw";
  public final static boolean ELASTIC_JWT_ENABLED_DEFAULT= false;
  public final static String ELASTIC_JWT_URL_PARAMETER_DEFAULT ="jt";
  public final static int ELASTIC_JWT_EXP_MS_DEFAULT = 10 * 60 * 1000;
  
  public final static String ELASTIC_ADMIN_ROLE = "admin";
  
  private final List<String> elasticIps;
  private final int elasticPort;
  private final int elasticRestPort;
  private final boolean openDistroEnabled;
  private final boolean httpsEnabled;
  private final String adminUser;
  private final String adminPassword;
  private final boolean elasticJWTEnabled;
  private final String elasticJWTURLParameter;
  private final int elasticJWTExpMs;
  
  private int rrIndex = 0;
  
  public ElasticSettings(String elasticIps, int elasticPort,
      int elasticRestPort, boolean openDistroSecurityEnabled,
      boolean httpsEnabled, String adminUser, String adminPassword,
      boolean elasticJWTEnabled, String elasticJWTURLParameter, int elasticJWTExpMs){
    String[] ips = elasticIps.split(",");
    List<String> validIps = new ArrayList<>();
    for (String ip : ips) {
      if (Ip.validIp(ip)) {
        validIps.add(ip);
      }
    }
    this.elasticIps = validIps;
    this.elasticPort = elasticPort;
    this.elasticRestPort = elasticRestPort;
    this.openDistroEnabled = openDistroSecurityEnabled;
    this.httpsEnabled = httpsEnabled;
    this.adminUser = adminUser;
    this.adminPassword = adminPassword;
    this.elasticJWTEnabled = elasticJWTEnabled;
    this.elasticJWTURLParameter = elasticJWTURLParameter;
    this.elasticJWTExpMs = elasticJWTExpMs;
  }
  
  public List<String> getElasticIps() {
    return elasticIps;
  }
  
  public String getElasticEndpoint() {
    return getElasticIp() + ":" + elasticPort;
  }
  
  public String getElasticRESTEndpoint() {
    return (isHttpsEnabled() ? "https" :
        "http") + "://" + getElasticIp() + ":" + elasticRestPort;
  }
  
  public int getElasticPort() {
    return elasticPort;
  }
  
  public int getElasticRESTPort() {
    return elasticRestPort;
  }
  
  private String getElasticIp(){
    if(rrIndex == elasticIps.size()){
      rrIndex = 0;
    }
    return elasticIps.get(rrIndex++);
  }
  
  public boolean isOpenDistroSecurityEnabled() {
    return openDistroEnabled;
  }
  
  public boolean isHttpsEnabled() {
    return httpsEnabled;
  }
  
  public String getAdminUser() {
    return adminUser;
  }
  
  public String getAdminPassword() {
    return adminPassword;
  }
  
  public boolean isElasticJWTEnabled() {
    return elasticJWTEnabled;
  }
  
  public String getElasticJWTURLParameter() {
    return elasticJWTURLParameter;
  }
  
  public int getElasticJWTExpMs() {
    return elasticJWTExpMs;
  }
}
