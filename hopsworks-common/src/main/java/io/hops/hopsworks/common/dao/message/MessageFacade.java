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

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class MessageFacade extends AbstractFacade<Message> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public MessageFacade() {
    super(Message.class);
  }

  /**
   * Get a message by id
   *
   * @param id
   * @return
   */
  public Message findMessageById(int id) {
    return em.find(Message.class, id);
  }

  /**
   * Get all messages to a user
   *
   * @param user
   * @return list of message
   */
  public List<Message> getAllMessagesTo(Users user) {
    TypedQuery<Message> query = em.
            createNamedQuery("Message.findByToAndDeleted",
                    Message.class)
            .setParameter("to", user)
            .setParameter("deleted", false);
    return query.getResultList();
  }

  /**
   * Get all unread messages to a user
   *
   * @param user
   * @return list of message
   */
  public List<Message> getAllUnreadMessagesTo(Users user) {
    TypedQuery<Message> query = em.createNamedQuery(
            "Message.findMessagesByToAndUnread",
            Message.class)
            .setParameter("to", user)
            .setParameter("unread", true);
    return query.getResultList();
  }

  /**
   * Gets the count of all messages to a user
   *
   * @param user
   * @return number of messages
   */
  public Long countAllMessagesTo(Users user) {
    TypedQuery query = em.createNamedQuery("Message.countByToAndDeleted",
            Long.class)
            .setParameter("to", user)
            .setParameter("deleted", false);
    try {
      return (Long) query.getSingleResult();
    } catch (NoResultException e) {
      return 0L;
    }
  }

  /**
   * Gets the count of unread messages to a user
   *
   * @param user
   * @return number of messages
   */
  public Long countUnreadMessagesTo(Users user) {
    TypedQuery query = em.createNamedQuery("Message.countByToAndUnread",
            Long.class)
            .setParameter("to", user)
            .setParameter("unread", true);
    try {
      return (Long) query.getSingleResult();
    } catch (NoResultException e) {
      return 0L;
    }
  }

  /**
   * Gets all deleted messages to a user
   *
   * @param user
   * @return list of messages
   */
  public List<Message> getAllDeletedMessagesTo(Users user) {
    TypedQuery<Message> query = em.
            createNamedQuery("Message.findByToAndDeleted",
                    Message.class)
            .setParameter("to", user)
            .setParameter("deleted", true);
    return query.getResultList();
  }

  /**
   * Gets the count of deleted messages to a user
   *
   * @param user
   * @return number of messages
   */
  public Long countDeletedMessagesTo(Users user) {
    TypedQuery query = em.createNamedQuery("Message.countByToAndDeleted",
            Long.class)
            .setParameter("to", user)
            .setParameter("deleted", true);
    try {
      return (Long) query.getSingleResult();
    } catch (NoResultException e) {
      return 0L;
    }
  }

  /**
   * Empty trash
   *
   * @param user
   * @return
   */
  public int emptyTrash(Users user) {
    TypedQuery query = em.createNamedQuery("Message.emptyToAndDeleted",
            Long.class)
            .setParameter("to", user)
            .setParameter("deleted", true);
    try {
      return query.executeUpdate();
    } catch (NoResultException e) {
      return 0;
    }
  }

  /**
   * Empty messages folder
   *
   * @param user
   * @return
   */
  public int emptyMessage(Users user) {
    TypedQuery query = em.createNamedQuery("Message.emptyToAndDeleted",
            Long.class)
            .setParameter("to", user)
            .setParameter("deleted", false);
    try {
      return query.executeUpdate();
    } catch (NoResultException e) {
      return 0;
    }
  }

//  public void persist(Message entity) {
//    em.persist(entity);
//    //entity.setId(em.merge(entity).getId());
//    em.flush();
//  }
}
