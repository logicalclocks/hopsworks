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
