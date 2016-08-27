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
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package se.kth.bbc.jobs.jobhistory;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

@Stateless
public class YarnApplicationAttemptStateFacade extends AbstractFacade<YarnApplicationattemptstate> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public YarnApplicationAttemptStateFacade() {
        super(YarnApplicationattemptstate.class);
    }

    public String findTrackingUrlByAppId(String applicationid) {
        if (applicationid == null) {
            return "";
        }
        TypedQuery<YarnApplicationattemptstate> query = em.createNamedQuery("YarnApplicationattemptstate.findByApplicationid",
            YarnApplicationattemptstate.class).setParameter(
                "applicationid", applicationid);
        List<YarnApplicationattemptstate> appAttempts = query.getResultList();
        if (appAttempts != null) {
            Integer highestAttemptId = 0;
            String trackingUrl = "";
            for (YarnApplicationattemptstate a : appAttempts) {
                try {
                    String attemptId =a.getYarnApplicationattemptstatePK().getApplicationattemptid();
                    // attemptIds look like 'application12133_1000032423423_0001'
                    // Only the last chars after '_' contain the actual attempt ID.
                    attemptId = attemptId.substring(attemptId.lastIndexOf("_")+1, attemptId.length());
                    Integer attempt = Integer.parseInt(attemptId);
                    if (attempt > highestAttemptId) {
                        highestAttemptId = attempt;
                        trackingUrl = a.getApplicationattempttrakingurl();
                    }

                } catch (NumberFormatException e) {
                    return "";
                }
            }
            return trackingUrl;
        }
        return "";
    }

}
