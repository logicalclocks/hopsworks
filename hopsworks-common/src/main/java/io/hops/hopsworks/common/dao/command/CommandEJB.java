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

package io.hops.hopsworks.common.dao.command;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class CommandEJB {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public CommandEJB() {
  }

  public List<Command> findAll() {

    TypedQuery<Command> query = em.createNamedQuery("Command.find",
            Command.class);
    return query.getResultList();
  }

  public List<Command> findRecentByCluster(String cluster) {

    TypedQuery<Command> query = em.createNamedQuery(
            "Command.findRecentByCluster", Command.class)
            .setParameter("cluster", cluster)
            .setParameter("status", Command.CommandStatus.Running);;
    return query.getResultList();
  }

  public List<Command> findRunningByCluster(String cluster) {

    TypedQuery<Command> query = em.createNamedQuery(
            "Command.findRunningByCluster", Command.class)
            .setParameter("cluster", cluster)
            .setParameter("status", Command.CommandStatus.Running);
    return query.getResultList();
  }

  public List<Command> findRecentByClusterService(String cluster, String service) {

    TypedQuery<Command> query = em.createNamedQuery(
            "Command.findRecentByCluster-Service", Command.class)
            .setParameter("cluster", cluster).setParameter("service", service)
            .setParameter("status", Command.CommandStatus.Running);
    return query.getResultList();
  }

  public List<Command> findRunningByClusterService(String cluster,
          String service) {

    TypedQuery<Command> query = em.createNamedQuery(
            "Command.findRunningByCluster-Service", Command.class)
            .setParameter("cluster", cluster).setParameter("service", service)
            .setParameter("status", Command.CommandStatus.Running);
    return query.getResultList();
  }

  public List<Command> findRecentByClusterServiceRoleHostId(String cluster,
          String service, String role, String hostId) {

    TypedQuery<Command> query = em.createNamedQuery(
            "Command.findRecentByCluster-Service-Role-HostId", Command.class)
            .setParameter("cluster", cluster).setParameter("service", service)
            .setParameter("role", role).setParameter("hostId", hostId)
            .setParameter("status", Command.CommandStatus.Running);
    return query.getResultList();
  }

  public List<Command> findLatestByClusterServiceRoleHostname(String cluster,
          String service, String role, String hostId) {

    TypedQuery<Command> query = em.createNamedQuery(
            "Command.findByCluster-Service-Role-HostId", Command.class)
            .setParameter("cluster", cluster).setParameter("service", service)
            .setParameter("role", role).setParameter("hostId", hostId);
    return query.setMaxResults(1).getResultList();
  }

  public void persistCommand(Command command) {
    em.persist(command);
  }

  public void updateCommand(Command command) {
    em.merge(command);
  }

}
