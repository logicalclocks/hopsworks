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
