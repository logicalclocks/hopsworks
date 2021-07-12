/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.integrations.databricks;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.featurestore.databricks.client.DbCluster;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.anyString;

public class TestDatabricksClusterBuilder {

    private DatabricksClusterBuilder databricksClusterBuilder;

    private UriInfo mockUriInfo;

    private ResourceRequest resourceRequest;

    @Before
    public void setup() {
        databricksClusterBuilder = new DatabricksClusterBuilder();

        mockUriInfo = Mockito.mock(UriInfo.class);
        UriBuilder mockUriBuilder = Mockito.mock(UriBuilder.class);
        Mockito.when(mockUriBuilder.path(anyString()))
                .thenReturn(mockUriBuilder);
        Mockito.when(mockUriInfo.getBaseUriBuilder())
                .thenReturn(mockUriBuilder);

        resourceRequest = new ResourceRequest(ResourceRequest.Name.DATABRICKS);
        Set<ResourceRequest> expansionSet = new HashSet<>();
        expansionSet.add(new ResourceRequest(ResourceRequest.Name.USERS));
        resourceRequest.setExpansions(expansionSet);
    }

    @Test
    public void testBuildNullList() throws Exception {
        //arrange
        Project project = new Project();
        project.setId(0);

        String dbInstance = "testInstance";

        List<DbCluster> dbClusters = null;

        //act
        DatabricksClusterDTO databricksClusterDTO = databricksClusterBuilder.build(
                mockUriInfo, project,resourceRequest, dbInstance, dbClusters);

        //assert
        Assert.assertEquals(0, databricksClusterDTO.getCount().intValue());
        Assert.assertNull (databricksClusterDTO.getItems());
    }

    @Test
    public void testBuildPopulatedList() throws Exception {
        //arrange
        Project project = new Project();
        project.setId(0);

        String dbInstance = "testInstance";

        List<DbCluster> dbClusters = new ArrayList<>();
        dbClusters.add(new DbCluster());

        //act
        DatabricksClusterDTO databricksClusterDTO = databricksClusterBuilder.build(
                mockUriInfo, project, resourceRequest, dbInstance, dbClusters);

        //assert
        Assert.assertEquals(1, databricksClusterDTO.getCount().intValue());
        Assert.assertEquals(1, databricksClusterDTO.getItems().size());
    }

}
