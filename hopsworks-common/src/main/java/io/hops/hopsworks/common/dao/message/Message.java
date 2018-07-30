/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.dao.message;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import io.hops.hopsworks.common.dao.user.Users;

@Entity
@Table(name = "hopsworks.message")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Message.findAll",
          query
          = "SELECT m FROM Message m"),
  @NamedQuery(name = "Message.findById",
          query
          = "SELECT m FROM Message m WHERE m.id = :id"),
  @NamedQuery(name = "Message.findByDateSent",
          query
          = "SELECT m FROM Message m WHERE m.dateSent = :dateSent"),
  @NamedQuery(name = "Message.findByFrom",
          query
          = "SELECT m FROM Message m WHERE m.from = :from"),
  @NamedQuery(name = "Message.findMessagesByToAndUnread",
          query
          = "SELECT m FROM Message m WHERE m.unread = :unread AND m.to = :to"),
  @NamedQuery(name = "Message.countByToAndUnread",
          query
          = "SELECT COUNT(m.id) FROM Message m WHERE m.to = :to AND m.unread = :unread"),
  @NamedQuery(name = "Message.findByToAndDeleted",
          query
          = "SELECT m FROM Message m WHERE m.to = :to AND m.deleted = :deleted  "
                  + "ORDER BY m.unread DESC, m.dateSent DESC"),
  @NamedQuery(name = "Message.emptyToAndDeleted",
          query
          = "DELETE FROM Message m WHERE m.to = :to AND m.deleted = :deleted"),
  @NamedQuery(name = "Message.countByToAndDeleted",
          query
          = "SELECT COUNT(m.id) FROM Message m WHERE m.to = :to AND m.deleted = :deleted")})
public class Message implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "date_sent")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateSent;
  @Size(max = 128)
  @Column(name = "subject")
  private String subject;
  @Size(max = 128)
  @Column(name = "preview")
  private String preview;
  @Basic(optional = false)
  @NotNull
  @Lob
  @Size(min = 1,
          max = 65535)
  @Column(name = "content")
  private String content;
  @Basic(optional = false)
  @NotNull
  @Column(name = "unread")
  private boolean unread;
  @Basic(optional = false)
  @NotNull
  @Column(name = "deleted")
  private boolean deleted;
  @Size(max = 600)
  @Column(name = "path")
  private String path;
  @JoinTable(name = "hopsworks.message_to_user",
          joinColumns
          = {
            @JoinColumn(name = "message",
                    referencedColumnName = "id")},
          inverseJoinColumns
          = {
            @JoinColumn(name = "user_email",
                    referencedColumnName = "email")})
  @ManyToMany
  private Collection<Users> usersCollection;
  @JoinColumn(name = "user_from",
          referencedColumnName = "email")
  @ManyToOne
  private Users from;
  @JoinColumn(name = "user_to",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users to;

  @JoinColumn(name = "reply_to_msg")
  @OneToOne
  private Message replyToMsg;

  public Message() {
  }

  public Message(Integer id) {
    this.id = id;
  }

  public Message(Users from, Users to, Date dateSent, String content,
          boolean unread,
          boolean deleted) {
    this.from = from;
    this.to = to;
    this.dateSent = dateSent;
    this.content = content;
    this.unread = unread;
    this.deleted = deleted;
  }

  public Message(Users from, Users to, Date dateSent) {
    this.from = from;
    this.to = to;
    this.dateSent = dateSent;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getDateSent() {
    return dateSent;
  }

  public void setDateSent(Date dateSent) {
    this.dateSent = dateSent;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getPreview() {
    return preview;
  }

  public void setPreview(String preview) {
    this.preview = preview;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public boolean getUnread() {
    return unread;
  }

  public void setUnread(boolean unread) {
    this.unread = unread;
  }

  public boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Collection<Users> getUsersCollection() {
    return usersCollection;
  }

  public void setUsersCollection(Collection<Users> usersCollection) {
    this.usersCollection = usersCollection;
  }

  public Users getFrom() {
    return from;
  }

  public void setFrom(Users from) {
    this.from = from;
  }

  public Users getTo() {
    return to;
  }

  public void setTo(Users to) {
    this.to = to;
  }

  @XmlTransient
  @JsonIgnore
  public Message getReplyToMsg() {
    return replyToMsg;
  }

  public void setReplyToMsg(Message replyToMsg) {
    this.replyToMsg = replyToMsg;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Message)) {
      return false;
    }
    Message other = (Message) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.message.Message[ id=" + id + " ]";
  }

}
