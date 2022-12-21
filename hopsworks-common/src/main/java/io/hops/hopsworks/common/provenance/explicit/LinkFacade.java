/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.explicit;

import com.google.common.collect.Lists;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.provenance.ProvExplicitNode;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class LinkFacade<T, C, P, L extends ProvExplicitNode> extends AbstractFacade<T> {
  static final Logger LOGGER = Logger.getLogger(LinkFacade.class.getName());
  
  protected Class<L> linkType;
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @EJB
  private Settings settings;
  
  public LinkFacade(Class<T> entityClass, Class<L> linkType) {
    super(entityClass);
    this.linkType = linkType;
  }
  
  /**
   * A transaction to persist an explicit provenance link in the database
   *
   * @param link to persist - used by explicit provenance
   */
  public void persist(L link) {
    try {
      em.persist(link);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the explicit link:" + link.getClass().getSimpleName(), cve);
      throw cve;
    }
  }
  
  /**
   * Gets the entity manager of the facade
   *
   * @return entity manager
   */
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  
  /**
   * Updates an existing explicit link
   *
   * @param link the entity to update
   * @return the updated entity
   */
  public L updateMetadata(L link) {
    em.merge(link);
    return link;
  }
  
  /**
   * Find the links based on the child
   * @return
   */
  public Collection<L> findByChild(C child) {
    return findByChildren(Collections.singletonList(child));
  }
  
  /**
   * Find the links based on the children
   * @return
   */
  public Collection<L> findByChildren(List<C> children) {
    if (children.size() > settings.getSQLMaxSelectIn()) {
      List<L> result = new ArrayList<>();
      for(List<C> childrenPart : Lists.partition(children, settings.getSQLMaxSelectIn())) {
        TypedQuery<L> query =
          em.createNamedQuery(linkType.getSimpleName() + ".findByChildren", linkType);
        query.setParameter("children", childrenPart);
        result.addAll(query.getResultList());
      }
      return result;
    } else {
      TypedQuery<L> query =
        em.createNamedQuery(linkType.getSimpleName() + ".findByChildren", linkType);
      query.setParameter("children", children);
      return query.getResultList();
    }
  }
  
  /**
   * Find the links based on the parent
   * @return
   */
  public Collection<L> findByParent(P parent) {
    return findByParents(Collections.singletonList(parent));
  }
  
  /**
   * Find the links based on the parents
   * @return
   */
  public Collection<L> findByParents(List<P> parents) {
    if (parents.size() > settings.getSQLMaxSelectIn()) {
      List<L> result = new ArrayList<>();
      for(List<P> parentsPart : Lists.partition(parents, settings.getSQLMaxSelectIn())) {
        TypedQuery<L> query =
          em.createNamedQuery(linkType.getSimpleName() + ".findByParents", linkType);
        query.setParameter("parents", parentsPart);
        result.addAll(query.getResultList());
      }
      return result;
    } else {
      TypedQuery<L> query =
        em.createNamedQuery(linkType.getSimpleName() + ".findByParents", linkType);
      query.setParameter("parents", parents);
      return query.getResultList();
    }
  }
}
