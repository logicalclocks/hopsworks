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
package io.hops.bbc.charon;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import se.kth.hopsworks.rest.AppException;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CharonController {

  
  @EJB
  private CharonRegisteredSitesFacade charonRegisteredSitesFacade;

  @EJB
  private CharonRepoSharedFacade charonRepoSharedFacade;

  public List<CharonRegisteredSites> getCharonRegisteredSites(int projectId) {
    return charonRegisteredSitesFacade.findByProjectId(projectId);
  }
  
  public CharonRegisteredSites getCharonRegisteredSite(int projectId, int siteId) {
    return charonRegisteredSitesFacade.findSiteForProject(projectId, siteId);
  }  

  public List<CharonRepoShared> getCharonSharedSites(int projectId) {
    return charonRepoSharedFacade.findByProjectId(projectId);
  }
  
  public void registerSite(int projectId, String site) throws AppException {
      int siteID = ParseSiteId.id(site);
      String name = ParseSiteId.name(site);
      String email = ParseSiteId.email(site);
      String addr = ParseSiteId.addr(site);
      CharonRegisteredSites registerSite = new CharonRegisteredSites(
              new CharonRegisteredSitesPK(projectId, siteID), email, name, addr);
      charonRegisteredSitesFacade.persist(registerSite);
  }
  public void removeSite(int projectId, int siteId) throws AppException {  
	CharonRegisteredSitesPK pk = new CharonRegisteredSitesPK(projectId, siteId);
	charonRegisteredSitesFacade.remove(pk);
  }
  
  public void shareWithSite(int projectId, int granteeId, String path, String permissions, 
		  String token) throws AppException {
      CharonRepoSharedPK pk = new CharonRepoSharedPK(projectId, granteeId, path);
      charonRepoSharedFacade.persist(new CharonRepoShared(pk, token, permissions));
  }  
}
