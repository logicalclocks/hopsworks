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

@Stateless
public class VariablesFacade extends AbstractFacade<Variables> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public VariablesFacade() { super(Variables.class); }

  public void storeVariable(String id, String value) {
    // Check if the variable is already
    Variables variable = getVariable(id);

    if (variable == null) {
      // The variable doesn't exist
      variable = new Variables(id, value);
      em.persist(variable);
    } else {
      // The variable already exists, update its value
      variable.setValue(value);
      em.merge(variable);
    }
  }

  public String getVariableValue(String id) {
    Variables variable = getVariable(id);
    if (variable == null) {
      return null;
    }
    return variable.getValue();
  }

  private Variables getVariable(String id) {
    TypedQuery<Variables> query =
        em.createNamedQuery("Variables.findById", Variables.class)
        .setParameter("id", id);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
