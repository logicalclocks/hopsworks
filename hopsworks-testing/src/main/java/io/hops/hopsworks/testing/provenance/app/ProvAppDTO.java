/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.testing.provenance.app;

import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateElastic;
import io.hops.hopsworks.common.provenance.core.Provenance;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class ProvAppDTO {
  List<ProvAppDTO> items;
  private String appId;
  private String appState;
  private Long appStateTimestamp;
  private String readableTimestamp;
  private String appName;
  private String appUser;
  
  public ProvAppDTO() {
  }
  
  public static ProvAppDTO withAppStates(Map<String, Map<Provenance.AppState, ProvAppStateElastic>> allAppStates) {
    ProvAppDTO apps = new ProvAppDTO();
    List<ProvAppDTO> appsItems = new ArrayList<>();
    apps.setItems(appsItems);
    for(Map.Entry<String, Map<Provenance.AppState, ProvAppStateElastic>> appStatesAux : allAppStates.entrySet()) {
      ProvAppDTO appStates = new ProvAppDTO();
      appsItems.add(appStates);
      List<ProvAppDTO> appStatesItems = new ArrayList<>();
      appStates.setItems(appStatesItems);
      for(Map.Entry<Provenance.AppState, ProvAppStateElastic> appStateAux : appStatesAux.getValue().entrySet()) {
        ProvAppDTO state = new ProvAppDTO();
        state.setAppId(appStateAux.getValue().getAppId());
        state.setAppName(appStateAux.getValue().getAppName());
        state.setAppState(appStateAux.getValue().getAppState().toString());
        state.setAppUser(appStateAux.getValue().getAppUser());
        state.setAppStateTimestamp(appStateAux.getValue().getAppStateTimestamp());
        state.setReadableTimestamp(appStateAux.getValue().getReadableTimestamp());
        appStatesItems.add(state);
      }
    }
    return apps;
  }
  
  public List<ProvAppDTO> getItems() {
    return items;
  }
  
  public void setItems(List<ProvAppDTO> items) {
    this.items = items;
  }
  
  public String getAppId() {
    return appId;
  }
  
  public void setAppId(String appId) {
    this.appId = appId;
  }
  
  public String getAppState() {
    return appState;
  }
  
  public void setAppState(String appState) {
    this.appState = appState;
  }
  
  public Long getAppStateTimestamp() {
    return appStateTimestamp;
  }
  
  public void setAppStateTimestamp(Long appStateTimestamp) {
    this.appStateTimestamp = appStateTimestamp;
  }
  
  public String getReadableTimestamp() {
    return readableTimestamp;
  }
  
  public void setReadableTimestamp(String readableTimestamp) {
    this.readableTimestamp = readableTimestamp;
  }
  
  public String getAppName() {
    return appName;
  }
  
  public void setAppName(String appName) {
    this.appName = appName;
  }
  
  public String getAppUser() {
    return appUser;
  }
  
  public void setAppUser(String appUser) {
    this.appUser = appUser;
  }
}
