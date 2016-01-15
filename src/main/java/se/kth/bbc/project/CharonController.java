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

import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.hopsworks.controller.FolderNameValidator;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ValidationException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CharonController {

  @EJB
  private CharonRegisteredSitesFacade charonRegisteredSitesFacade;

  @EJB
  private CharonRepoSharedFacade charonRepoSharedFacade;

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public List<CharonRegisteredSites> getAllCharonRegisteredSites() {
    return charonRegisteredSitesFacade.findAll();
  }

  public List<CharonRepoShared> getAllCharonRepoShared() {
    return charonRepoSharedFacade.findAll();
  }

  public void addSiteId(int projectId, String siteId) throws IOException {

    CharonRegisteredSitesPK stp = new CharonRegisteredSitesPK(projectId, siteId);
    CharonRegisteredSites st = new CharonRegisteredSites(stp);
    charonRegisteredSitesFacade.persistCharonRegisteredSite(st);
  }
}
