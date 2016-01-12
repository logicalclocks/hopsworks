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
package se.kth.bbc.project;

import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class CharonRegisteredSitesFacade extends AbstractFacade<CharonRegisteredSites> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public CharonRegisteredSitesFacade() {
    super(CharonRegisteredSites.class);
  }

  @Override
  public List<CharonRegisteredSites> findAll() {
    TypedQuery<CharonRegisteredSites> query = em.createNamedQuery("CharonRegisteredSites.findAll",
        CharonRegisteredSites.class);
    return query.getResultList();
  }
}
