/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.dao.hdfs.command;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.hdfs.command.HdfsCommandExecution;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Optional;
import java.util.logging.Logger;

@Stateless
public class HdfsCommandExecutionFacade extends AbstractFacade<HdfsCommandExecution> {
  private static final Logger LOGGER = Logger.getLogger(HdfsCommandExecutionFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager entityManager;

  public HdfsCommandExecutionFacade() {
    super(HdfsCommandExecution.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return entityManager;
  }

  public Optional<HdfsCommandExecution> findBySrc(Inode srcInode) {
    TypedQuery<HdfsCommandExecution>
      query = entityManager.createNamedQuery("HdfsCommandExecution.findBySrcInode", HdfsCommandExecution.class)
      .setParameter("srcInode", srcInode);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<HdfsCommandExecution> findByExecution(Execution execution) {
    TypedQuery<HdfsCommandExecution>
      query = entityManager.createNamedQuery("HdfsCommandExecution.findByExecution", HdfsCommandExecution.class)
      .setParameter("execution", execution);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
