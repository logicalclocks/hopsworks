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
package io.hops.hopsworks.apiV2.users;

import io.hops.hopsworks.common.dao.user.Users;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserView {
  private String firstname;
  private String lastname;
  private Integer uid;
  
  public UserView(){}
  
  public UserView(Users user){
    firstname = user.getFname();
    lastname = user.getLname();
    uid = user.getUid();
  }
  
  public UserView(String firstname, String lastname, Integer uid) {
    this.firstname = firstname;
    this.lastname = lastname;
    this.uid = uid;
  }
  
  public String getFirstname() {
    return firstname;
  }
  
  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }
  
  public String getLastname() {
    return lastname;
  }
  
  public void setLastname(String lastname) {
    this.lastname = lastname;
  }
  
  public Integer getUid() {
    return uid;
  }
  
  public void setUid(Integer uid) {
    this.uid = uid;
  }
}
