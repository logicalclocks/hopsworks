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
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.hops.bbc.charon;

import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import javax.persistence.EntityExistsException;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.rest.AppException;

public class CharonRepoSharedFacade extends AbstractFacade<CharonRepoShared> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CharonRepoSharedFacade() {
        super(CharonRepoShared.class);
    }

    public List<CharonRepoShared> findByProjectId(int projectId) {
        TypedQuery<CharonRepoShared> query = em.createNamedQuery("CharonRepoShared.findByProjectId",
                CharonRepoShared.class);
        query.setParameter("projectId", projectId);
        return query.getResultList();
    }
    
    public void persist(CharonRepoShared site) throws AppException {
        try {
            em.persist(site);
        } catch (EntityExistsException ex) {
            throw new AppException(Response.Status.CONFLICT.getStatusCode(), ResponseMessages.CHARON_SHARE_ALREADY_EXISTS);
        } catch (IllegalArgumentException ex) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), ResponseMessages.CHARON_BAD_SITE);
        }
    }    
}
