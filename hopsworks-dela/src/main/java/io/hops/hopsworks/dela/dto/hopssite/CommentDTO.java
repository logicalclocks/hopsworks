/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.dela.dto.hopssite;

import io.hops.hopsworks.dela.dto.common.UserDTO;
import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

public class CommentDTO {

  @XmlRootElement
  public static class Publish {

    private String content;
    private String userEmail;

    public Publish() {
    }

    public Publish(String userEmail, String content) {
      this.content = content;
      this.userEmail = userEmail;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public String getUserEmail() {
      return userEmail;
    }

    public void setUserEmail(String userEmail) {
      this.userEmail = userEmail;
    }
  }

  @XmlRootElement
  public static class RetrieveComment {

    private Integer id;
    private String content;
    private UserDTO.Complete user;
    private Date datePublished;

    public RetrieveComment() {
    }

    public RetrieveComment(Integer id, String content, UserDTO.Complete user, Date datePublished) {
      this.id = id;
      this.content = content;
      this.user = user;
      this.datePublished = datePublished;
    }

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public UserDTO.Complete getUser() {
      return user;
    }

    public void setUser(UserDTO.Complete user) {
      this.user = user;
    }

    public Date getDatePublished() {
      return datePublished;
    }

    public void setDatePublished(Date datePublished) {
      this.datePublished = datePublished;
    }
  }
}
