/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.users.UserView;
import io.hops.hopsworks.common.dao.user.activity.Activity;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class ActivityView {
  
  private UserView user;
  private LimitedProjectView project;
  private Integer id;
  private String flag;
  private Date timestamp;
  private String activity;
  
  public ActivityView(){}
  
  public ActivityView(Activity activityElement) {
    this.activity = activityElement.getActivity();
    this.timestamp = activityElement.getTimestamp();
    this.flag = activityElement.getFlag();
    this.id = activityElement.getId();
    this.project = new LimitedProjectView(activityElement.getProject());
    this.user = new UserView(activityElement.getUser());
  }
  
  public UserView getUser() {
    return user;
  }
  
  public void setUser(UserView user) {
    this.user = user;
  }
  
  public LimitedProjectView getProject() {
    return project;
  }
  
  public void setProject(LimitedProjectView project) {
    this.project = project;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getFlag() {
    return flag;
  }
  
  public void setFlag(String flag) {
    this.flag = flag;
  }
  
  public Date getTimestamp() {
    return timestamp;
  }
  
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
  
  public String getActivity() {
    return activity;
  }
  
  public void setActivity(String activity) {
    this.activity = activity;
  }
}
