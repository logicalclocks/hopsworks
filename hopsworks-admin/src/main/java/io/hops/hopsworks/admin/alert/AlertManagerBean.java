/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.admin.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class AlertManagerBean implements Serializable {
  private static final Logger LOGGER = Logger.getLogger(AlertManagerBean.class.getName());
  private String content = "";
  private String mode = "yaml";
  private String theme = "blackboard";
  private String keymap = "default";
  
  private String json;
  private String yaml;
  private AlertManagerConfig alertManagerConfigBackup;
  private AlertManagerConfig alertManagerConfig;

  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;
  
  @PostConstruct
  public void init() {
    read();
  }
  
  private void read() {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    ObjectMapper objectMapperJson = new ObjectMapper();
    try {
      alertManagerConfigBackup = alertManagerConfiguration.read();
      json = objectMapperJson.writerWithDefaultPrettyPrinter().writeValueAsString(alertManagerConfigBackup);
      yaml = objectMapper.writeValueAsString(alertManagerConfigBackup);
      changeMode();
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException | JsonProcessingException e) {
      MessagesController.addErrorMessage("Failed to init alert manager.", e.getMessage());
    }
  }
  
  private void changeMode() {
    if ("javascript".equals(mode)) {
      content = json;
    } else {
      content = yaml;
    }
  }
  
  private AlertManagerConfig toAlertManagerConfig(String value) throws IOException {
    AlertManagerConfig alertManagerConfig;
    if ("javascript".equals(mode)) {
      ObjectMapper objectMapperJson = new ObjectMapper();
      alertManagerConfig = objectMapperJson.readValue(value, AlertManagerConfig.class);
    } else {
      ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
      alertManagerConfig = objectMapper.readValue(value, AlertManagerConfig.class);
    }
    return alertManagerConfig;
  }
  
  public void submitContent() {
    try {
      alertManagerConfiguration.writeAndReload(alertManagerConfig);
      read();
      MessagesController.addInfoMessage("Success", "Alert manager config updated");
    } catch (Exception e) {
      MessagesController.addErrorMessage("Failed update alert manager config.", e.getMessage());
    }
  }
  
  public void validate(FacesContext context, UIComponent toValidate, Object value) {
    String newContent = value.toString();
    if (!newContent.isEmpty()) {
      try {
        alertManagerConfig = toAlertManagerConfig(newContent);
      } catch (IOException e) {
        ((UIInput) toValidate).setValid(false);
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
            "Validation error: " + e.getMessage());
        context.addMessage(toValidate.getClientId(context), message);
      }
    } else {
      ((UIInput) toValidate).setValid(false);
      FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
          "Validation error: Value is required.");
      context.addMessage(toValidate.getClientId(context), message);
    }
  }
  
  public List<String> getThemes() {
    final List<String> results = new ArrayList<>();
    
    results.add("3024-day");
    results.add("3024-night");
    results.add("abcdef");
    results.add("ambiance");
    results.add("ambiance-mobile");
    results.add("base16-dark");
    results.add("base16-light");
    results.add("bespin");
    results.add("blackboard");
    results.add("cobalt");
    results.add("colorforth");
    results.add("dracula");
    results.add("eclipse");
    results.add("elegant");
    results.add("erlang-dark");
    results.add("hopscotch");
    results.add("icecoder");
    results.add("isotope");
    results.add("lesser-dark");
    results.add("liquibyte");
    results.add("material");
    results.add("mbo");
    results.add("mdn-like");
    results.add("midnight");
    results.add("monokai");
    results.add("neat");
    results.add("neo");
    results.add("night");
    results.add("panda-syntax");
    results.add("paraiso-dark");
    results.add("paraiso-light");
    results.add("pastel-on-dark");
    results.add("railscasts");
    results.add("rubyblue");
    results.add("seti");
    results.add("solarized");
    results.add("the-matrix");
    results.add("tomorrow-night-bright");
    results.add("tomorrow-night-eighties");
    results.add("ttcn");
    results.add("twilight");
    results.add("vibrant-ink");
    results.add("xq-dark");
    results.add("xq-light");
    results.add("yeti");
    results.add("zenburn");
    
    Collections.sort(results);
    return results;
  }
  
  public List<Mode> getModes() {
    final List<Mode> results = new ArrayList<>();
    
    results.add(new Mode("javascript", "json"));
    results.add(new Mode("yaml", "yaml"));
    
    Collections.sort(results);
    return results;
  }
  
  public String getContent() {
    return content;
  }
  
  public void setContent(final String content) {
    this.content = content;
  }
  
  public String getMode() {
    return mode;
  }
  
  public void setMode(final String mode) {
    this.mode = mode;
    changeMode();
  }
  
  public String getTheme() {
    return theme;
  }
  
  public void setTheme(final String theme) {
    this.theme = theme;
  }
  
  public String getKeymap() {
    return keymap;
  }
  
  public void setKeymap(final String keymap) {
    this.keymap = keymap;
  }
  
  public class Mode implements Comparable<Mode> {
    private String name;
    private String displayName;
  
    public Mode(String name, String displayName) {
      this.name = name;
      this.displayName = displayName;
    }
  
    public String getName() {
      return name;
    }
  
    public void setName(String name) {
      this.name = name;
    }
  
    public String getDisplayName() {
      return displayName;
    }
  
    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }
  
    @Override
    public int compareTo(Mode mode) {
      return this.name.compareTo(mode.name);
    }
  }
}
