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

package io.hops.hopsworks.common.dao.jobhistory;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class YarnAppHeuristicResultDetailsFacade extends AbstractFacade<YarnAppHeuristicResultDetails> {

  private static final Logger logger = Logger.getLogger(
          YarnAppHeuristicResultDetailsFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnAppHeuristicResultDetailsFacade() {
    super(YarnAppHeuristicResultDetails.class);
  }

  public String searchByIdAndName(int yarnAppHeuristicResultId, String name) {

    try {
      TypedQuery<YarnAppHeuristicResultDetails> q = em.createNamedQuery(
              "YarnAppHeuristicResultDetails.findByIdAndName",
              YarnAppHeuristicResultDetails.class);
      q.setParameter("yarnAppHeuristicResultId", yarnAppHeuristicResultId);
      q.setParameter("name", name);

      YarnAppHeuristicResultDetails result = q.getSingleResult();
      return result.getValue();

    } catch (NoResultException e) {
      return "UNDEFINED";
    }
  }
}
