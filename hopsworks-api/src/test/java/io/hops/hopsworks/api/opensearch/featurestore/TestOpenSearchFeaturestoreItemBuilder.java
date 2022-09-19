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
package io.hops.hopsworks.api.opensearch.featurestore;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.opensearch.FeaturestoreDocType;
import io.hops.hopsworks.common.opensearch.OpenSearchFeaturestoreHit;
import io.hops.hopsworks.common.util.HopsworksJAXBContext;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;


public class TestOpenSearchFeaturestoreItemBuilder {
  OpenSearchFeaturestoreItemBuilder itemBuilder;
  UserFacade userFacade;
  HopsworksJAXBContext converter;
  Users user;
  
  @Before
  public void setup() {
    user = new Users();
    user.setFname("John");
    user.setLname("Doe");
    user.setEmail("doe@hopsworks.ai");
    user.setUsername("johndoe000");
    userFacade = Mockito.mock(UserFacade.class);
    converter = new HopsworksJAXBContext();
    converter.init();
    itemBuilder = new OpenSearchFeaturestoreItemBuilder(userFacade, converter);
  
    Mockito.when(userFacade.findByEmail(Mockito.anyString()))
      .thenReturn(user);
  }
  
  private OpenSearchFeaturestoreHit baseHit() {
    OpenSearchFeaturestoreHit hit = new OpenSearchFeaturestoreHit();
    hit.setId("1");
    hit.setScore(1L);
    hit.setName("artifact");
    hit.setVersion(1);
    hit.setProjectId(100);
    hit.setProjectName("project");
    hit.setDatasetIId(10L);
    return hit;
  }
  
  private OpenSearchFeaturestoreHit baseFeaturestoreXAttr(OpenSearchFeaturestoreHit hit) {
    Map<String, Object> featureStoreXattr = (Map)(hit.getXattrs().get(FeaturestoreXAttrsConstants.FEATURESTORE));
    if(featureStoreXattr == null) {
      featureStoreXattr = new HashMap<>();
      hit.getXattrs().put(FeaturestoreXAttrsConstants.FEATURESTORE, featureStoreXattr);
    }
    featureStoreXattr.put("featurestore_id", "1");
    featureStoreXattr.put("description", "some description");
    featureStoreXattr.put("create_date", System.currentTimeMillis());
    featureStoreXattr.put("creator", "dev@hopsworks.ai");
    return hit;
  }
  
  @Test
  public void testNoFeaturestoreXattr() throws GenericException {
    OpenSearchFeaturestoreHit hit = baseHit();
    hit.setDocType(FeaturestoreDocType.FEATUREGROUP.toString().toLowerCase());
    itemBuilder.fromBaseArtifact(hit);
    hit.setDocType(FeaturestoreDocType.FEATUREVIEW.toString().toLowerCase());
    itemBuilder.fromBaseArtifact(hit);
    hit.setDocType(FeaturestoreDocType.TRAININGDATASET.toString().toLowerCase());
    itemBuilder.fromBaseArtifact(hit);
    //just making sure no NPE exceptions
  }
  
  @Test
  public void testBaseArtifacts() throws GenericException {
    OpenSearchFeaturestoreHit hit = baseHit();
    hit = baseFeaturestoreXAttr(hit);
  
    OpenSearchFeaturestoreItemDTO.Base result;
    
    hit.setDocType(FeaturestoreDocType.FEATUREGROUP.toString().toLowerCase());
    result = itemBuilder.fromBaseArtifact(hit);
    Assert.assertNotNull(result.getCreated());
    Assert.assertNotNull(result.getCreator());
  
    hit.setDocType(FeaturestoreDocType.FEATUREVIEW.toString().toLowerCase());
    result = itemBuilder.fromBaseArtifact(hit);
    Assert.assertNotNull(result.getCreated());
    Assert.assertNotNull(result.getCreator());
  
    hit.setDocType(FeaturestoreDocType.TRAININGDATASET.toString().toLowerCase());
    result = itemBuilder.fromBaseArtifact(hit);
    Assert.assertNotNull(result.getCreated());
    Assert.assertNotNull(result.getCreator());
  }
}
