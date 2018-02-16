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
