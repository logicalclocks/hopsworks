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

public class OpenSearchSettings {
  public final static String OPENSEARCH_IP_DEFAULT ="127.0.0.1";
  public final static int OPENSEARCH_PORT_DEFAULT = 9300;
  public final static int OPENSEARCH_REST_PORT_DEFAULT = 9200;
  public final static boolean OPENSEARCH_SECURTIY_ENABLED_DEFAULT = false;
  public final static boolean OPENSEARCH_HTTPS_ENABLED_DEFAULT = false;
  public final static String OPENSEARCH_ADMIN_USER_DEFAULT ="admin";
  public final static String OPENSEARCH_ADMIN_SERVICE_LOG_USER_DEFAULT ="service_log_viewer";
  public final static String OPENSEARCH_ADMIN_PASSWORD_DEFAULT ="adminpw";
  public final static boolean OPENSEARCH_JWT_ENABLED_DEFAULT = false;
  public final static String OPENSEARCH_JWT_URL_PARAMETER_DEFAULT ="jt";
  public final static long OPENSEARCH_JWT_EXP_MS_DEFAULT = 10 * 60 * 1000;
  
  public final static String OPENSEARCH_ADMIN_ROLE = "admin";
  public final static String OPENSEARCH_SERVICE_LOG_ROLE = "service_log_viewer";

  public final static Integer DEFAULT_SCROLL_PAGE_SIZE = 1000;
  public final static Integer MAX_SCROLL_PAGE_SIZE = 10000;
  
  private final List<String> openSearchIps;
  private final int openSearchPort;
  private final int openSearchRestPort;
  private final boolean openSearchSecurityEnabled;
  private final boolean httpsEnabled;
  private final String adminUser;
  private final String adminPassword;
  private final boolean openSearchJWTEnabled;
  private final String openSearchJWTURLParameter;
  private final long openSearchJWTExpMs;
  private final String serviceLogUser;
  
  private int rrIndex = 0;
  
  public OpenSearchSettings(String openSearchIps, int openSearchPort, int openSearchRestPort,
                            boolean openSearchSecurityEnabled, boolean httpsEnabled, String adminUser,
                            String adminPassword, boolean openSearchJWTEnabled, String openSearchJWTURLParameter,
                            long openSearchJWTExpMs, String serviceLogUser){
    String[] ips = openSearchIps.split(",");
    List<String> validIps = new ArrayList<>();
    for (String ip : ips) {
      if (Ip.validIp(ip)) {
        validIps.add(ip);
      }
    }
    this.openSearchIps = validIps;
    this.openSearchPort = openSearchPort;
    this.openSearchRestPort = openSearchRestPort;
    this.openSearchSecurityEnabled = openSearchSecurityEnabled;
    this.httpsEnabled = httpsEnabled;
    this.adminUser = adminUser;
    this.adminPassword = adminPassword;
    this.openSearchJWTEnabled = openSearchJWTEnabled;
    this.openSearchJWTURLParameter = openSearchJWTURLParameter;
    this.openSearchJWTExpMs = openSearchJWTExpMs;
    this.serviceLogUser = serviceLogUser;
  }
  
  public List<String> getOpenSearchIps() {
    return openSearchIps;
  }
  
  public String getOpenSearchEndpoint() {
    return getOpenSearchIp() + ":" + openSearchPort;
  }
  
  public String getOpenSearchRESTEndpoint() {
    return (isHttpsEnabled() ? "https" :
        "http") + "://" + getOpenSearchIp() + ":" + openSearchRestPort;
  }
  
  public int getOpenSearchPort() {
    return openSearchPort;
  }
  
  public int getOpenSearchRESTPort() {
    return openSearchRestPort;
  }
  
  private String getOpenSearchIp(){
    if(rrIndex == openSearchIps.size()){
      rrIndex = 0;
    }
    return openSearchIps.get(rrIndex++);
  }
  
  public boolean isOpenSearchSecurityEnabled() {
    return openSearchSecurityEnabled;
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
  
  public boolean isOpenSearchJWTEnabled() {
    return openSearchJWTEnabled;
  }
  
  public String getOpenSearchJWTURLParameter() {
    return openSearchJWTURLParameter;
  }
  
  public long getOpenSearchJWTExpMs() {
    return openSearchJWTExpMs;
  }
  
  public Integer getDefaultScrollPageSize() {
    return DEFAULT_SCROLL_PAGE_SIZE;
  }
  
  public Integer getMaxScrollPageSize() {
    return MAX_SCROLL_PAGE_SIZE;
  }

  public String getServiceLogUser() {
    return serviceLogUser;
  }
}
