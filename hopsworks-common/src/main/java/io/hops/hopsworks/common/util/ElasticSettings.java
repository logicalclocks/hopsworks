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
  public final static Integer DEFAULT_SCROLL_PAGE_SIZE = 1000;
  public final static Integer MAX_SCROLL_PAGE_SIZE = 10000;
  
  private final List<String> elasticIps;
  private final int elasticPort;
  private final int elasticRestPort;
  
  private int rrIndex = 0;
  
  public ElasticSettings(String elasticIps, int elasticPort, int elasticRestPort){
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
  }
  
  public List<String> getElasticIps() {
    return elasticIps;
  }
  
  public String getElasticEndpoint() {
    return getElasticIp() + ":" + elasticPort;
  }
  
  public String getElasticRESTEndpoint() {
    return getElasticIp() + ":" + elasticRestPort;
  }
  
  public int getElasticPort() {
    return elasticPort;
  }
  
  private String getElasticIp(){
    if(rrIndex == elasticIps.size()){
      rrIndex = 0;
    }
    return elasticIps.get(rrIndex++);
  }
  
  public Integer getDefaultScrollPageSize() {
    return DEFAULT_SCROLL_PAGE_SIZE;
  }
  
  public Integer getMaxScrollPageSize() {
    return MAX_SCROLL_PAGE_SIZE;
  }
}
