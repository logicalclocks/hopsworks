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
package io.hops.hopsworks.api.provenance.explicit;

import io.hops.hopsworks.api.provenance.explicit.dto.ProvExplicitLinkDTO;
import io.hops.hopsworks.api.provenance.explicit.util.MockProvExplicitLinksBuilder;
import io.hops.hopsworks.api.provenance.explicit.util.MockUriHelper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.provenance.ProvExplicitNode;
import io.hops.hopsworks.provenance.explicit.ProvExplicitControllerEEImpl;
import io.hops.hopsworks.provenance.explicit.util.MockProvExplicitControllerEEImpl;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestProvExplicitLinksBuilder {
  private MockProvExplicitControllerEEImpl.Scenario scenario;
  private ProvExplicitControllerEEImpl provExplicitCtrl;
  private ProvExplicitLinksBuilder builder;
  private UriInfo uriInfo;
  
  @Before
  public void setup()
    throws FeaturestoreException, ServiceException, DatasetException, MetadataException, SchematizedTagException,
           IOException, CloudException {
    scenario = MockProvExplicitControllerEEImpl.standardScenario();
    provExplicitCtrl = MockProvExplicitControllerEEImpl.baseSetup(scenario);
    builder = MockProvExplicitLinksBuilder.baseSetup();
    uriInfo = MockUriHelper.nopUriInfo();
  }
  
  @Test
  public void test()
    throws DatasetException, SchematizedTagException, MetadataException, ServiceException, CloudException,
           GenericException, FeaturestoreException, IOException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.PROVENANCE);
    ResourceRequest artifactExpansion = new ResourceRequest(ResourceRequest.Name.PROVENANCE_ARTIFACTS);
    resourceRequest.setExpansions(new HashSet<>(Collections.singletonList(artifactExpansion)));
    ProvExplicitLink<FeatureView> links = provExplicitCtrl.featureViewLinks(scenario.project, scenario.fv1, -1, -1);
    ProvExplicitLinkDTO<FeatureViewDTO> linksDTO
      = builder.buildFVLinks(uriInfo, resourceRequest, scenario.project, scenario.user, links);
    //root
    checkFullExpansion(linksDTO);
    assertEquals(2, linksDTO.getUpstream().size());
    assertEquals(2, linksDTO.getDownstream().size());
  
    //upstream
    ProvExplicitLinkDTO<FeaturegroupDTO> fg1_lvl1 = linksDTO.getUpstream().get(0);
    assertTrue(fg1_lvl1.getNode().isAccessible());
    assertEquals(ProvExplicitNode.Type.FEATURE_GROUP, fg1_lvl1.getNode().getArtifactType());
    checkFullExpansion(fg1_lvl1);
    assertEquals(2, fg1_lvl1.getUpstream().size());
    
  
    ProvExplicitLinkDTO<FeaturegroupDTO> fg2_lvl1 = linksDTO.getUpstream().get(1);
    assertTrue(fg2_lvl1.getNode().isAccessible());
    assertEquals(ProvExplicitNode.Type.FEATURE_GROUP, fg2_lvl1.getNode().getArtifactType());
    checkFullExpansion(fg2_lvl1);
    assertEquals(1, fg2_lvl1.getUpstream().size());
  
    ProvExplicitLinkDTO<FeaturegroupDTO> fg1_lvl2 = fg1_lvl1.getUpstream().get(0);
    assertTrue(fg1_lvl2.getNode().isAccessible());
    assertEquals(ProvExplicitNode.Type.EXTERNAL_FEATURE_GROUP, fg1_lvl2.getNode().getArtifactType());
    checkFullExpansion(fg1_lvl2);
    assertEquals(0, fg1_lvl2.getUpstream().size());
  
    ProvExplicitLinkDTO<FeaturegroupDTO> fg2_lvl2 = fg1_lvl1.getUpstream().get(1);
    assertTrue(fg2_lvl2.getNode().isAccessible());
    assertEquals(ProvExplicitNode.Type.FEATURE_GROUP, fg2_lvl2.getNode().getArtifactType());
    checkFullExpansion(fg2_lvl2);
    assertEquals(0, fg2_lvl2.getUpstream().size());
  
    ProvExplicitLinkDTO<FeaturegroupDTO> fg3_lvl2 = fg2_lvl1.getUpstream().get(0);
    assertTrue(fg3_lvl2.getNode().isAccessible());
    assertEquals(ProvExplicitNode.Type.FEATURE_GROUP, fg3_lvl2.getNode().getArtifactType());
    checkFullExpansion(fg3_lvl2);
    assertEquals(1, fg3_lvl2.getUpstream().size());
  
    ProvExplicitLinkDTO<FeaturegroupDTO> fg1_lvl3 = fg3_lvl2.getUpstream().get(0);
    assertTrue(fg1_lvl3.getNode().isAccessible());
    assertEquals(ProvExplicitNode.Type.FEATURE_GROUP, fg1_lvl3.getNode().getArtifactType());
    checkFullExpansion(fg1_lvl3);
    assertEquals(0, fg1_lvl3.getUpstream().size());
    
    //downstream
    ProvExplicitLinkDTO<TrainingDatasetDTO> td1_lvl1 = linksDTO.getDownstream().get(0);
    assertTrue(td1_lvl1.getNode().isAccessible());
    assertEquals(ProvExplicitNode.Type.TRAINING_DATASET, td1_lvl1.getNode().getArtifactType());
    checkFullExpansion(td1_lvl1);
    assertEquals(0, td1_lvl1.getUpstream().size());
  
    ProvExplicitLinkDTO<TrainingDatasetDTO> td2_lvl1 = linksDTO.getDownstream().get(1);
    assertTrue(td2_lvl1.getNode().isAccessible());
    assertEquals(ProvExplicitNode.Type.TRAINING_DATASET, td2_lvl1.getNode().getArtifactType());
    checkFullExpansion(td2_lvl1);
    assertEquals(0, td2_lvl1.getUpstream().size());
  }
  
  private void checkFullExpansion(ProvExplicitLinkDTO dto) {
    assertNotNull(dto.getNode());
    assertNotNull(dto.getNode().getArtifact());
    assertNotNull(dto.getNode().getArtifact().getHref());
    assertNotNull(dto.getNode().getArtifactType());
  }
}
