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

package io.hops.hopsworks.common.dao.zeppelin;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class ZeppelinInterpreterConfFacade extends AbstractFacade<ZeppelinInterpreterConfs> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ZeppelinInterpreterConfFacade() {
    super(ZeppelinInterpreterConfs.class);
  }

  public ZeppelinInterpreterConfs findByProject(Project project) {
    TypedQuery<ZeppelinInterpreterConfs> query = em.createNamedQuery("ZeppelinInterpreterConfs.findByProject",
            ZeppelinInterpreterConfs.class);
    query.setParameter("projectId", project);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ZeppelinInterpreterConfs create(Project project, String intrepeterConf) {
    if (project == null || intrepeterConf == null) {
      throw new NullPointerException("project and config must be non-null.");
    }
    ZeppelinInterpreterConfs conf = findByProject(project);
    if (conf == null) {
      conf = new ZeppelinInterpreterConfs(project, intrepeterConf);
      em.persist(conf);
    } else {
      conf.setInterpreterConf(intrepeterConf);
      em.merge(conf);
    }
    em.flush();
    return conf;
  }
}
