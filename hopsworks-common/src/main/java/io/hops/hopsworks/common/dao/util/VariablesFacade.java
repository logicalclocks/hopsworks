/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.hops.hopsworks.common.dao.util;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class VariablesFacade extends AbstractFacade<Variables> {

  private final static Logger logger = Logger.getLogger(VariablesFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public VariablesFacade() { super(Variables.class); }

  public void storeVariable(String id, String value) {
    Variables variable = new Variables(id, value);
    em.merge(variable);
    em.flush();
  }

  public String getVariableValue(String id) {
    TypedQuery<Variables> query =
        em.createNamedQuery("Variables.findById", Variables.class)
        .setParameter("id", id);

    try {
      Variables var = query.getSingleResult();
      if (var != null) {
        return var.getValue();
      }
    } catch (NoResultException e) {
      logger.log(Level.INFO, "Variable " + id + " not found in the database");
    }

    return null;
  }
}
